package parser;
import java.io.*;
import java.util.List;
import java.util.Map;
import lexer.*;
import codigointermedio.*;
import Assembler.GeneradorAssembler;

public class MainParser {

    public static void main(String[] args) {
        // --- INICIO DE MODIFICACIÓN ---
        if (args.length < 1) {
            System.out.println("Uso: java parser.MainParser <ruta_del_archivo_fuente>");
            return;
        } 
        // Usar el primer argumento como ruta del archivo
        String filePath = args[0]; 
        // --- FIN DE MODIFICACIÓN ---
        
        String asmFilePath = "output.asm"; // Nombre del archivo de salida ASM

        System.out.println("=================================================");
        System.out.println("             INICIO DEL COMPILADOR");
        System.out.println("=================================================");

        // =================================================================
        // FASE 1: ANÁLISIS LÉXICO Y GENERACIÓN DE LISTA DE TOKENS (TP3 Requisito)
        // =================================================================
        System.out.println("\n=================================================");
        System.out.println("            LISTA DE TOKENS (LÉXICO)");
        System.out.println("=================================================");

        try (FileReader tokenReader = new FileReader(filePath)) {
            Scanner tokenScanner = new Scanner(tokenReader);
            Token currentToken;
            
            // Simulación de la lectura continua de tokens hasta EOF (Token.getType() == 0)
            while ((currentToken = tokenScanner.nextToken()).getType() != TokenType.EOF) {
                // Usamos el toString del Token para mostrar el lexema y tipo
                System.out.println(String.format(" -> Línea %d: Tipo: %s, Lexema: %s",
                    tokenScanner.getContext().getLine(),
                    currentToken.getType(),
                    currentToken.getLexeme()
                ));
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] Archivo de tokens no encontrado: " + filePath);
            return;
        } catch (IOException e) {
            System.err.println("[ERROR] Error de E/S durante el análisis léxico: " + e.getMessage());
            return;
        }

         System.out.println("\n");
         System.out.println ("=================================================================");
         System.out.println ( "ANALISIS SINTACTICO Y SEMANTICO ");
         System.out.println ("=================================================================");
        

        // Se vuelve a abrir el archivo, ya que el primer escaneo consumió el stream.
        try (FileReader parserReader = new FileReader(filePath)) { 
            
            Scanner scanner = new Scanner(parserReader);
            Parser parser = new Parser();
            parser.setScanner(scanner);
            
            // Ejecutar análisis sintáctico/semántico y generación de código
            parser.yyparse();

            // ---------------------------------------------------------
            //  NUEVO: MOSTRAR WARNINGS (Siempre, haya error o no)
            // ---------------------------------------------------------
            // Asumimos que implementaste el método getWarnings() en tu Parser 
            // como acordamos en el paso anterior.
            List<String> warnings = parser.getWarnings(); 

            if (!warnings.isEmpty()) {
                System.out.println("\n[!] ADVERTENCIAS (WARNINGS):");
                System.out.println("--------------------------");
                for (String warning : warnings) {
                    System.out.println(" -> " + warning);
                }
                System.out.println("--------------------------");
            }
            // ---------------------------------------------------------

            List<String> errores = parser.getListaErrores();

            if (!errores.isEmpty()) {
                System.err.println("\n[!] SE DETECTARON ERRORES:");
                System.err.println("--------------------------");
                for (String error : errores) {
                    System.err.println(" -> " + error);
                }
                System.err.println("--------------------------");
                System.err.println("NO se generó código intermedio debido a los errores.");
            } 
            else {
                // Si solo hubo warnings (o nada), entramos aquí para generar el ejecutable
                System.out.println("\n[OK] Compilación exitosa" + 
                    (warnings.isEmpty() ? "." : " (con WARNINGS)."));

                // REQUISITO 4: Contenido de la Tabla de Símbolos
                Map<String, PolacaInversa> polacas = parser.getPolacaGenerada();
                
                System.out.println("\n=================================================");
                System.out.println("         CONTENIDO DE LA TABLA DE SIMBOLOS");
                System.out.println("=================================================");
                System.out.println(parser.getSymbolTable().toString());

                // REQUISITO 1 y 2: Código Intermedio (Polaca)
                System.out.println("\n=================================================");
                System.out.println("      CÓDIGO INTERMEDIO (POLACA INVERSA)");
                System.out.println("=================================================");
                for (Map.Entry<String, PolacaInversa> entry : polacas.entrySet()) {
                    System.out.println("\n>>> BLOQUE: " + entry.getKey());
                    System.out.print(entry.getValue().toString()); 
                }
                
                // --- FASE DE GENERACIÓN DE CÓDIGO FINAL (TP4) ---
                System.out.println("\n=================================================");
                System.out.println("        INICIO DE GENERACIÓN DE ASSEMBLER");
                System.out.println("=================================================");
                
                Assembler.GeneradorAssembler assembler = new Assembler.GeneradorAssembler(
                    polacas,
                    parser.getSymbolTable(),
                    asmFilePath
                );
                
                assembler.generate(); 
                
                System.out.println("\n[OK] Código Assembler generado correctamente en: " + asmFilePath);
                System.out.println("PROCEDA A ENSAMBLAR CON MASM32:");
                System.out.println("ml /c /coff output.asm");
            }

        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] Archivo no encontrado: " + filePath);
        } catch (IOException e) {
            System.err.println("[ERROR] Error de E/S: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fatal durante el parsing o la generación de código.");
            e.printStackTrace();
        }
    }
}