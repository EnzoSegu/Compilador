package codigointermedio;
import lexer.SymbolEntry;
import java.util.List;

// Clase auxiliar para transportar datos del encabezado al final del bucle
public class ForContext {
    public SymbolEntry variableControl;
    public int labelInicio;
    public List<Integer> listaBF;
    public boolean esIncremento;

    public ForContext(SymbolEntry var, int label, List<Integer> bf, boolean inc) {
        this.variableControl = var;
        this.labelInicio = label;
        this.listaBF = bf;
        this.esIncremento = inc;
    }

}
