package lexer.semanticActions;

import lexer.*;

public class AS4 implements SemanticAction {
    @Override
    public void execute(ScannerContext context, int symbol) {
        String lexeme = context.getLexeme().trim();

        String normalized = lexeme;

        if (normalized.startsWith(".")) {
            normalized = "0" + normalized;
        }

        if (normalized.endsWith(".")) {
            normalized = normalized + "0";
        }

        normalized = normalized.replace('F', 'E').replace('f', 'E');

        try {
            float value = Float.parseFloat(normalized);

            if (value != 0.0f &&
                (Math.abs(value) < 1.17549435E-38f || Math.abs(value) > 3.40282347E38f)) {
                context.setToken(TokenType.ERROR, "Error Lexico: Constante FLOAT32 fuera de rango -> " + lexeme);
            } else {
            
                SymbolEntry entry = new SymbolEntry(lexeme, "float", value);
                context.setToken(TokenType.FLOAT32, entry); // <-- ¡ARREGLADO!
            }

        } catch (NumberFormatException e) {
           
            context.setToken(TokenType.ERROR, "Error lexico: Formato inválido de constante FLOAT32 -> " + lexeme);
        }

        context.clear();
        if (symbol == ' ' || symbol == '\t' || symbol == '\n') {
            context.setDebug(true);
        }   else {
            context.setDebug(false);
        }
        }
    }
