package lexer.semanticActions;

import lexer.*;

public class AS7 implements SemanticAction {

    @Override
    public void execute(ScannerContext context, int symbol) {
        String lexeme = context.getLexeme();
        char unexpected = (char) symbol;

        // Generar token de error
        context.setToken(TokenType.ERROR, "Error Lexico en línea " + context.getLine() +
                ": símbolo no esperado -> '" + lexeme + "'");

        // Limpiar el buffer de lexema para reiniciar
        context.clear();
    }
}
