package lexer.semanticActions;

import lexer.*;

public class AS9 implements SemanticAction {

    @Override
    public void execute(ScannerContext context, int symbol) {
        // Ignoramos todo lo acumulado porque es un comentario
        context.clear();

        // No se genera token, simplemente seguimos
        // (el scanner seguirá en estado 0 buscando nuevo lexema)
    }
}
    