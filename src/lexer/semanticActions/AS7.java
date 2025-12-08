package lexer.semanticActions;

import lexer.*;

public class AS7 implements SemanticAction {

    @Override
    public void execute(ScannerContext context, int symbol) {
        String lexeme = context.getLexeme();
        char unexpected = (char) symbol;

        // Generar token de error
        context.setToken(TokenType.ERROR, "Error Lexico "  +
                ": símbolo no esperado -> '" + lexeme + "'", context.getLine());

        // Limpiar el buffer de lexema para reiniciar
        context.clear();
    }
}
