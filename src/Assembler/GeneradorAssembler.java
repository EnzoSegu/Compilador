package Assembler;
import lexer.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.HashSet; 

import codigointermedio.PolacaEntry;
import codigointermedio.PolacaInversa;

public class GeneradorAssembler {
    
    private final Map<String, PolacaInversa> polacaCompleta; 
    private final SymbolTable symbolTable;
    private final BufferedWriter writer;
    private String currentFunctionName = ""; 

    public GeneradorAssembler(Map<String, PolacaInversa> polacaCompleta, 
                              SymbolTable symbolTable, 
                              String filename) throws IOException {
        this.polacaCompleta = polacaCompleta;
        this.symbolTable = symbolTable;
        this.writer = new BufferedWriter(new FileWriter(filename));
    }
    
    public void generate() throws IOException {
        try {
            generateHeader();
            generateDataSegment();
            generateCodeSegment(); // main + funciones
        } finally {
            writer.close();
        }
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
        writer.write("extern print_float:PROC\n");
        writer.write("extern print_string:PROC\n");
        writer.write("extern print_int:PROC\n");
        writer.write("\n");
    }

    private void generateDataSegment() throws IOException {
        writer.write(".DATA\n");
        writer.write("; Variables Auxiliares (DD ?) y Constantes\n");

        Set<String> emittedSymbols = new HashSet<>();

        for (SymbolEntry entry : symbolTable.getAllEntries()) { 
            String uso = entry.getUso();
            String tipo = entry.getTipo();
            // Uso getAsmName completo (incluye prefijo '_') para que DATA y CODE coincidan
            String asmName = getAsmName(entry); 

            if (emittedSymbols.contains(asmName)) {
                continue; 
            }

            if ("programa".equals(uso)) {
             emittedSymbols.add(asmName); 
             continue;
            }

            // 1. Variables, Parámetros y Temporales (Asignación de espacio)
            if ("variable".equals(uso) || "parametro".equals(uso) || "temporal".equals(uso) || "parametro_lambda".equals(uso)) {
                writer.write(String.format("%s\tDD ?\n", asmName)); 
                emittedSymbols.add(asmName);
            } 
            // 2. Constantes
            else if ("constante".equals(uso)) {
                String lexeme = entry.getLexeme();
                if (lexeme == null) lexeme = "";

                // Limpiar espacios invisibles
                String cleanLexeme = lexeme.replaceAll("\\s", "");

                // Si es una constante numérica con sufijo 'I' (ej 10I) lo removemos
                if (cleanLexeme.endsWith("I") || cleanLexeme.endsWith("i")) {
                    cleanLexeme = cleanLexeme.substring(0, cleanLexeme.length()-1);
                }

                if ("int".equals(tipo) || "float".equals(tipo)) {
                    if (cleanLexeme.isEmpty()) {
                        // seguridad: si no hay literal numérico, inicializo a 0
                        cleanLexeme = "0";
                    }
                    writer.write(String.format("%s\tDD %s\n", asmName, cleanLexeme));
                    emittedSymbols.add(asmName);
                }
                else if ("string".equals(tipo)) {
                    // Defensive: si lexeme no tiene comillas, lo tratamos como contenido crudo
                    String content;
                    if (lexeme.length() >= 2 && lexeme.charAt(0) == '"' && lexeme.charAt(lexeme.length()-1) == '"') {
                        content = lexeme.substring(1, lexeme.length() - 1);
                    } else {
                        content = lexeme;
                    }
                    // Escapar comillas internas para MASM
                    content = content.replace("\"", "\\\"");
                    // Si está vacío, al menos emitimos una cadena vacía terminada en 0
                    writer.write(String.format("%s\tDB \"%s\", 0\n", asmName, content));
                    emittedSymbols.add(asmName);
                } else {
                    // Uso desconocido: declarar como DD ? por seguridad
                    writer.write(String.format("%-40s DD ? ; (const desconocida tipo=%s lex=%s)\n", asmName, tipo, lexeme));
                    emittedSymbols.add(asmName);
                }
            } else {
                // Otros usos inesperados: declarar como DD ?
                writer.write(String.format("%s\tDD ? ; (uso=%s tipo=%s)\n", asmName, uso, entry.getTipo()));
                emittedSymbols.add(asmName);
            }
        }
        
        // Añadir la constante '_CTE_1' usada internamente para FORs si no existe
        if (!emittedSymbols.contains("_CTE_1")) {
             writer.write("_CTE_1\tDD 1\n");
             emittedSymbols.add("_CTE_1");
        }
        
        // Mensajes de Error de Runtime (asegurar que estén presentes)
        writer.write("\n; Rutinas de error de Runtime\n");
        if (!emittedSymbols.contains("_DIV_CERO")) {
            writer.write(String.format("_DIV_CERO\tDB \"%s\", 0\n", "Error en runtime: Division por cero!"));
            emittedSymbols.add("_DIV_CERO");
        }
        if (!emittedSymbols.contains("_OVERFLOW_FLOAT")) {
            writer.write(String.format("_OVERFLOW_FLOAT\tDB \"%s\", 0\n", "Error en runtime: Overflow de flotante!"));
            emittedSymbols.add("_OVERFLOW_FLOAT");
        }
        if (!emittedSymbols.contains("_RECURSION_ERR")) {
            writer.write(String.format("_RECURSION_ERR\tDB \"%s\", 0\n", "Error en runtime: Recursion directa prohibida!"));
            emittedSymbols.add("_RECURSION_ERR");
        }
        writer.write("\n");
    }

    private void generateCodeSegment() throws IOException {
        writer.write(".CODE\n");
        
        // Rutina de error para división por cero
        writer.write("_RTH_DIV_CERO:\n");
        writer.write("\tPUSH OFFSET _DIV_CERO\n");
        writer.write("\tCALL print_string\n");
        writer.write("\tADD ESP, 4\n");
        writer.write("\tJMP _EXIT_PROGRAM\n");
        
        // Rutina de error para Overflow de flotante (Fix A2005)
        writer.write("_RTH_OVERFLOW_FLOAT:\n");
        writer.write("\tPUSH OFFSET _OVERFLOW_FLOAT\n");
        writer.write("\tCALL print_string\n");
        writer.write("\tADD ESP, 4\n");
        writer.write("\tJMP _EXIT_PROGRAM\n");
        
        // Rutina de error para recursión directa (Fix A2006)
        writer.write("_RTH_RECURSION_DIRECTA:\n");
        writer.write("\tPUSH OFFSET _RECURSION_ERR\n");
        writer.write("\tCALL print_string\n");
        writer.write("\tADD ESP, 4\n");
        writer.write("\tJMP _EXIT_PROGRAM\n");
        
        String mainFuncName = null;
        PolacaInversa mainPolaca = null;

        for (SymbolEntry entry : symbolTable.getAllEntries()) {
        if ("programa".equals(entry.getUso())) {
            String funcName = entry.getMangledName();
            PolacaInversa polaca = polacaCompleta.get(funcName); 
            
            if (polaca != null) {
                mainFuncName = funcName;
                mainPolaca = polaca;
                break; // Encontrado el programa principal
            }
        }
    }

        for (Map.Entry<String, PolacaInversa> entry : polacaCompleta.entrySet()) {
        String funcName = entry.getKey();
        PolacaInversa polaca = entry.getValue();
        
        // 1. Omitir el programa principal (se define al final)
        if (mainFuncName != null && mainFuncName.equals(funcName)) {
            continue;
        }
        
        // 2. Omitir los bloques Lambda anónimos, ya que no son procedimientos PROC/ENDP propios
        if (funcName.toUpperCase().contains("LAMBDA_ANON_")) {
             continue;
        }
        
        this.currentFunctionName = funcName; 
        generateFunctionCode(funcName, polaca);
    }

        // Generar el punto de entrada 'start' y la definición PROC/ENDP para MAIN.
        if (mainFuncName != null) {
            this.currentFunctionName = mainFuncName;
            
            // 1. Generar el punto de entrada y salida del programa
            writer.write("\nstart:\n");
            writer.write(String.format("\tCALL _%s\n", mainFuncName.replace(":", "_"))); 
            writer.write("_EXIT_PROGRAM:\n");
            writer.write("\tINVOKE ExitProcess, 0\n"); 
            
            // 2. Generar la función principal PROC/ENDP
            generateFunctionCode(mainFuncName, mainPolaca);
        } else {
            // seguridad: si no hay MAIN generamos etiqueta start vacía para END start no falle
            writer.write("\nstart:\n");
            writer.write("\t; NO SE ENCONTRO MAIN - start vacio\n");
            writer.write("_EXIT_PROGRAM:\n");
            writer.write("\tINVOKE ExitProcess, 0\n");
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

        if (polaca == null || polaca.getCode() == null) {
            writer.write("\t; ERROR: Polaca nula o vacía para función " + currentFunctionName + "\n");
            return;
        }

        for (PolacaEntry entry : polaca.getCode()) {
            if (entry == null) continue;
            // 1. Generar Etiqueta de la Dirección
            writer.write(String.format("L_%d:\n", entry.getAddress())); 
            
            instruction = entry.getValue();
            String operator = null;
            Integer targetAddress = null; // Usaremos Integer

            // Prioridad saltos BF/BI
            if (entry.getType() != null && (entry.getType().equals("BF") || entry.getType().equals("BI"))){
                operator = entry.getType();
                if (!(instruction instanceof Integer)) {
                    writer.write("\t; ERROR: El valor del salto debe ser un entero (entrada addr=" + entry.getAddress() + ").\n");
                    continue;
                }
                targetAddress = (Integer)instruction;
            } 
            
            if (operator == null) {
                if (instruction instanceof SymbolEntry) {
                    writer.write("\t; OPERANDO OMITIDO\n"); 
                    continue; 
                } else if (instruction instanceof Integer) {
                    writer.write("\t; VALOR NUMÉRICO OMITIDO\n"); 
                    continue;
                }
                operator = instruction.toString();
            }
            
            switch (operator) {
                case "PROC_":
                case "ENDP_":
                    writer.write("\t; INSTRUCCIÓN DE BLOQUE OMITIDA\n"); 
                    continue; 
                
                case "RETURN":
                    writer.write("\tJMP _" + currentFunctionName.replace(":", "_") + "_EPILOGUE\n");
                    break;
                
                case "+":
                case "-":
                case "*":
                    translatePolacaArithmetic(polaca, entry, operator);
                    break;

                case "/":
                    translateDivision(polaca, entry);
                    break;
                
                case "ASSIGN":
                    translateAssignment(polaca, entry);
                    break;

                case "PRINT":
                    translatePrint(polaca, entry);
                    break;
                    
                case "CALL":
                    translateCall(polaca, entry);
                    break;
                    
                case "BI":
                case "BF":
                    translateJump(polaca,entry,operator, targetAddress);
                    break;
                    
                case "TOF":
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

    private PolacaEntry safeGet(PolacaInversa polaca, int address) {
        if (polaca == null || polaca.getCode() == null) return null;
        if (address < 0 || address >= polaca.getCode().size()) return null;
        return polaca.getEntryAt(address);
    }

    private String getAsmName(SymbolEntry entry) {
        if (entry == null) return "NULL_ENTRY";
        String mangled = entry.getMangledName();
        
        if (mangled == null || mangled.isEmpty()) {
            mangled = entry.getLexeme();
            if (mangled == null) mangled = "anon";
        }
        return "_" + mangled.replace(":", "_").replace(".", "_").replace("\"", "").replace(" ", "_")
                .replace("+", "_").replace("-", "_").replace("(", "_").replace(")", "_").replace(",", "_")
                .replace("=", "_");
    }

    private String normalizeFuncKeyToAsm(String funcName) {
        if (funcName == null) return "";
        return "_" + funcName.replace(":", "_");
    }

    private String getIntInstruction(String op) {
        switch (op) {
            case "+": return "ADD";
            case "-": return "SUB";
            case "*": return "IMUL";
            default: return "";
        }
    }

    private String getFpuInstruction(String op) {
        switch (op) {
            case "+": return "FADD";
            case "-": return "FSUB";
            case "*": return "FMUL";
            case "/": return "FDIV";
            default: return "";
        }
    }

    private void translatePolacaArithmetic(PolacaInversa polaca, PolacaEntry entry, String operator) throws IOException {
        PolacaEntry targetEntry = safeGet(polaca, entry.getAddress() + 1);
        PolacaEntry op2Entry = safeGet(polaca, entry.getAddress() - 1);
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 2);

        if (targetEntry == null || op1Entry == null || op2Entry == null) {
            writer.write("\t; ERROR: entradas faltantes para operacion aritmetica en addr " + entry.getAddress() + "\n");
            return;
        }
        if (!(targetEntry.getValue() instanceof SymbolEntry) || !(op1Entry.getValue() instanceof SymbolEntry) || !(op2Entry.getValue() instanceof SymbolEntry)) {
            writer.write("\t; ERROR: algun operando no es SymbolEntry en aritmetica addr " + entry.getAddress() + "\n");
            return;
        }

        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        SymbolEntry op2 = (SymbolEntry) op2Entry.getValue();
        String opType = target.getTipo();

        if ("int".equals(opType)) {
            writer.write("\tMOV EAX, " + getAsmName(op1) + "\n");
            writer.write("\t" + getIntInstruction(operator) + " EAX, " + getAsmName(op2) + "\n");
            writer.write("\tMOV " + getAsmName(target) + ", EAX\n");
        } else if ("float".equals(opType)) {
            writer.write("\tFLD DWORD PTR " + getAsmName(op1) + "\n");
            writer.write("\tFLD DWORD PTR " + getAsmName(op2) + "\n");
            writer.write("\t" + getFpuInstruction(operator) + "\n");
            writer.write("\t; Chequeo de Overflow/Underflow Flotante\n");
            writer.write("\tFWAIT\n");
            writer.write("\tFNSTSW AX\n");
            writer.write("\tTEST AH, 0004h\n");
            writer.write("\tJNE _RTH_OVERFLOW_FLOAT\n");
            writer.write("\tFSTP DWORD PTR " + getAsmName(target) + "\n");
        } else {
            writer.write("\t; ERROR: tipo no manejado en aritmetica: " + opType + "\n");
        }
    }

    private void translateDivision(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        PolacaEntry op2Entry = safeGet(polaca, entry.getAddress() - 1);
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 2);
        PolacaEntry targetEntry = safeGet(polaca, entry.getAddress() + 1);

        if (op1Entry == null || op2Entry == null || targetEntry == null) {
            writer.write("\t; ERROR: entradas faltantes para division en addr " + entry.getAddress() + "\n");
            return;
        }
        if (!(op1Entry.getValue() instanceof SymbolEntry) || !(op2Entry.getValue() instanceof SymbolEntry) || !(targetEntry.getValue() instanceof SymbolEntry)) {
            writer.write("\t; ERROR: algunos operandos no son SymbolEntry en division addr " + entry.getAddress() + "\n");
            return;
        }

        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        SymbolEntry op2 = (SymbolEntry) op2Entry.getValue();
        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        String opType = op2.getTipo();

        if ("int".equals(opType)) {
            writer.write("\t; Chequeo de division por cero (INT)\n");
            writer.write("\tMOV EBX, " + getAsmName(op2) + "\n");
            writer.write("\tCMP EBX, 0\n");
            writer.write("\tJE _RTH_DIV_CERO\n");
            writer.write("\tMOV EAX, " + getAsmName(op1) + "\n");
            writer.write("\tCDQ\n");
            writer.write("\tIDIV EBX\n");
            writer.write("\tMOV " + getAsmName(target) + ", EAX\n");
        } else if ("float".equals(opType)) {
            writer.write("\t; Chequeo de division por cero (FLOAT)\n");
            writer.write("\tFLD DWORD PTR " + getAsmName(op1) + "\n");
            writer.write("\tFLD DWORD PTR " + getAsmName(op2) + "\n");
            writer.write("\tFTST\n");
            writer.write("\tFWAIT\n");
            writer.write("\tFSTSW AX\n");
            writer.write("\tSAHF\n");
            writer.write("\tJZ _RTH_DIV_CERO\n");
            writer.write("\tFDIV\n");
            writer.write("\tFSTP DWORD PTR " + getAsmName(target) + "\n");
        } else {
            writer.write("\t; ERROR: tipo no manejado en division: " + opType + "\n");
        }
    }

    private void translateAssignment(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        PolacaEntry targetEntry = safeGet(polaca, entry.getAddress() - 1);
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 2);

        if (targetEntry == null || op1Entry == null) {
            writer.write("\t; ERROR: entradas faltantes para ASSIGN en addr " + entry.getAddress() + "\n");
            return;
        }
        if (!(targetEntry.getValue() instanceof SymbolEntry)) {
            writer.write("\t; ERROR: target ASSIGN no es SymbolEntry\n");
            return;
        }

        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        Object op1Value = op1Entry.getValue();

        if (!(op1Value instanceof SymbolEntry op1)) {
             writer.write("\t; ASIGNACION REDUNDANTE OMITIDA (Operando no es SymbolEntry)\n");
             return;
        }

        String asmTarget = getAsmName(target);
        String asmSource = getAsmName(op1);

        if ("int".equals(target.getTipo())) {
            writer.write("\tMOV EAX, " + asmSource + "\n");
            writer.write("\tMOV " + asmTarget + ", EAX\n");
        } else if ("float".equals(target.getTipo())) {
            writer.write("\tFLD DWORD PTR " + asmSource + "\n");
            writer.write("\tFSTP DWORD PTR " + asmTarget + "\n");
        } else {
            writer.write("\t; WARNING: tipo de asignacion no manejado: " + target.getTipo() + "\n");
        }
    }

    private void translateJump(PolacaInversa polaca, PolacaEntry entry, String operator, Integer targetAddress) throws IOException {
        if (targetAddress == null) {
            writer.write("\t; ERROR CRITICO: La direccion de salto (BF/BI) no fue proporcionada.\n");
            return;
        }
        if ("BI".equals(operator)) {
            writer.write("\t; Salto incondicional (BI) a L_" + targetAddress + "\n");
            writer.write("\tJMP L_" + targetAddress + "\n");
        } else if ("BF".equals(operator)) {
            PolacaEntry resultEntry = safeGet(polaca, entry.getAddress() - 1);
            if (resultEntry == null || !(resultEntry.getValue() instanceof SymbolEntry)) {
                 writer.write("\t; ERROR: Operando booleano de BF no es SymbolEntry.\n");
                 return;
            }
            SymbolEntry resultVar = (SymbolEntry) resultEntry.getValue();
            writer.write("\t; Salto si Falso (BF) a L_" + targetAddress + "\n");
            writer.write("\tMOV EAX, " + getAsmName(resultVar) + "\n");
            writer.write("\tCMP EAX, 0\n");
            writer.write("\tJE L_" + targetAddress + "\n");
        }
    }

    private void translateCall(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        PolacaEntry targetEntry = safeGet(polaca, entry.getAddress() - 1);
        if (targetEntry == null || !(targetEntry.getValue() instanceof SymbolEntry)) {
            writer.write("\t; ERROR: CALL sin funcion valida\n");
            return;
        }
        SymbolEntry targetFunc = (SymbolEntry) targetEntry.getValue();
        String targetMangledName = targetFunc.getMangledName() != null ? targetFunc.getMangledName().replace(":", "_") : targetFunc.getLexeme().replace(":", "_");

        String currentNormalized = normalizeFuncKeyToAsm(currentFunctionName);
        String targetNormalized = "_" + targetMangledName;

        if (currentNormalized.equals(targetNormalized)) { 
            writer.write("\tJMP _RTH_RECURSION_DIRECTA\n");
        } else {
            writer.write("\tCALL " + targetNormalized + "\n");
        }
    }

    private void translateConversion(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        PolacaEntry targetEntry = safeGet(polaca, entry.getAddress() + 1);
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 1);

        if (op1Entry == null || targetEntry == null) {
            writer.write("\t; ERROR: Operandos para TOF no disponibles\n");
            return;
        }
        if (!(op1Entry.getValue() instanceof SymbolEntry) || !(targetEntry.getValue() instanceof SymbolEntry)) {
            writer.write("\t; ERROR: Operandos para TOF no son SymbolEntry\n");
            return;
        }

        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();

        writer.write("\tFILD DWORD PTR " + getAsmName(op1) + "\n");
        writer.write("\tFSTP DWORD PTR " + getAsmName(target) + "\n");
    }

    private void translatePrint(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 1);
        if (op1Entry == null) {
            writer.write("\t; ERROR: PRINT sin operando\n");
            return;
        }
        Object op1Value = op1Entry.getValue();

        if (op1Value instanceof SymbolEntry op1) {
            String asmName = getAsmName(op1);

            if ("int".equals(op1.getTipo())) {
                writer.write("\tPUSH " + asmName + "\n");
                writer.write("\tCALL print_int\n");
                writer.write("\tADD ESP, 4\n"); 
            } else if ("float".equals(op1.getTipo())) {
                writer.write("\tPUSH OFFSET " + asmName + "\n");
                writer.write("\tCALL print_float\n");
                writer.write("\tADD ESP, 4\n"); 
            } else if ("string".equals(op1.getTipo())) {
                writer.write("\tPUSH OFFSET " + asmName + "\n");
                writer.write("\tCALL print_string\n");
                writer.write("\tADD ESP, 4\n");
            } else {
                writer.write("\t; WARNING: PRINT tipo no soportado: " + op1.getTipo() + "\n");
            }
        } else {
            writer.write("\t; ERROR: Operando de impresión no es SymbolEntry.\n");
        }
    }

    private void translateComparison(PolacaInversa polaca, PolacaEntry entry, String operator) throws IOException {
        PolacaEntry targetEntry = safeGet(polaca, entry.getAddress() + 1);
        PolacaEntry op2Entry = safeGet(polaca, entry.getAddress() - 1);
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 2);

        if (targetEntry == null || op1Entry == null || op2Entry == null) {
            writer.write("\t; ERROR: entradas faltantes para comparacion addr " + entry.getAddress() + "\n");
            return;
        }
        if (!(targetEntry.getValue() instanceof SymbolEntry) || !(op1Entry.getValue() instanceof SymbolEntry) || !(op2Entry.getValue() instanceof SymbolEntry)) {
            writer.write("\t; ERROR: comparacion con operandos no SymbolEntry\n");
            return;
        }

        SymbolEntry target = (SymbolEntry) targetEntry.getValue();
        SymbolEntry op1 = (SymbolEntry) op1Entry.getValue();
        SymbolEntry op2 = (SymbolEntry) op2Entry.getValue();

        String asmTarget = getAsmName(target);
        String jumpInstruction;
        switch (operator) {
            case "==": jumpInstruction = "JE"; break;
            case "!=": jumpInstruction = "JNE"; break;
            case ">":  jumpInstruction = "JG"; break;
            case "<":  jumpInstruction = "JL"; break;
            case ">=": jumpInstruction = "JGE"; break;
            case "<=": jumpInstruction = "JLE"; break;
            default: jumpInstruction = "JMP"; break;
        }

        if ("int".equals(op1.getTipo())) {
            writer.write("\tMOV EAX, " + getAsmName(op1) + "\n");
            writer.write("\tCMP EAX, " + getAsmName(op2) + "\n");
        } else {
            writer.write("\t; Comparacion flotante FPU\n");
            writer.write("\tFLD DWORD PTR " + getAsmName(op1) + "\n");
            writer.write("\tFCOM DWORD PTR " + getAsmName(op2) + "\n");
            writer.write("\tFSTSW AX\n");
            writer.write("\tSAHF\n");
        }

        writer.write("\tMOV EAX, 0\n");
        writer.write("\t" + jumpInstruction + " L_SET_1_" + entry.getAddress() + "\n");
        writer.write("\tJMP L_STORE_" + entry.getAddress() + "\n");
        writer.write("L_SET_1_" + entry.getAddress() + ":\n");
        writer.write("\tMOV EAX, 1\n");
        writer.write("L_STORE_" + entry.getAddress() + ":\n");
        writer.write("\tMOV " + asmTarget + ", EAX\n");
    }

}
