package Assembler;
import lexer.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Set;
import java.util.HashSet; 

import codigointermedio.PolacaEntry;
import codigointermedio.PolacaInversa;

import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

public class GeneradorAssembler {
    
    private final Map<String, PolacaInversa> polacaCompleta; 
    private final SymbolTable symbolTable;
    private final BufferedWriter writer;
    private String currentFunctionName = ""; 
    private int printfFormatCounter = 0; 
    
    public GeneradorAssembler(Map<String, PolacaInversa> polacaCompleta, 
                          SymbolTable symbolTable, 
                          String filename) throws IOException {
    this.polacaCompleta = polacaCompleta;
    this.symbolTable = symbolTable;
    
    // CAMBIO CRÍTICO: Usar Cp850 para que los acentos se vean bien en el CMD de Windows
    this.writer = new BufferedWriter(
        new OutputStreamWriter(
            new FileOutputStream(filename), 
            Charset.forName("Cp850")
        )
    );
}
    
    public void generate() throws IOException {
        try {
            generateHeader();
            generateDataSegment();
            generateCodeSegment(); 
        } finally {
            writer.close();
        }
    }
    
    private void generateHeader() throws IOException {
        writer.write("; --- Encabezado y Directivas (MASM 32-bit) ---\n");
        writer.write(".386\n");
        writer.write("option casemap:none\n");
        writer.write("includelib \\masm32\\lib\\kernel32.lib\n");
        writer.write("include \\masm32\\include\\masm32rt.inc\n"); 
        writer.write("printf PROTO C : VARARG\n");
        writer.write("\n");
        writer.write("; Las rutinas de impresión se manejarán con invoke printf de masm32rt.inc\n");
        writer.write("\n");
    }

    private void generateDataSegment() throws IOException {
        writer.write(".DATA\n");
        writer.write("; Variables Auxiliares (DD ?) y Constantes\n");

        Set<String> emittedSymbols = new HashSet<>();

        // -------------------------------------------------------------
        // 1. FORMATOS DE PRINTF (NUEVAS CONSTANTES)
        // -------------------------------------------------------------
        writer.write("\n; Formatos de printf para rutinas de I/O\n");
        writer.write("_FMT_INT\tDB \"%d\", 0\n"); 
        writer.write("_FMT_FLOAT\tDB \"%.10f\", 0\n"); 
        writer.write("_FMT_STRING\tDB \"%s\", 0\n"); 
        
        // -------------------------------------------------------------
        // 2. CONSTANTES Y VARIABLES DEL USUARIO
        // -------------------------------------------------------------
        
        for (SymbolEntry entry : symbolTable.getAllEntries()) { 
            String uso = entry.getUso();
            String tipo = entry.getTipo();
            String asmName = getAsmName(entry); 

            if (emittedSymbols.contains(asmName)) {
                continue; 
            }

            // =========================================================================
            // *** CORRECCIÓN CRÍTICA (Error A2005) ***
            // NO se debe generar 'DD ?' para símbolos de tipo FUNCION.
            // Si el uso es 'funcion' o 'programa', el símbolo se define en la sección .CODE
            // con la directiva 'PROC'. Definirlo aquí causa redefinición.
            // =========================================================================
            if ("programa".equals(uso) || "funcion".equals(uso)) {
               emittedSymbols.add(asmName); 
               continue;
            }

            if ("variable".equals(uso) || "parametro".equals(uso) || "temporal".equals(uso) || "parametro_lambda".equals(uso)) {
                writer.write(String.format("%s\tDD ?\n", asmName)); 
                emittedSymbols.add(asmName);
            } 
            else if ("constante".equals(uso)) {
                String lexeme = entry.getLexeme();
                if (lexeme == null) lexeme = "";

                // ** NOTA: La sustitución de caracteres especiales como 'ñ' o 'í' 
                // ** DEBE realizarse aquí si el ensamblador no los soporta.
                // ** El error A2044 se origina en el char ' ' (espacio no-separador),
                // ** pero asegurar ASCII puro en strings literales es buena práctica.
                
                String cleanLexeme = lexeme.replaceAll("\\s", "");
                
                if (cleanLexeme.endsWith("I") || cleanLexeme.endsWith("i")) {
                    cleanLexeme = cleanLexeme.substring(0, cleanLexeme.length()-1);
                }

                if ("int".equals(tipo) || "float".equals(tipo)) {
                    if (cleanLexeme.isEmpty()) {
                        cleanLexeme = "0.0"; 
                    }
                    if ("float".equals(tipo)) {
                    cleanLexeme = cleanLexeme.replace('F', 'E').replace('f', 'e');
                    }
                    writer.write(String.format("%s\tDD %s\n", asmName, cleanLexeme));
                    emittedSymbols.add(asmName);
                }
                else if ("string".equals(tipo)) {
                    String content;
                    if (lexeme.length() >= 2 && lexeme.charAt(0) == '"' && lexeme.charAt(lexeme.length()-1) == '"') {
                        content = lexeme.substring(1, lexeme.length() - 1);
                    } else {
                        content = lexeme;
                    }
                    // Sanitización básica: reemplazar el caracter de comilla dentro del string
                    content = content.replace("\"", "\\\"");
                    // Si el ensamblador falla por acentos o ñ, se debe remover o reemplazar aquí:
                    // content = content.replaceAll("[áéíóúÁÉÍÓÚñÑ]", "x"); 
                    
                    writer.write(String.format("%s\tDB \"%s\", 0\n", asmName, content));
                    emittedSymbols.add(asmName);
                } else {
                    writer.write(String.format("%-40s DD ? ; (const desconocida tipo=%s lex=%s)\n", asmName, tipo, lexeme));
                    emittedSymbols.add(asmName);
                }
            } else {
                writer.write(String.format("%s\tDD ? ; (uso=%s tipo=%s)\n", asmName, uso, entry.getTipo()));
                emittedSymbols.add(asmName);
            }
        }
        
        if (!emittedSymbols.contains("_CTE_1")) {
             writer.write("_CTE_1\tDD 1\n");
             emittedSymbols.add("_CTE_1");
        }
        
        // ADICIÓN de la constante de salto de línea.
        if (!emittedSymbols.contains("_SALTO_LINEA")) {
             writer.write("_SALTO_LINEA\tDB 0Dh, 0Ah, 0\n"); // CR (0Dh) + LF (0Ah) + NULL (0)
             emittedSymbols.add("_SALTO_LINEA");
        }
        
        // Mensajes de Error de Runtime (asegurar que estén presentes)
        writer.write("\n; Rutinas de error de Runtime\n");
        writer.write("_ERR_CAPTION\tDB \"Error de Ejecucion\", 0\n");
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
        
        writer.write("; Rutina de error para división por cero\n");
        writer.write("_RTH_DIV_CERO:\n");
        writer.write("\tINVOKE MessageBox, NULL, ADDR _DIV_CERO, ADDR _ERR_CAPTION, MB_OK + MB_ICONSTOP\n");
        writer.write("\tJMP _EXIT_PROGRAM\n");
        
        writer.write("; Rutina de error para Overflow de flotante\n");
        writer.write("_RTH_OVERFLOW_FLOAT:\n");
        writer.write("\tINVOKE MessageBox, NULL, ADDR _OVERFLOW_FLOAT, ADDR _ERR_CAPTION, MB_OK + MB_ICONSTOP\n");
        writer.write("\tJMP _EXIT_PROGRAM\n");
        
        writer.write("; Rutina de error para recursión directa\n");
        writer.write("_RTH_RECURSION_DIRECTA:\n");
        writer.write("\tINVOKE MessageBox, NULL, ADDR _RECURSION_ERR, ADDR _ERR_CAPTION, MB_OK + MB_ICONSTOP\n");
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
                break; 
            }
        }
    }

        for (Map.Entry<String, PolacaInversa> entry : polacaCompleta.entrySet()) {
        String funcName = entry.getKey();
        PolacaInversa polaca = entry.getValue();
        
        if (mainFuncName != null && mainFuncName.equals(funcName)) {
            continue;
        }
        
        if (funcName.toUpperCase().contains("LAMBDA_ANON_")) {
             continue;
        }
        
        this.currentFunctionName = funcName; 
        generateFunctionCode(funcName, polaca);
    }

        if (mainFuncName != null) {
            this.currentFunctionName = mainFuncName;
            
            writer.write("\nstart:\n");
            writer.write("\tFINIT\n"); // Inicializa la FPU para evitar errores de punto flotante
            writer.write(String.format("\tCALL _%s\n", mainFuncName.replace(":", "_"))); 
            writer.write("_EXIT_PROGRAM:\n");
            writer.write("\tINVOKE ExitProcess, 0\n");
            
            generateFunctionCode(mainFuncName, mainPolaca);
        } else {
            writer.write("\nstart:\n");
            writer.write("\t; NO SE ENCONTRO MAIN - start vacio\n");
            writer.write("_EXIT_PROGRAM:\n");
            writer.write("\tINVOKE ExitProcess, 0\n");
        }
        
        writer.write("END start\n");
    }

    private void generateFunctionCode(String funcName, PolacaInversa polaca) throws IOException {
        String asmName = funcName.replace(":", "_");
        
        writer.write(String.format("\n_%s PROC\n", asmName));

        writer.write("\tPUSH EBP\n");
        writer.write("\tMOV EBP, ESP\n");
        
        translatePolaca(polaca);

        writer.write(String.format("_%s_EPILOGUE:\n", asmName));
        writer.write("\tMOV ESP, EBP\n"); 
        writer.write("\tPOP EBP\n"); 
        writer.write("\tRET\n"); 
        
        writer.write(String.format("_%s ENDP\n", asmName));
    }

    private void translatePolaca(PolacaInversa polaca) throws IOException {
        // =========================================================================
        // *** CORRECCIÓN CARACTERES INVÁLIDOS (Error A2044) ***
        // Se reemplaza el espacio no-separador (' ') por un espacio normal en el comentario.
        // =========================================================================
        writer.write("\n\t; --- Traduccion Polaca Inversa (" + currentFunctionName + ") ---\n");
        Object instruction=null;

        if (polaca == null || polaca.getCode() == null) {
            writer.write("\t; ERROR: Polaca nula o vacía para función " + currentFunctionName + "\n");
            return;
        }

        for (PolacaEntry entry : polaca.getCode()) {
            if (entry == null) continue;
            writer.write(String.format("L_%d:\n", entry.getAddress())); 
            
            instruction = entry.getValue();
            String operator = null;
            Integer targetAddress = null;

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
        int finalAddr = polaca.getCode().size() + 1; 
        writer.write("L_" + finalAddr + ":\n");
        writer.write("\tNOP ; Etiqueta de fin de bloque\n");

        writer.write("\n\t; --- Fin Traduccion Polaca Inversa ---\n");
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

    // 1. Aplicamos tus reemplazos manuales primero
    String name = mangled.replace(":", "_").replace(".", "_").replace("\"", "").replace(" ", "_")
                 .replace("+", "_").replace("-", "_").replace("(", "_").replace(")", "_").replace(",", "_")
                 .replace("=", "_").replace(";", "_").replace("#", "_");

    // 2. REEMPLAZO FINAL: Cualquier cosa que no sea letra básica, número o _ se vuelve _
    // Esto quita acentos, diéresis y basura de los nombres de etiquetas
    name = name.replaceAll("[^a-zA-Z0-9_]", "_");

    return "_" + name;
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

            writer.write("\tFSTP DWORD PTR " + getAsmName(target) + "\n");

            writer.write("\t; Chequeo REAL de Overflow conversion a float32\n");
            writer.write("\tMOV EAX, DWORD PTR " + getAsmName(target) + "\n");

            // +Infinity
            writer.write("\tCMP EAX, 07F800000h\n");
            writer.write("\tJE _RTH_OVERFLOW_FLOAT\n");

            // -Infinity
            writer.write("\tCMP EAX, 0FF800000h\n");
            writer.write("\tJE _RTH_OVERFLOW_FLOAT\n");

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

        if ("int".equals(target.getTipo()) || "string".equals(target.getTipo())) {
        if ("constante".equals(op1.getUso()) && "string".equals(op1.getTipo())) {
            writer.write("\tMOV EAX, OFFSET " + asmSource + "\n");
        } else {
            writer.write("\tMOV EAX, " + asmSource + "\n");
        }
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
            // Esto es un chequeo rudimentario de recursión directa.
            writer.write("\tJMP _RTH_RECURSION_DIRECTA\n");
        } else {
            writer.write("\tCALL " + targetNormalized + "\n");
        }
    }

    // *** [CORRECCIÓN TOF y FLOTANTES] ***
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
        writer.write("\tFWAIT\n");
        writer.write("\tFSTP DWORD PTR " + getAsmName(target) + "\n");
    }

    // *** [CORRECCIÓN FORMATO y FLOTANTES] ***
    private void translatePrint(PolacaInversa polaca, PolacaEntry entry) throws IOException {
        PolacaEntry op1Entry = safeGet(polaca, entry.getAddress() - 1);
        if (op1Entry == null) {
            writer.write("\t; ERROR: PRINT sin operando\n");
            return;
        }
        Object op1Value = op1Entry.getValue();

        if (op1Value instanceof SymbolEntry op1) {
            String asmName = getAsmName(op1);
            String formatName = "";

            // --- PASO 1: IMPRIMIR DATO ---
            if ("int".equals(op1.getTipo())) {
                formatName = "_FMT_INT";
                writer.write("\tPUSH " + asmName + "\n"); 
                writer.write("\tPUSH OFFSET " + formatName + "\n");
                writer.write("\tCALL printf\n"); 
                writer.write("\tADD ESP, 8\n"); // Cleanup: 4 bytes int + 4 bytes format
            } else if ("float".equals(op1.getTipo())) {
                formatName = "_FMT_FLOAT";
                writer.write("\tFLD DWORD PTR " + asmName + "\n"); 
                writer.write("\tSUB ESP, 8\n"); 
                writer.write("\tFSTP QWORD PTR [ESP]\n"); 

                
                writer.write("\tPUSH OFFSET " + formatName + "\n");
                writer.write("\tCALL printf\n"); 
                writer.write("\tADD ESP, 12\n"); // Cleanup: 8 bytes double + 4 bytes format
            } else if ("string".equals(op1.getTipo())) {
               if ("constante".equals(op1.getUso())) {
                writer.write("\tPUSH OFFSET " + asmName + "\n"); // Es un literal "HOLA"
            } else {
                writer.write("\tPUSH " + asmName + "\n");        // Es una variable var C
            }
                writer.write("\tPUSH OFFSET _FMT_STRING\n");
            writer.write("\tCALL printf\n"); 
            writer.write("\tADD ESP, 8\n");
            }
            
            // --- PASO 2: IMPRIMIR SALTO DE LÍNEA ---
            writer.write("\n\t; Salto de linea forzado\n");
            writer.write("\tPUSH OFFSET _SALTO_LINEA\n");
            writer.write("\tPUSH OFFSET _FMT_STRING\n"); 
            writer.write("\tCALL printf\n"); 
            writer.write("\tADD ESP, 8\n"); // Cleanup: 4 bytes pointer + 4 bytes format
            
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
            case "<": jumpInstruction = "JL"; break;
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