package lexer.semanticActions;

import lexer.*;

public class AS5 implements SemanticAction {
    @Override
    public void execute(ScannerContext context, int symbol) {
        // 1. Agregar el caracter leído al lexema

        if (context.getToken() != null) {
            return;
        }

        String lexeme = context.getLexeme();

        if (TokenType.RESERVED_WORDS.containsKey(lexeme)) {
            TokenType reservedType = TokenType.RESERVED_WORDS.get(lexeme);
            context.setToken(reservedType, lexeme);
            context.clear();}
        else{
            if(lexeme!=lexeme.toUpperCase()){
                context.setToken(TokenType.ERROR, "Error Lexico: identificador con minúsculas " +
                        "no permitido -> " + lexeme, context.getLine());
                context.clear();
            }
            else{
                if (lexeme.length() > 20) {
                    String truncated = lexeme.substring(0, 20);
                    System.out.println("⚠ Warning (línea " + context.getLine() + "): " +
                               "Identificador demasiado largo, truncado a '" + truncated + "'");
                    lexeme = truncated;
            }
                SymbolEntry entry = new SymbolEntry(lexeme);
                context.setToken(TokenType.ID, entry); // <-- ¡ARREGLADO!
                context.clear();
            }
        }
        if (symbol == ' ' || symbol == '\t' || symbol == '\n') {
            context.setDebug(true);
        }
        else {
            context.setDebug(false);
        }
    }
}
