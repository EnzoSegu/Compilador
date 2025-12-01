package lexer.semanticActions;

import lexer.ScannerContext;
import lexer.Token;

public class AS2 implements SemanticAction {

    @Override
    public void execute(ScannerContext context, int symbol) {
        if(symbol!=' '){
            context.append((char) symbol);
        }
        context.setToken(Token.fromLexeme(context.getLexeme()));
        context.clear();
    }

}
