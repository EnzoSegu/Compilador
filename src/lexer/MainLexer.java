package lexer;

import java.io.*;
import java.nio.file.*;

public class MainLexer {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java lexer.MainLexer <ruta_del_archivo_fuente>");
            return;
        }

        String filePath = "src/tests/testLexer/test.txt";
        Path inputPath = Paths.get(filePath).toAbsolutePath().normalize();

        String baseName = inputPath.getFileName().toString().replace(".txt", "");

        // 📁 Carpeta "src/salidas"
        Path outputDir = Paths.get("src/salidas");

        // Crear carpeta si no existe
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
        } catch (IOException e) {
            System.err.println("Error al crear carpeta de salida: " + e.getMessage());
            return;
        }

        // Archivos de salida
        Path tokensOut = outputDir.resolve(baseName + "_tokens.txt");
        Path erroresOut = outputDir.resolve(baseName + "_errors.txt");
        Path tablaOut = outputDir.resolve(baseName + "_symbol_table.txt");

        try (
            FileReader reader = new FileReader(filePath);
            BufferedWriter tokWriter = Files.newBufferedWriter(tokensOut);
            BufferedWriter errWriter = Files.newBufferedWriter(erroresOut);
            BufferedWriter tabWriter = Files.newBufferedWriter(tablaOut)
        ) {
            System.out.println("Analizando archivo: " + filePath);
            Scanner scanner = new Scanner(reader);

            Token token;
            while ((token = scanner.nextToken()) != null) {
                if (token.getType() == TokenType.EOF) {
                    tokWriter.write("EOF\n");
                    break;
                }

                System.out.println(token);
                tokWriter.write(token + "\n");

                if (token.getType() == TokenType.ERROR) {
                    errWriter.write("Línea " + scanner.getContext().getLine() + ": " + token.getLexeme() + "\n");
                }
            }

            // Tabla de símbolos
            tabWriter.write(new SymbolTable().toString());

            System.out.println("\n=== Análisis Léxico Completado ===");
            System.out.println("Tokens → " + tokensOut);
            System.out.println("Errores → " + erroresOut);
            System.out.println("Tabla de símbolos → " + tablaOut);

        } catch (IOException e) {
            System.err.println("Error al leer archivo: " + e.getMessage());
        }
    }
}
