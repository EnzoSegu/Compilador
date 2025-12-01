package lexer.semanticActions;

import lexer.ScannerContext;

public class AS1 implements SemanticAction {
    
    @Override
    public void execute(ScannerContext context, int symbol) {
        context.append((char) symbol);
    }

}
