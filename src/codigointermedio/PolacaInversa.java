package codigointermedio;
import java.util.ArrayList;
import java.util.List;

import javax.print.DocFlavor.STRING;

import lexer.SymbolEntry;
import lexer.SymbolTable;

/**
 * Almacena y genera el código intermedio en notación de Polaca Inversa.
 * Usa PolacaEntry para un almacenamiento robusto.
 */
public class PolacaInversa {
    
    // ¡CORREGIDO! Usamos PolacaEntry para almacenar objetos y sus propiedades.
    private final List<PolacaEntry> code = new ArrayList<>();
    private int counter = 1; 
    private static int tempCounter = 1;
    private SymbolTable symbolTable;

    

    public PolacaInversa(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public PolacaElement generateOperand(SymbolEntry entry) {

        if (entry == null) {
        // Devuelve un elemento de error si la entrada es nula
        return generateErrorElement("unknown");
        }
        // Asume que SymbolEntry ya tiene el tipo ('int', 'float')
        return new PolacaElement(entry.getTipo(), entry);
    }
    public void insertOperandAt(PolacaElement element) {
        PolacaEntry newEntry = new PolacaEntry(counter, element.getResultEntry(), element.getResultEntry().getTipo(), false);
        code.add(newEntry);
        counter++; 
        }
    /**
     * Añade un operador/instrucción.
     */
    public int generateOperation(String operator, boolean isJump) {
        Object value = isJump ? null : operator; 
        String labelSalto = isJump ? operator : null; 
        
        PolacaEntry newEntry = new PolacaEntry(counter, value, labelSalto, isJump);
        code.add(newEntry);
        return counter++;
    }
    
    /**
     * Genera un temporal para almacenar el resultado de una expresión.
     */
    private SymbolEntry createTemporary(String resultType) {
        String tempName = "@T" + tempCounter++;
        
        // Usa el constructor que recibe el lexema para evitar el constructor por defecto inexistente
        SymbolEntry entry = new SymbolEntry(tempName);
        
        // Si el tipo es null o error, ponemos "untype" para evitar NullPointer
        entry.setTipo((resultType != null) ? resultType : "untype"); 
        
        entry.setUso("temporal"); // Importante para el assembler
        
        // Nota: SymbolTable.add se encargará del Name Mangling (ej: @T1:MAIN)
        // 3. GUARDAR EN LA TABLA
        if (symbolTable != null) {
            symbolTable.add(entry);
        }
        
        return entry; 
    }

    /**
     * Genera una operación binaria y devuelve el PolacaElement resultante (temporal).
     */
    public PolacaElement generateOperation(PolacaElement expr1, PolacaElement expr2, String operator, String resultType) {
    SymbolEntry tempEntry = createTemporary(resultType);
    Object value=null;

    Object v1 = expr1.getResultEntry().getValue();
    Object v2 = expr2.getResultEntry().getValue();
    String type = expr1.getResultType(); // "int" o "float"

    // 2. Creamos una clave única combinando TIPO + OPERADOR
    // Ejemplo: "int+" o "float/"
    if (v1 != null && v2 != null) {
            
            String operationKey = type + operator; 

            try {
                value = switch (operationKey) {
                    // Casos para Enteros
                    case "int+" -> (Integer)v1 + (Integer)v2;
                    case "int-" -> (Integer)v1 - (Integer)v2;
                    case "int*" -> (Integer)v1 * (Integer)v2;
                    case "int/" -> (Integer)v2 != 0 ? (Integer)v1 / (Integer)v2 : null; 
                    
                    case "float+" -> ((Number)v1).doubleValue() + ((Number)v2).doubleValue();
                    case "float-" -> ((Number)v1).doubleValue() - ((Number)v2).doubleValue();
                    case "float*" -> ((Number)v1).doubleValue() * ((Number)v2).doubleValue();
                    case "float/" -> ((Number)v2).doubleValue() != 0.0 ? ((Number)v1).doubleValue() / ((Number)v2).doubleValue() : null;
                    
                    default -> null;
                };
            } catch (Exception e) {
                value = null; // Ante cualquier error de cast, asumimos runtime
            }
        }
    tempEntry.setValue(value);
    insertOperandAt(expr1); // Carga el operando izquierdo (Ej: @T1)
    insertOperandAt(expr2); // Carga el operando derecho (Ej: 3.5)

        // Añadir el operador a la Polaca
    generateOperation(operator, false);
        
        // Añadir el temporal como destino
    insertOperandAt(generateOperand(tempEntry));
        
        // Añadir el operador de asignación
    int assignIndex = generateOperation("ASSIGN", false); 

        // Retornar el temporal como el resultado de la expresión
    return new PolacaElement(resultType, tempEntry); 
    
    }

    /**
     * **BACKPATCHING:** Rellena las direcciones de salto pendientes.
     */
    public void backpatch(List<Integer> list, int target) {
        for (int address : list) {
            // Buscamos la PolacaEntry por la dirección y actualizamos su valor
            for (PolacaEntry entry : code) {
                if (entry.getAddress() == address && entry.isJump()) {
                    // El valor del salto (que era null/vacio) ahora es la dirección destino.
                    entry.setValue(target); 
                    break;
                }
            }
        }
    }

    // --- Otras Generaciones Clave ---

    public void generateAssignment(SymbolEntry destino, PolacaElement fuente) {
        // Asignación: <expresion> <destino> ASSIGN
        
        // 1. El resultado de la expresión (fuente) ya está en la PI
        insertOperandAt(fuente);
        // 2. Añadir el destino
        insertOperandAt(generateOperand(destino));
        
        // 3. Añadir el operador de asignación
        generateOperation("ASSIGN", false);
    }
    
    public PolacaElement generateTOF(PolacaElement expr) {
        // La expresión ya está en PI. Se genera el operador y se retorna un nuevo temporal.
        insertOperandAt(expr);
        // 1. Crear temporal para resultado float
        SymbolEntry tempEntry = createTemporary("float");
        
        // 2. Generar operador TOF
        generateOperation("TOF", false);
        
        // 3. Generar destino temporal y ASSIGN
        insertOperandAt(generateOperand(tempEntry));
        generateOperation("ASSIGN", false);
        
        return new PolacaElement("float", tempEntry);
    }

    public PolacaElement generateErrorElement(String defaultType) {
        return new PolacaElement(defaultType, -1);
    }

    // --- Salida Final ---
    
    @Override
public String toString() {
        StringBuilder sb = new StringBuilder();
        String se;

        for (PolacaEntry entry : code) {
            Object valor = entry.getValue();
            // --- CORRECCIÓN AQUÍ ---
            if (entry.isJump()) {
                // 1. CASO SALTO: Recuperamos el tipo (BF/BI) y le pegamos el destino
                String etiqueta = (entry.getType() != null) ? entry.getType() : "JUMP";
                String destino = (valor != null) ? valor.toString() : "?";
                se = etiqueta + " " + destino;
            } 
            else if (valor instanceof SymbolEntry) {
                // 2. CASO VARIABLE/CONSTANTE
                // Usamos getLexeme() para ver el nombre original, o getMangledName() si prefieres
                se = ((SymbolEntry) valor).getLexeme();
            } 
            else {
                // 3. CASO OPERADOR NORMAL (+, -, etc.)
                se = (valor != null) ? valor.toString() : "null";
            }
            // -----------------------

            sb.append(String.format("%-5d | %s\n", entry.getAddress(), se));
        }
        return sb.toString();
    }
    public List<Integer> generateUnconditionalJump() {
        // El operador es "BI" o la representación que use para salto incondicional.
        int jumpAddress = generateOperation("BI", true); 
        
        List<Integer> pendingList = new ArrayList<>();
        pendingList.add(jumpAddress);
        return pendingList;
    }

    /**
     * Une dos listas de direcciones pendientes para backpatching.
     */
    public List<Integer> mergeLists(List<Integer> list1, List<Integer> list2) {
        List<Integer> newList = new ArrayList<>(list1);
        newList.addAll(list2);
        return newList;
    }
    
    /**
     * Retorna la dirección actual (siguiente instrucción) para usar como destino (target).
     */
    public int getCurrentAddress() {
        return counter; 
    }

    public void generatePrint(PolacaElement expr) {

    insertOperandAt(expr);
    
    generateOperation("PRINT", false); 
}

/**
 * Genera el código para la sentencia RETURN.
 */
public void generateReturn(PolacaElement expr) {
    // El parser es responsable de asegurar que el tipo de expr coincida con 
    // el tipo de retorno de la función.
    
    // Si la expresión no es nula, significa que hay un valor a retornar:
    if (expr != null && expr.getResultEntry() != null) {
        // Se asume que el operando de retorno ya está en la Polaca.
    }
    generateOperation("RETURN", false); 
}
public int generateFunctionStart(SymbolEntry entry) {
    // Se usa el nombre mangled para identificar la función.
    String value = "PROC_" + entry.getMangledName(); 
    
    PolacaEntry newEntry = new PolacaEntry(counter, value, null, false);
    code.add(newEntry);
    
    // **Importante:** Actualizar la Tabla de Símbolos con la dirección de inicio
    // entry.setDirInicio(counter); // Esta llamada se haría en el parser
    
    return counter++;
}

/**
 * Genera el marcador de fin de una función/procedimiento (ENDP).
 * @param entry El SymbolEntry de la función.
 */
public int generateFunctionEnd(SymbolEntry entry) {
    String value = "ENDP_" + entry.getMangledName(); 
    
    PolacaEntry newEntry = new PolacaEntry(counter, value, null, false);
    code.add(newEntry);
    return counter++;
}
public PolacaElement generateCondition(PolacaElement expr1, PolacaElement expr2, String operator, String resultType) {
    
    // 1. Crear un temporal para almacenar el resultado de la comparación (0 o 1)
    SymbolEntry tempEntry = createTemporary(resultType); 

    insertOperandAt(expr1);
    insertOperandAt(expr2);

    generateOperation(operator, false);
    
    // Añadir el temporal como destino
    insertOperandAt(generateOperand(tempEntry));
    
    // Añadir el operador de asignación
    generateOperation("ASSIGN", false); 

    // Crear el PolacaElement resultante (el resultado de la expresión)
    PolacaElement resultExpr = new PolacaElement(resultType, tempEntry); 
    insertOperandAt(generateOperand(tempEntry)); // Cargamos el temporal para evaluar
    int bf_address = generateOperation("BF", true); 

    // 6. Agregar a la lista de pendientes para backpatching
    resultExpr.getFalseList().add(bf_address);

    return resultExpr;
}

/**
     * Genera la instrucción de llamada a función.
     * Sigue la lógica de 3 direcciones: CALL -> Asignar resultado a Temporal -> Retornar Temporal.
     */
    public PolacaElement generateCall(SymbolEntry functionEntry) {
        // 1. Insertar el operando: El identificador de la función
        // En la Polaca se verá como:  Dirección | ID_Funcion
        insertOperandAt(generateOperand(functionEntry));

        // 2. Generar la operación CALL
        // En la Polaca se verá como:  Dirección | CALL
        generateOperation("CALL", false);

        // 3. Gestionar el Valor de Retorno
        String returnType = "void";
        
        // Verificamos si la función retorna algo (consultando la Tabla de Símbolos)
        if (functionEntry.getTiposRetorno() != null && !functionEntry.getTiposRetorno().isEmpty()) {
            returnType = functionEntry.getTiposRetorno().get(0);
        }

        if (!returnType.equals("void")) {
            SymbolEntry tempEntry = createTemporary(returnType);
            
            tempEntry.setFuncionOrigen(functionEntry);

            insertOperandAt(generateOperand(tempEntry));
            
            generateOperation("ASSIGN", false);
            
            return new PolacaElement(returnType, tempEntry);
        } else {
            // Si es void, retornamos un elemento dummy para que no rompa el flujo,
            // pero que no debe ser usado en expresiones aritméticas.
            return new PolacaElement("void", (SymbolEntry)null);
        }
    }

public int getSize() {
    return code.size();
}
public void appendPolaca(PolacaInversa other) {
    if (other == null || other.code.isEmpty()) {
        return;
    }
    
    int offset = this.counter - 1; // Desplazamiento base
    
    for (PolacaEntry entry : other.code) {
        int nuevaDireccion = this.counter;
        Object valor = entry.getValue();
        
        /* Si es un salto, ajustar el destino */
        if (entry.isJump() && valor instanceof Integer) {
            int targetRelativo = (Integer)valor;
            /* El target en 'other' es relativo (1-based), 
               hay que convertirlo a absoluto en 'this' */
            valor = targetRelativo + offset;
        }
        
        /* Crear nueva entrada con dirección ajustada */
        PolacaEntry nuevaEntrada = new PolacaEntry(
            nuevaDireccion,
            valor,
            entry.getType(),
            entry.isJump()
        );
        
        this.code.add(nuevaEntrada);
        this.counter++;
    }
}

public List<PolacaEntry> getCode() {
    return this.code;
}

public PolacaEntry getEntryAt(int address) {
    // Verificación básica de límites
    if (address < 1 || address > code.size()) {
        // En un compilador robusto, esto generaría un error fatal en tiempo de compilación.
        return null; 
    }
    // La lista 'code' en Java se indexa desde 0, Polaca desde 1.
    return code.get(address - 1);
}



}
