package lexer.semanticActions;

import lexer.ScannerContext;
import lexer.Token;

public class AS10 implements SemanticAction {

    @Override
    public void execute(ScannerContext context, int symbol) {
        
        context.setDebug(false);
        context.setToken(Token.fromLexeme(context.getLexeme()));
        context.clear();

    }
}

