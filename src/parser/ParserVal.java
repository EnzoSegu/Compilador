package parser;
import codigointermedio.*;
import lexer.*;
import java.util.ArrayList;
import java.util.List;
public class ParserVal
{
    public int ival;
    public double dval;
    public String sval;
    public Object obj;
    
    public SymbolEntry entry;
    public PolacaElement Polacaelement;
    public List<PolacaElement> listPe;
    public ArrayList<SymbolEntry> listSe;
    public ForContext contextfor;
    public ArrayList<String> listStr;
    public ParametroInvocacion paramInv;
    public ArrayList<ParametroInvocacion> listParamInv;

    public String[] semantica;

    // Constructores
    public ParserVal() { }


    public ParserVal(int val) { ival = val; }
    public ParserVal(double val) { dval = val; }
    public ParserVal(String val) { sval = val; }
    public ParserVal(Object val) { obj = val; }

    public ParserVal(SymbolEntry val) { entry = val; }
    public ParserVal(PolacaElement val) { Polacaelement = val; }
    public ParserVal(ArrayList<SymbolEntry> val) { listSe = val; }
    public ParserVal(List<PolacaElement> val) { listPe = val; }
    public ParserVal(ForContext val) { contextfor = val; }
    public ParserVal(String[] val) {
        this.semantica = val;
    }
    public ParserVal(ParametroInvocacion val) {
        this.paramInv=val;
    }
}