package lexer.semanticActions;

import lexer.*;

public class AS3 implements SemanticAction {

    @Override
    public void execute(ScannerContext context, int symbol) {
        
        context.append((char) symbol);
        String lexeme = context.getLexeme();

        try {
            // Quitar el sufijo 'I' del entero
            String numberPart = lexeme.substring(0, lexeme.length() - 1);
            int value = Integer.parseInt(numberPart);

            // Chequeo de rango (16 bits)
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                context.setToken(TokenType.ERROR, "Error Lexico en línea " + context.getLine() +
                        ": entero fuera de rango -> " + value);
            } else {
                SymbolEntry entry = new SymbolEntry(lexeme, "int", value);
                context.setToken(TokenType.INT16, entry); // <-- ¡ARREGLADO!
            }
        } catch (NumberFormatException e) {
            context.setToken(TokenType.ERROR, "Error Lexico en línea " + context.getLine() +
                    ": formato inválido para entero -> " + lexeme);
        }

        context.clear(); // limpiar buffer de lexema
    }
}
