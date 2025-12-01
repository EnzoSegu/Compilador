package Assembler;
import lexer.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import codigointermedio.PolacaEntry;
import codigointermedio.PolacaInversa;

public class GeneradorAssembler {
    
    private final Map<String, PolacaInversa> polacaCompleta; 
    
    // La instancia única de la Tabla de Símbolos
    private final SymbolTable symbolTable;
    private final FileWriter writer;
    
    // Variable de estado crucial para los chequeos de recursión y salto
    private String currentFunctionName = ""; 

    public GeneradorAssembler(Map<String, PolacaInversa> polacaCompleta, 
                              SymbolTable symbolTable, 
                              String filename) throws IOException {
        this.polacaCompleta = polacaCompleta;
        this.symbolTable = symbolTable;
        this.writer = new FileWriter(filename);
    }
    
    public void generate() throws IOException {
        generateHeader();
        generateDataSegment();
        generateCodeSegment(); // Esto contendrá el motor principal (MAIN) y las funciones
        writer.close();
    }
    
    private void generateHeader() throws IOException {
        writer.write("; --- Encabezado y Directivas (MASM 32-bit) ---\n");
        writer.write(".386\n");
        writer.write(".model flat, stdcall\n");
        writer.write("option casemap:none\n");
        writer.write("include \\masm32\\include\\windows.inc\n");
        writer.write("include \\masm32\\include\\kernel32.inc\n");
        writer.write("includelib \\masm32\\lib\\kernel32.lib\n");
        writer.write("include \\masm32\\include\\masm32.inc\n");
        writer.write("includelib \\masm32\\lib\\masm32.lib\n");
        writer.write("\n");
        writer.write("; --- Importar Rutinas Externas (Asumiendo que existen) ---\n");
        writer.write("extern _print_float:PROC\n");
        writer.write("extern _print_string:PROC\n");
        writer.write("extern _print_int:PROC\n");
        writer.write("\n");
    }

    private void generateDataSegment() throws IOException {
        writer.write(".DATA\n");
        writer.write("; Variables Auxiliares (DD ?) y Constantes\n");

        for (SymbolEntry entry : symbolTable.getAllEntries()) { 
            String uso = entry.getUso();
            String tipo = entry.getTipo();
            // Usamos getAsmName para sanitizar el nombre también aquí
            String asmName = getAsmName(entry).substring(1); // Quitamos el "_" inicial generado por getAsmName

            // 1. Variables, Parámetros y Temporales (Asignación de espacio)
            if ("variable".equals(uso) || "parametro".equals(uso) || "temporal".equals(uso) || "parametro_lambda".equals(uso)) {
                // Los temporales booleanos pueden ser DB para ahorrar espacio, pero DD también funciona.
                writer.write(String.format("%-15s DD ?\n", asmName)); 
            } 
            
            // 2. Constantes
            else if ("constante".equals(uso)) {
                String lexeme = entry.getLexeme();
                
                if ("int".equals(tipo) || "float".equals(tipo)) {
                    // Constantes numéricas (ambos usan DD en el segmento de datos)
                    writer.write(String.format("%-15s DD %s\n", asmName, lexeme));
                }
                else if ("string".equals(tipo)) {
                    // Constantes de cadena (Usan DB - Define Byte, con terminador nulo)
                    // Eliminamos las comillas externas antes de guardar en DB (ej: "Hola" -> Hola)
                    String content = lexeme.substring(1, lexeme.length() - 1);
                    writer.write(String.format("%-15s DB \"%s\", 0\n", asmName, content));
                }
            }
        }
        
        // 3. Mensajes de Error de Runtime (Obligatorio por TP4)
        writer.write("\n; Rutinas de error de Runtime\n");
        writer.write("_DIV_CERO       DB \"Error en runtime: Division por cero!\", 0\n");
        writer.write("_OVERFLOW_FLOAT DB \"Error en runtime: Overflow de flotante!\", 0\n");
        writer.write("_RECURSION_ERR  DB \"Error en runtime: Recursion directa prohibida!\", 0\n");
        writer.write("\n");
    }

    private void generateCodeSegment() throws IOException {
    writer.write(".CODE\n");
    
    // Rutina de error para división por cero
    writer.write("_RTH_DIV_CERO:\n");
    writer.write("\tPUSH OFFSET _DIV_CERO\n");
    writer.write("\tCALL _print_string\n");
    writer.write("\tADD ESP, 4\n");
    writer.write("\tJMP _EXIT_PROGRAM\n");
    
    // Rutina de error para Overflow de flotante (Fix A2005)
    writer.write("_RTH_OVERFLOW_FLOAT:\n");
    writer.write("\tPUSH OFFSET _OVERFLOW_FLOAT\n");
    writer.write("\tCALL _print_string\n");
    writer.write("\tADD ESP, 4\n");
    writer.write("\tJMP _EXIT_PROGRAM\n");
    
    // Rutina de error para recursión directa (Fix A2006)
    writer.write("_RTH_RECURSION_DIRECTA:\n");
    writer.write("\tPUSH OFFSET _RECURSION_ERR\n");
    writer.write("\tCALL _print_string\n");
    writer.write("\tADD ESP, 4\n");
    writer.write("\tJMP _EXIT_PROGRAM\n");
    
    for (Map.Entry<String, PolacaInversa> entry : polacaCompleta.entrySet()) {
        String funcName = entry.getKey();
        PolacaInversa polaca = entry.getValue();
        
        this.currentFunctionName = funcName; // Establecer el contexto actual
        
        // El MAIN es un caso especial
        if (funcName.contains("MAIN")) { 
            writer.write("\nstart:\n");
            // Llamamos a la función principal del programa
            writer.write(String.format("\tCALL _%s\n", funcName.replace(":", "_"))); 
            writer.write("_EXIT_PROGRAM:\n");
            writer.write("\tINVOKE ExitProcess, 0\n"); 
        } else {
            generateFunctionCode(funcName, polaca);
        }
    }
    
    // Como el MAIN real debe ser una función, se genera aquí el procedimiento
    if (polacaCompleta.containsKey(currentFunctionName)) { 
         generateFunctionCode(currentFunctionName, polacaCompleta.get(currentFunctionName));
    }
    
    writer.write("END start\n");
}

    private void generateFunctionCode(String funcName, PolacaInversa polaca) throws IOException {
        String asmName = funcName.replace(":", "_");
        
        // Generar la etiqueta del procedimiento (función)
        writer.write(String.format("\n_%s PROC\n", asmName));

        // 1. PRÓLOGO DEL STACK FRAME
        writer.write("\tPUSH EBP\n");
        writer.write("\tMOV EBP, ESP\n");

        // 2. TRADUCCIÓN DEL CUERPO
        translatePolaca(polaca);

        // 3. EPÍLOGO Y RETORNO
        writer.write(String.format("_%s_EPILOGUE:\n", asmName));
        writer.write("\tMOV ESP, EBP\n"); 
        writer.write("\tPOP EBP\n"); 
        writer.write("\tRET\n"); 
        
        writer.write(String.format("_%s ENDP\n", asmName));
    }

    private void translatePolaca(PolacaInversa polaca) throws IOException {
        writer.write("\n\t; --- Traduccion Polaca Inversa (" + currentFunctionName + ") --- \n");
        Object instruction=null;
        // Recorremos la Polaca por instrucciones (nos saltamos los operandos)
        for (PolacaEntry entry : polaca.getCode()) {
            
            // 1. Generar Etiqueta de la Dirección
            writer.write(String.format("L_%d:\t", entry.getAddress())); 
        
         instruction = entry.getValue();
            String operator = null;
            Integer targetAddress = null; // Usaremos Integer, no String

        // 1. PRIORIDAD: Usar entry.getType() para operadores de control de flujo (BF/BI)
            if (null != entry.getType() && (entry.getType().equals("BF") || entry.getType().equals("BI"))){
            operator = entry.getType();
            
            // CRUCIAL: Capturar la dirección como Integer, ya que entry.getValue() es el número (33/36)
            if (!(instruction instanceof Integer)) {
                 writer.write("\t; ERROR: El valor del salto debe ser un entero.\n");
                 continue;
            }
            targetAddress = (Integer)instruction;
        } 
        
        // 2. Filtros Normales (solo se aplican si no fue un salto BF/BI)
        if (operator == null) {
            
            if (instruction instanceof SymbolEntry) {
                // Omitir operandos (variables, constantes, temporales)
                continue; 
            } 
            else if (instruction instanceof Integer) {
                 // Omitir valores numéricos que no son direcciones de BF/BI
                 writer.write("\t; VALOR NUMÉRICO OMITIDO\n");
                 continue;
            }

            // Asumir que la instrucción restante es un operador normal (String)
            operator = instruction.toString();
        }
            
            switch (operator) {
                
                // --- ESTRUCTURALES ---
                case "PROC_":
                case "ENDP_":
                    continue; 
                
                case "RETURN":
                    // Retorno de la función (Epílogo)
                    writer.write("\tJMP _" + currentFunctionName.replace(":", "_") + "_EPILOGUE\n");
                    break;
                
                // --- OPERACIONES ARITMÉTICAS BÁSICAS ---
                case "+":
                case "-":
                case "*":
                    translatePolacaArithmetic(polaca, entry, operator);
                    break;

                case "/":
                    // REQUIERE CHEQUEO DE DIVISIÓN POR CERO (Mandatorio por TP4)
                    translateDivision(polaca, entry);
                    break;
                
                // --- ASIGNACIÓN Y FLUJO ---
                case "ASSIGN":
                    translateAssignment(polaca, entry);
                    break;

                case "PRINT":
                    translatePrint(polaca, entry);
                    break;
                    
                case "CALL":
                    translateCall(polaca, entry);
                    break;
                    
                // --- JUMPS Y CONVERSIONES ---
                case "BI":
                case "BF":
                    translateJump(polaca,entry,operator, targetAddress);
                    break;
                    
                case "TOF": // To Float (Conversión Explícita)
                    translateConversion(polaca, entry);
                    break;

                case "==":
                case "!=": 
                case "<":
                case "<=": 
                case ">":
                case ">=":
                    translateComparison(polaca, entry, operator); 
                    break;

                default:
                    writer.write(String.format("\t; ERROR: Instruccion '%s' no implementada.\n", operator));
                    break;
            }
        }
        writer.write("\n\t; --- Fin Traduccion Polaca Inversa --- \n");
    }


    private String getAsmName(SymbolEntry entry) {
        if (entry == null) return "NULL_ENTRY";
        String mangled = entry.getMangledName();
        
        if (mangled == null || mangled.isEmpty()) {
            mangled = entry.getLexeme();
        }
        // Prefijo "_" y reemplazo de caracteres no permitidos en etiquetas MASM
        // Se añade el reemplazo de espacios en blanco, '+', '-' para evitar etiquetas MASM inválidas.
        return "_" + mangled.replace(":", "_").replace(".", "_").replace("\"", "").replace(" ", "_").replace("+", "_").replace("-", "_");
    }


    private String getIntInstruction(String op) {
        String instruction;
        switch (op) {
            case "+": instruction = "ADD"; break;
            case "-": instruction = "SUB"; break;
            case "*": instruction = "IMUL"; break;
            default: instruction = ""; break;
        }
        return instruction;
    }

    private String getFpuInstruction(String op) {
        String instruction;
        switch (op) {
            case "+": instruction = "FADD"; break;
            case "-": instruction = "FSUB"; break;
            case "*": instruction = "FMUL"; break;
            case "/": instruction = "FDIV"; break;
            default: instruction = ""; break;
        }
        return instruction;
    }

    private void translatePolacaArithmetic(PolacaInversa polaca, PolacaEntry entry, String operator) throws IOException {
        
        PolacaEntry targetEntry = polaca.getEntryAt(entry.getAddress() + 1);
        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        String opType = target.getTipo(); 
        
        PolacaEntry op2Entry = polaca.getEntryAt(entry.getAddress() - 1);
        PolacaEntry op1Entry = polaca.getEntryAt(entry.getAddress() - 2);
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        SymbolEntry op2 = (SymbolEntry) op2Entry.getValue();
        
        // --- LÓGICA DE TRADUCCIÓN ---
        if ("int".equals(opType)) {
            writer.write(String.format("\tMOV EAX, %s\n", getAsmName(op1)));
            writer.write(String.format("\t%s EAX, %s\n", getIntInstruction(operator), getAsmName(op2)));
            writer.write(String.format("\tMOV %s, EAX\n", getAsmName(target)));
        } 
        else if ("float".equals(opType)) {
            // FPU: ST(1) = OP1, ST(0) = OP2. OPERATOR -> ST(0) = ST(1) op ST(0)
            writer.write(String.format("\tFLD %s\n", getAsmName(op1))); 
            writer.write(String.format("\tFLD %s\n", getAsmName(op2))); 
            writer.write(String.format("\t%s\n", getFpuInstruction(operator)));
            
            // --- IMPLEMENTACIÓN DEL CHEQUEO DE OVERFLOW FLOTANTE (TP4) ---
            writer.write("\t; Chequeo de Overflow/Underflow Flotante\n");
            writer.write("\tFWAIT\n"); // Espera a que la operación FPU termine
            writer.write("\tFNSTSW AX\n"); // Guarda el FPU Status Word en AX (No destruye la pila)
            writer.write("\tTEST AH, 0004h\n"); // Chequea el bit OE (Overflow Exception) en el byte alto de AX (AH)
            writer.write("\tJNE _OVERFLOW_FLOAT\n"); 
            // Nota: El bit 0004h (bit 2) es el OE. Si es necesario chequear Underflow (UE), se usa 0002h.
            
            writer.write(String.format("\tFSTP %s\n", getAsmName(target)));
        }
    }

    private void translateDivision(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        
        PolacaEntry op2Entry = polaca.getEntryAt(entry.getAddress() - 1);
        PolacaEntry op1Entry = polaca.getEntryAt(entry.getAddress() - 2);
        PolacaEntry targetEntry = polaca.getEntryAt(entry.getAddress() + 1);
        
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        SymbolEntry op2 = (SymbolEntry) op2Entry.getValue();
        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        String opType = op2.getTipo();

        if ("int".equals(opType)) {
            // --- INTEGER DIVISION (IDIV/DIV) con Chequeo de Cero ---
            writer.write("\t; Chequeo de division por cero (INT)\n");
            writer.write(String.format("\tMOV EBX, %s\n", getAsmName(op2)));
            writer.write("\tCMP EBX, 0\n");
            writer.write("\tJE _ERROR_DIV_CERO\n"); 
            
            writer.write(String.format("\tMOV EAX, %s\n", getAsmName(op1))); 
            writer.write("\tCDQ\n"); 
            writer.write("\tIDIV EBX\n");
            
            writer.write(String.format("\tMOV %s, EAX\n", getAsmName(target)));
        } 
        else if ("float".equals(opType)) {
            // --- FLOAT DIVISION (FDIV) con Chequeo de Cero (FPU) ---
            writer.write("\t; Chequeo de division por cero (FLOAT)\n");
            
            // Cargar dividendo (OP1) y divisor (OP2)
            writer.write(String.format("\tFLD %s\n", getAsmName(op1))); 
            writer.write(String.format("\tFLD %s\n", getAsmName(op2))); 
            
            // Chequear el divisor (ST(0))
            writer.write("\tFTST\n");  // Compara ST(0) con 0.0
            writer.write("\tFWAIT\n");
            // Mover FPU Status Word (SW) a FLAGS de la CPU
            writer.write("\tFSTSW AX\n");
            writer.write("\tSAHF\n"); 
            
            writer.write("\tJZ _ERROR_DIV_CERO\n"); // JZ (Jump Zero) = divisor es 0
            
            writer.write("\tFDIV\n"); 
            
            writer.write(String.format("\tFSTP %s\n", getAsmName(target)));
        }
    }

    private void translateAssignment(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        
        PolacaEntry targetEntry = polaca.getEntryAt(entry.getAddress() - 1);
        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        
        PolacaEntry op1Entry = polaca.getEntryAt(entry.getAddress() - 2);
        Object op1Value = op1Entry.getValue();
        
        if (!(op1Value instanceof SymbolEntry op1)) {
             writer.write("\t; ASIGNACION REDUNDANTE OMITIDA (Operando no es SymbolEntry)\n");
             return; // <--- ESTO ELIMINA EL ERROR VISUAL
        }

        String asmTarget = getAsmName(target);
        String asmSource = getAsmName(op1);

        // 1. Asignación de Enteros (CPU Registers)
        if ("int".equals(target.getTipo())) {
            writer.write(String.format("\tMOV EAX, %s\n", asmSource));
            writer.write(String.format("\tMOV %s, EAX\n", asmTarget));
        } 
        // 2. Asignación de Flotantes (FPU)
        else if ("float".equals(target.getTipo())) {
            writer.write(String.format("\tFLD %s\n", asmSource)); 
            writer.write(String.format("\tFSTP %s\n", asmTarget)); 
        }
    }


    private void translateJump(PolacaInversa polaca, PolacaEntry entry, String operator, Integer targetAddress) throws IOException {
    
    // 1. Verificar si el operador fue identificado y la dirección extraída
    if (targetAddress == null) {
        writer.write("\t; ERROR CRITICO: La direccion de salto (BF/BI) no fue proporcionada.\n");
        return;
    }
    
    // 2. Generación del Assembly
    if ("BI".equals(operator)) {
        // Salto incondicional: Siempre JMP a la etiqueta de destino (L_XXX)
        writer.write("\t; Salto incondicional (BI) a L_" + targetAddress + "\n");
        writer.write(String.format("\tJMP L_%d\n", targetAddress));
    } 
    else if ("BF".equals(operator)) {
        
        // El resultado booleano (@T3) está en N-1
        PolacaEntry resultEntry = polaca.getEntryAt(entry.getAddress() - 1);
        
        if (!(resultEntry.getValue() instanceof SymbolEntry)) {
             writer.write("\t; ERROR: Operando booleano de BF no es SymbolEntry.\n");
             return;
        }
        SymbolEntry resultVar = (SymbolEntry) resultEntry.getValue();

        // 1. Mover el resultado (0 o 1) a EAX
        writer.write("\t; Salto si Falso (BF) a L_" + targetAddress + "\n");
        writer.write(String.format("\tMOV EAX, %s\n", getAsmName(resultVar)));
        // 2. Comparar con 0
        writer.write("\tCMP EAX, 0\n");
        // 3. Saltar si es Igual a 0 (Jump Equal = Salto si Falso)
        writer.write(String.format("\tJE L_%d\n", targetAddress));
    }
}

    private void translateCall(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        
        PolacaEntry targetEntry = polaca.getEntryAt(entry.getAddress() - 1); 
        SymbolEntry targetFunc = (SymbolEntry) targetEntry.getValue(); 
        String targetMangledName = targetFunc.getMangledName().replace(":", "_");
        
        // 1. CHEQUEO DE RECURSIÓN DIRECTA (TP4)
        if (currentFunctionName.equals(targetFunc.getLexeme())) { 
            writer.write("\tJMP _ERROR_RECURSION_DIRECTA\n");
        } else {
            // 2. LLAMADA REAL
            writer.write(String.format("\tCALL _%s\n", targetMangledName));
        }
        
    }

    private void translateConversion(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        
        PolacaEntry targetEntry = polaca.getEntryAt(entry.getAddress() - 1);
        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        
        PolacaEntry op1Entry = polaca.getEntryAt(entry.getAddress() - 2);
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        
        String asmTarget = getAsmName(target);
        String asmSource = getAsmName(op1);
        
        // FILD (Float Integer Load) carga un entero de 4 bytes a la pila FPU y lo convierte a float.
        writer.write(String.format("\tFILD %s\n", asmSource)); 
        // FSTP (Float Store and Pop) guarda el resultado en el destino (float) y limpia la pila FPU.
        writer.write(String.format("\tFSTP %s\n", asmTarget)); 
    }

    private void translatePrint(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        
        PolacaEntry op1Entry = polaca.getEntryAt(entry.getAddress() - 1);
        Object op1Value = op1Entry.getValue();
        
        if (op1Value instanceof SymbolEntry op1) {
            String asmName = getAsmName(op1);
            
            if ("int".equals(op1.getTipo())) {
                // Imprimir entero: pasa el valor por PUSH
                writer.write(String.format("\tPUSH %s\n", asmName));
                writer.write("\tCALL _print_int\n");
                writer.write("\tADD ESP, 4\n"); 
            } 
            else if ("float".equals(op1.getTipo())) {
                // Imprimir flotante: pasa la dirección por PUSH OFFSET
                writer.write(String.format("\tPUSH OFFSET %s\n", asmName));
                writer.write("\tCALL _print_float\n");
                writer.write("\tADD ESP, 4\n"); 
            } 
            else if ("string".equals(op1.getTipo())) {
                // Imprimir cadena: pasa la dirección por PUSH OFFSET
                writer.write(String.format("\tPUSH OFFSET %s\n", asmName));
                writer.write("\tCALL _print_string\n");
                writer.write("\tADD ESP, 4\n");
            }
        } 
        else {
            writer.write("\t; ERROR: Operando de impresión no es SymbolEntry.\n");
        }
    }

    private void translateComparison(PolacaInversa polaca, PolacaEntry entry, String operator) throws IOException {
        // OP1 (pos-2) OP2 (pos-1) OPERATOR (pos) TARGET (pos+1) 

        PolacaEntry targetEntry = polaca.getEntryAt(entry.getAddress() + 1);
        SymbolEntry target = (SymbolEntry) targetEntry.getValue();

        PolacaEntry op2Entry = polaca.getEntryAt(entry.getAddress() - 1);
        PolacaEntry op1Entry = polaca.getEntryAt(entry.getAddress() - 2);
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        SymbolEntry op2 = (SymbolEntry) op2Entry.getValue();
        
        String asmTarget = getAsmName(target);

        String jumpInstruction;
        switch (operator) {
            case "==": jumpInstruction = "JE"; break; // Jump Equal
            case "!=": jumpInstruction = "JNE"; break; // Jump Not Equal
            case ">":  jumpInstruction = "JG"; break; // Jump Greater
            case "<":  jumpInstruction = "JL"; break; // Jump Less
            case ">=": jumpInstruction = "JGE"; break; // Jump Greater or Equal
            case "<=": jumpInstruction = "JLE"; break; // Jump Less or Equal
            default: jumpInstruction = "JMP"; break;
        }
        
        if ("int".equals(op1.getTipo())) { 
            // Integer Comparison: CMP sets FLAGS directly
            writer.write(String.format("\tMOV EAX, %s\n", getAsmName(op1)));
            writer.write(String.format("\tCMP EAX, %s\n", getAsmName(op2))); 
        } else {
            // FPU Comparison: FCOM -> FSTSW AX -> SAHF sets FLAGS
            writer.write("\t; Comparacion flotante FPU\n");
            writer.write(String.format("\tFLD %s\n", getAsmName(op1)));
            writer.write(String.format("\tFCOM %s\n", getAsmName(op2))); 
            writer.write("\tFSTSW AX\n"); 
            writer.write("\tSAHF\n"); 
        }
        
        // 2. GUARDAR RESULTADO (0 o 1)
        
        writer.write(String.format("\tMOV EAX, 0\n")); 
        writer.write(String.format("\t%s L_SET_1_%d\n", jumpInstruction, entry.getAddress())); 
        writer.write(String.format("\tJMP L_STORE_%d\n", entry.getAddress())); 
        
        writer.write(String.format("L_SET_1_%d:\n", entry.getAddress())); 
        writer.write("\tMOV EAX, 1\n"); 
        
        writer.write(String.format("L_STORE_%d:\n", entry.getAddress())); 
        writer.write(String.format("\tMOV %s, EAX\n", asmTarget)); 
    }

}