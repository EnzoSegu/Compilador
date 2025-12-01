package lexer.semanticActions;

import lexer.ScannerContext;

public interface SemanticAction {
    void execute(ScannerContext context,int symbol);
}
