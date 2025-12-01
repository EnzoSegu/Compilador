package lexer.semanticActions;

import lexer.*;

public class AS8 implements SemanticAction {
    @Override
    public void execute(ScannerContext context, int symbol) {

        context.append((char)symbol);
        String lexeme = context.getLexeme();
        
        // Quitar comillas inicial y final si existen
        if (lexeme.startsWith("\"") && lexeme.endsWith("\"")) {
            lexeme = lexeme.substring(1, lexeme.length() - 1);
        }

        SymbolEntry entry = new SymbolEntry(lexeme, "string", lexeme);
        context.setToken(TokenType.STRING, entry);

        // Limpiar para siguiente token
        context.clear();
    }
}
