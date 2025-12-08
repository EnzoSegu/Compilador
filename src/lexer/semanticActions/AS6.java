package lexer.semanticActions;

import lexer.ScannerContext;
import lexer.TokenType;

public class AS6 implements SemanticAction {
    @Override
    public void execute(ScannerContext context, int symbol) {
        char c = (char) symbol;

        if (c == '\n') {
            // Error: salto de línea dentro de string
            context.setToken(TokenType.ERROR, 
                "Error Lexico: salto de línea en cadena ", context.getLine());
            context.clear();
        } else {
            // Si no es salto de línea, lo agregamos al lexema normalmente
            context.append(c);
        }
    }
}
