package parser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import lexer.*;
import codigointermedio.*; 



public class Parser
{
    /* --- Gestores Principales ---*/
    private SymbolTable symbolTable;
    private Map<String, PolacaInversa> polacaGenerada;
    private Stack<PolacaInversa> pilaGestoresPolaca;
    private Stack<List<Integer>> pilaSaltosBF = new Stack<>();
    private Stack<List<Integer>> pilaSaltosElse = new Stack<>();

    /* --- Variables de control ---*/
    private Scanner lexer;
    private boolean errorEnProduccion = false;
    private boolean listaVariablesError = false;
    private boolean listaExpresionesError = false;
    private boolean listaTiposError = false;
    private SymbolEntry currentFunctionEntry = null;
    private int lambdaCounter = 0;
    private PolacaInversa lambdaBodyBuffer = null; 
    private PolacaInversa lambdaAssignBuffer = null; 
    private SymbolEntry currentLambdaFormal = null;
    
    /* --- LISTA DONDE SE GUARDAN LOS MENSAJES ---*/
    private List<String> listaErrores = new ArrayList<>();

    public Parser() {
        this.symbolTable = new SymbolTable();
        this.polacaGenerada = new HashMap<>();
        this.pilaGestoresPolaca = new Stack<>();
    }

    private PolacaInversa PI() {
        if (pilaGestoresPolaca.isEmpty()) return new PolacaInversa(symbolTable); 
        return pilaGestoresPolaca.peek();
    }

    public SymbolTable getSymbolTable() { return this.symbolTable; }
    public Map<String, PolacaInversa> getPolacaGenerada() { return this.polacaGenerada; }
    public List<String> getListaErrores() { return this.listaErrores; }

    /* =========================================================================*/
    /*  MÉTODOS DE ERROR (Sustituyen a System.err.println)*/
    /* =========================================================================*/

    /* 1. Método para tus errores manuales en la gramática (Sintácticos)*/
    private void addError(String mensaje) {
        String error = "Línea " + lexer.getContext().getLine() + ": " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }

    /* 2. Método para errores SEMÁNTICOS (Tipos, declaraciones)*/
    /* Se usa: yyerror("Mensaje", true);*/
    public void yyerror(String s, boolean semantico) {
        String error = "Error Semántico - Línea " + lexer.getContext().getLine() + ": " + s;
        listaErrores.add(error);
        errorEnProduccion = true;
    }

    /* 3. Método automático de BYACC (Sintácticos por defecto)*/
    public void yyerror(String s) {
        String error = "Error Sintáctico - Línea " + lexer.getContext().getLine() + ": " + s;
        listaErrores.add(error);
        errorEnProduccion = true;
    }

    public void setScanner(Scanner lexer) { this.lexer = lexer; }

    private int yylex() {
        Token tok = null;
        try {
            tok = lexer.nextToken();
        } catch (IOException e) {
            addError("Error Fatal de IO: " + e.getMessage());
            return 0;
        }
        if (tok == null) return 0;
        if (tok.getEntry() != null) {
            SymbolEntry entryDelScanner = tok.getEntry();
            
            /* Buscamos si ya existe (para recuperar el objeto FUNCION original)*/
            SymbolEntry entryExistente = symbolTable.lookup(entryDelScanner.getLexeme());
            
            if (entryExistente != null) {
                yylval = new ParserVal(entryExistente); /* <--- IMPORTANTE: new*/
            } else {
                yylval = new ParserVal(entryDelScanner); /* <--- IMPORTANTE: new*/
            }
        }
        switch (tok.getType()) {
            case ID: return ID;
            case STRING: return STRING;
            case INT16: return INT16;
            case FLOAT32: return FLOAT32;
            case VAR: return VAR;
            case PRINT: return PRINT;
            case IF: return IF;
            case ELSE: return ELSE;
            case ENDIF: return ENDIF;
            case RETURN: return RETURN;
            case FOR: return FOR;
            case FROM: return FROM;
            case TO: return TO;
            case PLUS: return PLUS;
            case MINUS: return MINUS;
            case STAR: return STAR;
            case SLASH: return SLASH;
            case ASSIGN: return ASSIGN;
            case ASSIGN_COLON: return ASSIGN_COLON;
            case EQ: return EQ;
            case NEQ: return NEQ;
            case LT: return LT;
            case LE_OP: return LE_OP;
            case GT: return GT;
            case GE: return GE;
            case LPAREN: return LPAREN;
            case RPAREN: return RPAREN;
            case LBRACE: return LBRACE;
            case RBRACE: return RBRACE;
            case UNDERSCORE: return UNDERSCORE;
            case SEMICOLON: return SEMICOLON;
            case ARROW: return ARROW;
            case COMMA: return COMMA;
            case CV: return CV;
            case CR: return CR;
            case LE_KW: return LE_KW;
            case TOF: return TOF;
            case INT_KW: return INT_KW;
            case FLOAT_KW: return FLOAT_KW;
            case ERROR:
                addError(tok.getLexeme()); 
                SymbolEntry fakeEntry = new SymbolEntry("ERROR_IGNORE");
                fakeEntry.setTipo("error"); 
                yylval.entry = fakeEntry;
                return ID;
            case EOF: return 0;
            case POINT: return POINT;
            default: return ERROR;
        }
    }
boolean yydebug;        //do I want debug output?
int yynerrs;            //number of errors so far
int yyerrflag;          //was there an error?
int yychar;             //the current working character

//########## MESSAGES ##########
//###############################################################
// method: debug
//###############################################################
void debug(String msg)
{
  if (yydebug)
    System.out.println(msg);
}

//########## STATE STACK ##########
final static int YYSTACKSIZE = 500;  //maximum stack size
int statestk[] = new int[YYSTACKSIZE]; //state stack
int stateptr;
int stateptrmax;                     //highest index of stackptr
int statemax;                        //state when highest index reached
//###############################################################
// methods: state stack push,pop,drop,peek
//###############################################################
final void state_push(int state)
{
  try {
		stateptr++;
		statestk[stateptr]=state;
	 }
	 catch (ArrayIndexOutOfBoundsException e) {
     int oldsize = statestk.length;
     int newsize = oldsize * 2;
     int[] newstack = new int[newsize];
     System.arraycopy(statestk,0,newstack,0,oldsize);
     statestk = newstack;
     statestk[stateptr]=state;
  }
}
final int state_pop()
{
  return statestk[stateptr--];
}
final void state_drop(int cnt)
{
  stateptr -= cnt; 
}
final int state_peek(int relative)
{
  return statestk[stateptr-relative];
}
//###############################################################
// method: init_stacks : allocate and prepare stacks
//###############################################################
final boolean init_stacks()
{
  stateptr = -1;
  val_init();
  return true;
}
//###############################################################
// method: dump_stacks : show n levels of the stacks
//###############################################################
void dump_stacks(int count)
{
int i;
  System.out.println("=index==state====value=     s:"+stateptr+"  v:"+valptr);
  for (i=0;i<count;i++)
    System.out.println(" "+i+"    "+statestk[i]+"      "+valstk[i]);
  System.out.println("======================");
}


//########## SEMANTIC VALUES ##########
//public class ParserVal is defined in ParserVal.java


String   yytext;//user variable to return contextual strings
ParserVal yyval; //used to return semantic vals from action routines
ParserVal yylval;//the 'lval' (result) I got from yylex()
ParserVal valstk[];
int valptr;
//###############################################################
// methods: value stack push,pop,drop,peek.
//###############################################################
void val_init()
{
  valstk=new ParserVal[YYSTACKSIZE];
  yyval=new ParserVal();
  yylval=new ParserVal();
  valptr=-1;
}
void val_push(ParserVal val)
{
  if (valptr>=YYSTACKSIZE)
    return;
  valstk[++valptr]=val;
}
ParserVal val_pop()
{
  if (valptr<0)
    return new ParserVal();
  return valstk[valptr--];
}
void val_drop(int cnt)
{
int ptr;
  ptr=valptr-cnt;
  if (ptr<0)
    return;
  valptr = ptr;
}
ParserVal val_peek(int relative)
{
int ptr;
  ptr=valptr-relative;
  if (ptr<0)
    return new ParserVal();
  return valstk[ptr];
}
final ParserVal dup_yyval(ParserVal val)
{
  ParserVal dup = new ParserVal();
  dup.ival = val.ival;
  dup.dval = val.dval;
  dup.sval = val.sval;
  dup.obj = val.obj;
  return dup;
}
//#### end semantic value section ####
public final static short ID=257;
public final static short STRING=258;
public final static short INT16=259;
public final static short FLOAT32=260;
public final static short IF=261;
public final static short ELSE=262;
public final static short ENDIF=263;
public final static short PRINT=264;
public final static short RETURN=265;
public final static short VAR=266;
public final static short FOR=267;
public final static short FROM=268;
public final static short TO=269;
public final static short PLUS=270;
public final static short MINUS=271;
public final static short STAR=272;
public final static short SLASH=273;
public final static short ASSIGN=274;
public final static short ASSIGN_COLON=275;
public final static short EQ=276;
public final static short NEQ=277;
public final static short LT=278;
public final static short LE_OP=279;
public final static short GT=280;
public final static short GE=281;
public final static short LPAREN=282;
public final static short RPAREN=283;
public final static short LBRACE=284;
public final static short RBRACE=285;
public final static short UNDERSCORE=286;
public final static short SEMICOLON=287;
public final static short ARROW=288;
public final static short COMMA=289;
public final static short POINT=290;
public final static short CV=291;
public final static short CR=292;
public final static short LE_KW=293;
public final static short TOF=294;
public final static short ERROR=295;
public final static short EOF=296;
public final static short INT_KW=297;
public final static short FLOAT_KW=298;
public final static short STRING_KW=299;
public final static short YYERRCODE=256;
final static short yylhs[] = {                           -1,
    0,    0,    0,   12,   28,   28,   28,   28,   29,   29,
   29,   29,   32,   32,   32,   32,   30,   30,   35,   35,
   35,    1,    1,   11,   11,   16,   16,   16,   13,   20,
   20,   14,   34,   34,   34,   34,   34,   34,   21,   21,
   21,   17,   17,   18,   18,   18,   15,   15,   15,   15,
   25,   25,   25,   25,   36,   31,   31,   31,   31,   31,
   31,   31,   33,   33,   33,   33,   33,   33,   38,   38,
   41,   41,   41,   42,   43,   44,   43,   43,   45,   39,
   39,   39,   39,   22,    2,   40,   40,   40,   40,   40,
   40,   40,   40,   40,    8,    8,    8,    8,   24,   24,
   24,   24,   24,   24,   37,   37,   37,   37,   19,   19,
   19,    3,    3,    3,    3,    3,    4,    4,    4,    4,
    4,    5,    5,    5,    5,    5,    5,    7,    6,    6,
    6,   26,   26,   27,   27,   23,   10,   10,   10,   10,
    9,    9,    9,
};
final static short yylen[] = {                            2,
    0,    1,    2,    1,    4,    4,    4,    3,    0,    2,
    2,    3,    0,    2,    2,    3,    1,    1,    3,    2,
    3,    1,    3,    1,    3,    1,    3,    2,    1,    1,
    3,    2,    9,   10,    9,    9,    9,    9,    1,    3,
    2,    1,    1,    1,    3,    3,    2,    3,    2,    1,
    2,    2,    2,    2,    5,    1,    1,    2,    2,    1,
    1,    1,    1,    2,    2,    1,    1,    1,    4,    4,
    0,    2,    3,    3,    2,    0,    5,    1,    0,    7,
    6,    6,    6,    5,    8,    5,   10,   10,   10,   10,
   10,   10,   10,    3,    3,    2,    3,    3,    1,    1,
    1,    1,    1,    1,    4,    4,    4,    4,    1,    3,
    2,    1,    3,    3,    3,    3,    1,    3,    3,    3,
    3,    1,    1,    1,    1,    1,    1,    4,    4,    4,
    4,    1,    3,    3,    2,    4,    7,    7,    7,    7,
    1,    1,    1,
};
final static short yydefred[] = {                         0,
    0,    0,    0,    4,    0,    0,    9,    0,    9,    0,
    3,    0,   23,    0,    0,    0,    0,    0,    0,    0,
    0,   42,   43,   24,    0,   62,    0,    0,    0,   39,
    0,    0,    0,   10,   11,   17,   18,   57,   56,    0,
   60,   61,    0,    6,    0,    5,   12,    0,    0,    0,
    0,   20,    0,    0,    0,    0,    0,   71,    0,    0,
    0,    0,    0,    0,    0,    0,   32,   41,    0,    0,
   59,   58,    0,    0,  125,  123,  124,    0,    0,    0,
  117,  127,  126,    0,    0,    0,    0,    0,    0,    0,
   21,   19,    0,    0,   94,    0,    0,    0,    0,    0,
   25,    0,    0,    0,   50,    0,    0,   44,    0,    0,
    0,    0,   31,    0,   40,    0,    0,    0,   99,  100,
  101,  102,  103,  104,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,   70,   69,    0,
    0,    0,    0,    0,    0,    0,    0,  136,    0,    0,
   72,  107,  105,    0,    0,   53,   51,   54,   52,   47,
    0,    0,    0,   49,    0,  108,  106,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  132,    0,    0,  114,
    0,  116,    0,   98,    0,  119,  118,  121,  120,   71,
    0,    0,    0,    0,   55,    0,    0,    0,    0,    0,
    0,   73,   86,    0,   13,   13,   45,   13,   13,   46,
   48,   13,    0,    0,    0,    0,  135,    0,  128,    0,
  130,  131,  129,    0,   78,   76,    0,   81,   82,   83,
    0,    0,    0,    0,    0,    0,    0,   13,    0,    0,
    0,    0,    0,    0,  141,  142,  143,    0,    0,    0,
  134,  133,   74,    0,   75,   80,    0,    0,    0,    0,
    0,    0,    0,    0,    0,   68,   14,   15,    9,   63,
    0,   66,   67,    9,    9,    9,    9,   84,  138,  139,
  140,  137,    0,    0,    0,    0,    0,    0,    0,    0,
   85,    9,   16,    0,   65,   64,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,   35,
   38,   36,   33,    0,   77,   87,   88,   89,   90,   91,
   92,   93,   34,
};
final static short yydgoto[] = {                          3,
   24,   25,   79,   80,   81,   82,   83,   84,  248,   26,
   85,    5,   28,   29,  108,   54,   30,  110,   90,   31,
   32,  117,   33,  125,  111,  176,  177,    6,   10,   34,
   35,  239,  268,   36,   37,   38,   39,   40,   41,   42,
  100,  191,  228,  254,  194,
};
final static short yysindex[] = {                       -53,
 -255, -248,    0,    0, -113, -214,    0,  -90,    0,  -17,
    0, -188,    0,  -39,  -42, -109,  -27,    9, -223,  -18,
  -25,    0,    0,    0,   10,    0,    8,   21,   26,    0,
 -242, -227, -235,    0,    0,    0,    0,    0,    0, -196,
    0,    0,  -42,    0,  -42,    0,    0,   18,   18,   67,
 -152,    0,    8, -136, -168,   63,   53,    0,   72, -152,
   77, -239, -152,   72, -154,  -25,    0,    0,  101,  101,
    0,    0,  190, -245,    0,    0,    0,   27,  476,   81,
    0,    0,    0,  -26,    8, -199,   95,   60,   87,  482,
    0,    0,   72,    8,    0,  100,  112, -225,  109,  351,
    0, -122, -154,  127,    0, -237, -232,    0,  154,   -4,
 -217,  443,    0,   39,    0,  139,  144, -174,    0,    0,
    0,    0,    0,    0, -152, -152, -152, -152,   80,  129,
   87,  178,  217,  232,  140,  140,  185,    0,    0,  155,
 -152,   87,    8, -152, -152, -152,  271,    0,  164,  167,
    0,    0,    0,  322,  171,    0,    0,    0,    0,    0,
 -162, -130, -154,    0,  199,    0,    0, -154,  180,   18,
  197,  205,  218,   87, -125,  -70,    0,  137,  123,    0,
   81,    0,   81,    0,   87,    0,    0,    0,    0,    0,
    4,    4,    4,  140,    0,   87,  239,  241,  248,  250,
  -45,    0,    0,  228,    0,    0,    0,    0,    0,    0,
    0,    0,  235,  290,  290,  290,    0,  244,    0, -152,
    0,    0,    0,  502,    0,    0,  245,    0,    0,    0,
    4, -152, -152, -152, -152, -152,  310,    0,  161,  161,
  161,  161,  161,  516,    0,    0,    0,  256,  257, -156,
    0,    0,    0,  140,    0,    0,  268,  272,  273,  274,
  275,  288, -133,  161,  246,    0,    0,    0,    0,    0,
 -163,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,  309,  140,  140,  140,  140,  140,  140,  140,
    0,    0,    0,   36,    0,    0,   50,   85,   99,  183,
  286,  289,  292,  299,  301,  302,  303,  307,  134,    0,
    0,    0,    0,  -42,    0,    0,    0,    0,    0,    0,
    0,    0,    0,
};
final static short yyrindex[] = {                       577,
    0,  -82,    0,    0,  196,  595,    0,    0,    0,    0,
    0,    0,    0,    0,    5,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  -33, -108,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    6,    0,    0,    0,    0,    0,
    0,    0, -117,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  226,    0,    0,    0,    0,    0,  304,
    0,    0,    0,    0,  265,    0,    0,    0,  390,    0,
    0,    0,    0,  -36,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
 -115,    0,    0,    0,    0,    0,  312,    0,    0,    0,
    0,  429,   88,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  -47,    0,    0,    0,    0,    0,    0,
  343,    0,  382,    0,  -46,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,  437,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  148,    0,    0,    0,    0,    0,    0,
    0,    0,    0,
};
final static short yygindex[] = {                         0,
   16,    0,  -40,  243, -119,    0,    0,  -37,  187,  465,
  -10,    0,  249,    0,  208,    0,    1,  -52,  534,    0,
    0,  536,    0,  530,    0,    0,  397,    0,   -6,  549,
  -92, -110,    0,    0,    0,  554,  560,  565,  580,  591,
  420, -129, -157,    0,    0,
};
final static int YYTABLESIZE=855;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         27,
   12,   27,   14,   27,    8,    7,  192,  151,   53,   88,
   89,   86,  114,  187,  189,    4,  104,  105,  156,  102,
   69,   57,   89,  158,  197,  198,  199,  201,    7,    2,
  146,   63,   68,    2,  229,  230,  126,    8,  131,  164,
    8,   11,  147,   94,   27,   27,   64,   67,   70,  142,
  154,  106,  107,   27,   65,  157,  136,   22,   23,   71,
  159,   66,  109,   52,  231,  109,  115,   43,    2,   22,
   23,  142,   16,  256,  101,   17,   18,   19,   20,   22,
   23,  172,  143,  137,  174,  175,  178,  179,    2,   27,
   72,  185,  295,   21,  105,  240,   44,  241,  242,  281,
  196,  243,  105,  109,   74,   75,   76,   77,   22,   23,
  173,  165,  257,  258,  259,  260,  261,  263,   95,   91,
    2,  206,  290,  296,  283,  208,  282,  264,  106,  107,
  217,  151,  213,  152,   22,   23,  106,  107,   26,   26,
   96,   78,   22,   23,  129,  130,   48,  129,  130,  291,
   92,  278,   93,  209,  302,  303,  304,  305,  306,  307,
  308,  109,  218,  109,  153,   30,   13,   96,  109,   26,
    9,   26,   49,   22,   22,   22,   22,   22,   22,  175,
   30,   22,   22,   22,   22,   22,   22,   22,   22,   22,
   22,   22,   22,   22,   22,   22,   22,   22,   22,   22,
   22,   22,    1,    2,   22,   22,   22,   22,   97,   95,
  236,   22,  219,   27,   22,   22,   45,    2,  220,   28,
   28,   16,   29,  237,   17,   18,   19,   20,   27,   27,
   27,   27,   27,   27,   29,   97,   95,   55,   15,    2,
   29,   29,   21,   16,   47,   46,   17,   18,   19,   20,
   28,  161,   28,   27,   50,   29,  135,   22,   23,  225,
    8,    7,  294,   56,   21,  226,  227,  297,  298,  299,
  300,   22,   23,   73,   74,   75,   76,   77,  162,   22,
   23,   61,  127,   27,  163,  309,   27,   27,   27,   27,
   51,   43,    2,   58,  168,   60,   16,   59,   27,   17,
   18,   19,   20,   96,   98,   43,    2,   62,  128,   99,
   16,   78,  113,   17,   18,   19,   20,   21,   97,    2,
  310,  169,   87,   74,   75,   76,   77,  163,    2,  129,
  130,   21,   22,   23,  311,  180,   74,   75,   76,   77,
   43,    2,  139,   27,   27,   16,   22,   23,   17,   18,
   19,   20,  133,  134,   43,    2,  129,  130,  103,   16,
   78,  116,   17,   18,   19,   20,   21,  144,  207,  312,
  210,  181,  183,   78,   27,  207,   27,  138,  222,  145,
   21,   22,   23,  313,  182,   74,   75,   76,   77,   43,
    2,  148,  129,  130,   16,   22,   23,   17,   18,   19,
   20,  249,  250,   37,   37,  223,  129,  130,   37,  155,
  160,   37,   37,   37,   37,   21,  265,    2,  323,  221,
  170,   16,   78,  190,   17,   18,   19,   20,  171,   37,
   22,   23,   37,  184,   74,   75,   76,   77,  314,    2,
  193,  195,   21,   16,   37,   37,   17,   18,   19,   20,
  202,    9,    9,  203,  205,  211,    9,   22,   23,    9,
    9,    9,    9,  212,   21,  119,  120,  121,  122,  123,
  124,   78,  186,   74,   75,   76,   77,    9,  214,   22,
   23,   22,   22,   22,   22,   22,  215,  188,   74,   75,
   76,   77,    9,    9,   22,   22,   22,   22,   22,  216,
  251,   22,   22,   22,   22,   22,   22,  232,   22,  233,
   78,  238,   22,   22,   22,   22,  234,  244,  235,   22,
  122,  122,  122,  122,  122,   78,  200,   74,   75,   76,
   77,  255,  293,  122,  122,  122,  122,  122,  279,  280,
  122,  122,  122,  122,  122,  122,  245,  122,  246,  247,
  284,  122,  122,  122,  285,  286,  287,  288,  122,  112,
  112,  112,  112,  112,   78,  262,   74,   75,   76,   77,
  289,  301,  315,  112,  112,  316,    1,  168,  317,  112,
  112,  112,  112,  112,  112,  318,  112,  319,  320,  321,
  112,  112,  112,  322,    2,   79,  112,  112,  113,  113,
  113,  113,  113,   78,  204,  118,  149,    2,  132,  224,
  163,   16,  113,  113,   17,   18,  252,   20,  113,  113,
  113,  113,  113,  113,    0,  113,    0,    0,    0,  113,
  113,  113,   21,    0,    0,  150,  113,  115,  115,  115,
  115,  115,    0,    0,    0,  109,  109,  109,  109,  109,
    0,  115,  115,    0,    0,    0,    0,  115,  115,  115,
  115,  115,  115,    0,  115,    0,    0,    0,  115,  115,
  115,    0,  109,    0,    0,  115,  109,    0,  109,    0,
    0,    0,    0,  109,  111,  111,  111,  111,  111,    0,
    0,    0,  110,  110,  110,  110,  110,    0,  166,   74,
   75,   76,   77,  266,  266,  266,  266,  266,    0,    0,
    0,  111,    0,    0,    0,  111,    0,  111,    0,  110,
    0,    0,  111,  110,    0,  110,    0,    0,  266,  167,
  110,  141,   74,   75,   76,   77,   78,    0,   74,   75,
   76,   77,    0,    0,    0,  129,  130,    0,    0,    0,
    0,  119,  120,  121,  122,  123,  124,  149,    2,    0,
    0,    0,   16,    0,  140,   17,   18,    0,   20,   78,
  141,    0,    2,    0,    0,   78,   16,    0,    0,   17,
   18,    0,   20,   21,    0,    0,  253,  267,  267,  267,
  267,  267,  269,  274,  275,  276,  277,   21,  270,  270,
  270,  270,  270,  271,  271,  271,  271,  271,    0,    0,
    0,    0,  267,    0,    0,    0,    0,  292,  272,  272,
  272,  272,  272,  270,    0,    0,    0,    0,  271,  273,
  273,  273,  273,  273,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  272,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  273,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         10,
    7,   12,    9,   14,    0,    0,  136,  100,   19,   50,
   51,   49,   65,  133,  134,    0,  256,  257,  256,   60,
  256,   21,   63,  256,  144,  145,  146,  147,  284,  257,
  256,  274,   32,  257,  192,  193,  282,  286,   79,  257,
  286,  256,  268,   54,   55,   56,  289,   32,  284,   90,
  103,  291,  292,   64,  282,  293,  256,  297,  298,  256,
  293,  289,   62,  287,  194,   65,   66,  256,  257,  297,
  298,  112,  261,  231,   59,  264,  265,  266,  267,  297,
  298,  256,   93,  283,  125,  126,  127,  128,  257,  100,
  287,  132,  256,  282,  257,  206,  285,  208,  209,  256,
  141,  212,  257,  103,  257,  258,  259,  260,  297,  298,
  285,  111,  232,  233,  234,  235,  236,  237,  287,  256,
  257,  284,  256,  287,  254,  256,  283,  238,  291,  292,
  256,  224,  170,  256,  297,  298,  291,  292,  256,  257,
  256,  294,  297,  298,  270,  271,  256,  270,  271,  283,
  287,  244,  289,  284,  284,  285,  286,  287,  288,  289,
  290,  161,  288,  163,  287,  274,  257,  283,  168,  287,
  284,  289,  282,  256,  257,  258,  259,  260,  261,  220,
  289,  264,  265,  266,  267,  268,  269,  270,  271,  272,
  273,  274,  275,  276,  277,  278,  279,  280,  281,  282,
  283,  284,  256,  257,  287,  288,  289,  290,  256,  256,
  256,  294,  283,  224,  297,  298,  256,  257,  289,  256,
  257,  261,  256,  269,  264,  265,  266,  267,  239,  240,
  241,  242,  243,  244,  268,  283,  283,  256,  256,  257,
  274,  275,  282,  261,  287,  285,  264,  265,  266,  267,
  287,  256,  289,  264,  282,  289,  283,  297,  298,  256,
  256,  256,  269,  282,  282,  262,  263,  274,  275,  276,
  277,  297,  298,  256,  257,  258,  259,  260,  283,  297,
  298,  256,  256,  294,  289,  292,  297,  298,  299,  300,
  282,  256,  257,  284,  256,  275,  261,  290,  309,  264,
  265,  266,  267,   55,   56,  256,  257,  282,  282,  257,
  261,  294,   64,  264,  265,  266,  267,  282,  256,  257,
  285,  283,  256,  257,  258,  259,  260,  289,  257,  270,
  271,  282,  297,  298,  285,  256,  257,  258,  259,  260,
  256,  257,  283,  256,  257,  261,  297,  298,  264,  265,
  266,  267,  272,  273,  256,  257,  270,  271,  282,  261,
  294,  261,  264,  265,  266,  267,  282,  268,  161,  285,
  163,  129,  130,  294,  287,  168,  289,  283,  256,  268,
  282,  297,  298,  285,  256,  257,  258,  259,  260,  256,
  257,  283,  270,  271,  261,  297,  298,  264,  265,  266,
  267,  215,  216,  256,  257,  283,  270,  271,  261,  283,
  257,  264,  265,  266,  267,  282,  256,  257,  285,  283,
  282,  261,  294,  284,  264,  265,  266,  267,  285,  282,
  297,  298,  285,  256,  257,  258,  259,  260,  256,  257,
  256,  287,  282,  261,  297,  298,  264,  265,  266,  267,
  287,  256,  257,  287,  284,  257,  261,  297,  298,  264,
  265,  266,  267,  284,  282,  276,  277,  278,  279,  280,
  281,  294,  256,  257,  258,  259,  260,  282,  282,  297,
  298,  256,  257,  258,  259,  260,  282,  256,  257,  258,
  259,  260,  297,  298,  269,  270,  271,  272,  273,  282,
  257,  276,  277,  278,  279,  280,  281,  269,  283,  269,
  294,  284,  287,  288,  289,  290,  269,  283,  269,  294,
  256,  257,  258,  259,  260,  294,  256,  257,  258,  259,
  260,  287,  287,  269,  270,  271,  272,  273,  283,  283,
  276,  277,  278,  279,  280,  281,  257,  283,  259,  260,
  283,  287,  288,  289,  283,  283,  283,  283,  294,  256,
  257,  258,  259,  260,  294,  256,  257,  258,  259,  260,
  283,  263,  287,  270,  271,  287,    0,  256,  287,  276,
  277,  278,  279,  280,  281,  287,  283,  287,  287,  287,
  287,  288,  289,  287,    0,  284,   63,  294,  256,  257,
  258,  259,  260,  294,  283,   70,  256,  257,   79,  190,
  289,  261,  270,  271,  264,  265,  220,  267,  276,  277,
  278,  279,  280,  281,   -1,  283,   -1,   -1,   -1,  287,
  288,  289,  282,   -1,   -1,  285,  294,  256,  257,  258,
  259,  260,   -1,   -1,   -1,  256,  257,  258,  259,  260,
   -1,  270,  271,   -1,   -1,   -1,   -1,  276,  277,  278,
  279,  280,  281,   -1,  283,   -1,   -1,   -1,  287,  288,
  289,   -1,  283,   -1,   -1,  294,  287,   -1,  289,   -1,
   -1,   -1,   -1,  294,  256,  257,  258,  259,  260,   -1,
   -1,   -1,  256,  257,  258,  259,  260,   -1,  256,  257,
  258,  259,  260,  239,  240,  241,  242,  243,   -1,   -1,
   -1,  283,   -1,   -1,   -1,  287,   -1,  289,   -1,  283,
   -1,   -1,  294,  287,   -1,  289,   -1,   -1,  264,  287,
  294,  289,  257,  258,  259,  260,  294,   -1,  257,  258,
  259,  260,   -1,   -1,   -1,  270,  271,   -1,   -1,   -1,
   -1,  276,  277,  278,  279,  280,  281,  256,  257,   -1,
   -1,   -1,  261,   -1,  283,  264,  265,   -1,  267,  294,
  289,   -1,  257,   -1,   -1,  294,  261,   -1,   -1,  264,
  265,   -1,  267,  282,   -1,   -1,  285,  239,  240,  241,
  242,  243,  239,  240,  241,  242,  243,  282,  239,  240,
  241,  242,  243,  239,  240,  241,  242,  243,   -1,   -1,
   -1,   -1,  264,   -1,   -1,   -1,   -1,  264,  239,  240,
  241,  242,  243,  264,   -1,   -1,   -1,   -1,  264,  239,
  240,  241,  242,  243,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,  264,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,  264,
};
}
final static short YYFINAL=3;
final static short YYMAXTOKEN=299;
final static String yyname[] = {
"end-of-file",null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,"ID","STRING","INT16","FLOAT32","IF","ELSE","ENDIF","PRINT",
"RETURN","VAR","FOR","FROM","TO","PLUS","MINUS","STAR","SLASH","ASSIGN",
"ASSIGN_COLON","EQ","NEQ","LT","LE_OP","GT","GE","LPAREN","RPAREN","LBRACE",
"RBRACE","UNDERSCORE","SEMICOLON","ARROW","COMMA","POINT","CV","CR","LE_KW",
"TOF","ERROR","EOF","INT_KW","FLOAT_KW","STRING_KW",
};
final static String yyrule[] = {
"$accept : input",
"input :",
"input : programa",
"input : programa error",
"inicio_programa : identificador",
"programa : inicio_programa LBRACE lista_sentencias RBRACE",
"programa : error LBRACE lista_sentencias RBRACE",
"programa : inicio_programa LBRACE lista_sentencias error",
"programa : inicio_programa lista_sentencias error",
"lista_sentencias :",
"lista_sentencias : lista_sentencias sentencia_declarativa",
"lista_sentencias : lista_sentencias sentencia_ejecutable",
"lista_sentencias : lista_sentencias error SEMICOLON",
"lista_sentencias_sin_return :",
"lista_sentencias_sin_return : lista_sentencias_sin_return sentencia_declarativa",
"lista_sentencias_sin_return : lista_sentencias_sin_return sentencia_ejecutable_sin_return",
"lista_sentencias_sin_return : lista_sentencias_sin_return error SEMICOLON",
"sentencia_declarativa : declaracion_funcion",
"sentencia_declarativa : declaracion_variable",
"declaracion_variable : VAR lista_variables SEMICOLON",
"declaracion_variable : VAR SEMICOLON",
"declaracion_variable : VAR lista_variables error",
"identificador : ID",
"identificador : ID UNDERSCORE ID",
"identificador_completo : identificador",
"identificador_completo : identificador_completo POINT identificador",
"lista_variables : identificador_completo",
"lista_variables : lista_variables COMMA identificador_completo",
"lista_variables : lista_variables identificador_completo",
"identificador_destino : identificador_completo",
"lista_variables_destino : identificador_destino",
"lista_variables_destino : lista_variables_destino COMMA identificador_destino",
"inicio_funcion : lista_tipos identificador",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion error LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion LPAREN error RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales RPAREN error lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : lista_tipos LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias error",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales error LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"lista_tipos : tipo",
"lista_tipos : lista_tipos COMMA tipo",
"lista_tipos : lista_tipos tipo",
"tipo : INT_KW",
"tipo : FLOAT_KW",
"parametros_formales : parametro_formal",
"parametros_formales : parametros_formales error parametro_formal",
"parametros_formales : parametros_formales COMMA parametro_formal",
"parametro_formal : tipo ID",
"parametro_formal : sem_pasaje tipo ID",
"parametro_formal : sem_pasaje ID",
"parametro_formal : ID",
"sem_pasaje : CV LE_KW",
"sem_pasaje : CR LE_KW",
"sem_pasaje : CV error",
"sem_pasaje : CR error",
"sentencia_return : RETURN LPAREN lista_expresiones RPAREN SEMICOLON",
"sentencia_ejecutable : asignacion",
"sentencia_ejecutable : sentencia_return",
"sentencia_ejecutable : sentencia_print SEMICOLON",
"sentencia_ejecutable : sentencia_print error",
"sentencia_ejecutable : sentencia_if",
"sentencia_ejecutable : sentencia_for",
"sentencia_ejecutable : lambda_expresion",
"sentencia_ejecutable_sin_return : asignacion",
"sentencia_ejecutable_sin_return : sentencia_print SEMICOLON",
"sentencia_ejecutable_sin_return : sentencia_print error",
"sentencia_ejecutable_sin_return : sentencia_if",
"sentencia_ejecutable_sin_return : sentencia_for",
"sentencia_ejecutable_sin_return : lambda_expresion",
"sentencia_print : PRINT LPAREN expresion RPAREN",
"sentencia_print : PRINT LPAREN error RPAREN",
"lista_sentencias_ejecutables :",
"lista_sentencias_ejecutables : lista_sentencias_ejecutables sentencia_ejecutable",
"lista_sentencias_ejecutables : lista_sentencias_ejecutables error SEMICOLON",
"bloque_sentencias_ejecutables : LBRACE lista_sentencias_ejecutables RBRACE",
"resto_if : ENDIF SEMICOLON",
"$$1 :",
"resto_if : ELSE $$1 bloque_sentencias_ejecutables ENDIF SEMICOLON",
"resto_if : error",
"$$2 :",
"sentencia_if : IF LPAREN condicion RPAREN $$2 bloque_sentencias_ejecutables resto_if",
"sentencia_if : IF error condicion RPAREN bloque_sentencias_ejecutables resto_if",
"sentencia_if : IF LPAREN condicion error bloque_sentencias_ejecutables resto_if",
"sentencia_if : IF LPAREN condicion RPAREN error resto_if",
"if_simple : IF LPAREN condicion RPAREN sentencia_ejecutable",
"encabezado_for : FOR LPAREN identificador_destino FROM factor TO factor RPAREN",
"sentencia_for : encabezado_for LBRACE lista_sentencias_ejecutables RBRACE SEMICOLON",
"sentencia_for : FOR error identificador_destino FROM factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN error FROM factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino error factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM error TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM factor error factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM factor TO error RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM factor TO factor error bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR error SEMICOLON",
"condicion : expresion operador_comparacion expresion",
"condicion : expresion expresion",
"condicion : error operador_comparacion expresion",
"condicion : expresion operador_comparacion error",
"operador_comparacion : EQ",
"operador_comparacion : NEQ",
"operador_comparacion : LT",
"operador_comparacion : LE_OP",
"operador_comparacion : GT",
"operador_comparacion : GE",
"asignacion : identificador_destino ASSIGN_COLON expresion SEMICOLON",
"asignacion : lista_variables_destino ASSIGN lista_expresiones SEMICOLON",
"asignacion : identificador_destino ASSIGN_COLON expresion error",
"asignacion : lista_variables_destino ASSIGN lista_expresiones error",
"lista_expresiones : expresion",
"lista_expresiones : lista_expresiones COMMA expresion",
"lista_expresiones : lista_expresiones expresion",
"expresion : termino",
"expresion : expresion PLUS termino",
"expresion : expresion PLUS error",
"expresion : expresion MINUS termino",
"expresion : expresion MINUS error",
"termino : factor",
"termino : termino STAR factor",
"termino : termino STAR error",
"termino : termino SLASH factor",
"termino : termino SLASH error",
"factor : identificador_completo",
"factor : INT16",
"factor : FLOAT32",
"factor : STRING",
"factor : invocacion_funcion",
"factor : conversion_tof",
"invocacion_funcion : ID LPAREN parametros_reales RPAREN",
"conversion_tof : TOF LPAREN expresion RPAREN",
"conversion_tof : TOF error expresion RPAREN",
"conversion_tof : TOF LPAREN expresion error",
"parametros_reales : parametro_real",
"parametros_reales : parametros_reales COMMA parametro_real",
"parametro_real : expresion ARROW ID",
"parametro_real : expresion error",
"lambda_prefix : LPAREN tipo ID RPAREN",
"lambda_expresion : lambda_prefix LBRACE if_simple RBRACE LPAREN lambda_argumento RPAREN",
"lambda_expresion : lambda_prefix error if_simple RBRACE LPAREN lambda_argumento RPAREN",
"lambda_expresion : lambda_prefix LBRACE if_simple error LPAREN lambda_argumento RPAREN",
"lambda_expresion : lambda_prefix LBRACE if_simple RBRACE LPAREN lambda_argumento error",
"lambda_argumento : ID",
"lambda_argumento : INT16",
"lambda_argumento : FLOAT32",
};

//#line 1596 "gramatic.y"

/* ======= Código Java adicional (opcional) ======= */
//#line 806 "Parser.java"
//###############################################################
// method: yylexdebug : check lexer state
//###############################################################
void yylexdebug(int state,int ch)
{
String s=null;
  if (ch < 0) ch=0;
  if (ch <= YYMAXTOKEN) //check index bounds
     s = yyname[ch];    //now get it
  if (s==null)
    s = "illegal-symbol";
  debug("state "+state+", reading "+ch+" ("+s+")");
}





//The following are now global, to aid in error reporting
int yyn;       //next next thing to do
int yym;       //
int yystate;   //current parsing state from state table
String yys;    //current token string


//###############################################################
// method: yyparse : parse input and execute indicated items
//###############################################################
int yyparse()
{
boolean doaction;
  init_stacks();
  yynerrs = 0;
  yyerrflag = 0;
  yychar = -1;          //impossible char forces a read
  yystate=0;            //initial state
  state_push(yystate);  //save it
  val_push(yylval);     //save empty value
  while (true) //until parsing is done, either correctly, or w/error
    {
    doaction=true;
    if (yydebug) debug("loop"); 
    //#### NEXT ACTION (from reduction table)
    for (yyn=yydefred[yystate];yyn==0;yyn=yydefred[yystate])
      {
      if (yydebug) debug("yyn:"+yyn+"  state:"+yystate+"  yychar:"+yychar);
      if (yychar < 0)      //we want a char?
        {
        yychar = yylex();  //get next token
        if (yydebug) debug(" next yychar:"+yychar);
        //#### ERROR CHECK ####
        if (yychar < 0)    //it it didn't work/error
          {
          yychar = 0;      //change it to default string (no -1!)
          if (yydebug)
            yylexdebug(yystate,yychar);
          }
        }//yychar<0
      yyn = yysindex[yystate];  //get amount to shift by (shift index)
      if ((yyn != 0) && (yyn += yychar) >= 0 &&
          yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
        {
        if (yydebug)
          debug("state "+yystate+", shifting to state "+yytable[yyn]);
        //#### NEXT STATE ####
        yystate = yytable[yyn];//we are in a new state
        state_push(yystate);   //save it
        val_push(yylval);      //push our lval as the input for next rule
        yychar = -1;           //since we have 'eaten' a token, say we need another
        if (yyerrflag > 0)     //have we recovered an error?
           --yyerrflag;        //give ourselves credit
        doaction=false;        //but don't process yet
        break;   //quit the yyn=0 loop
        }

    yyn = yyrindex[yystate];  //reduce
    if ((yyn !=0 ) && (yyn += yychar) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
      {   //we reduced!
      if (yydebug) debug("reduce");
      yyn = yytable[yyn];
      doaction=true; //get ready to execute
      break;         //drop down to actions
      }
    else //ERROR RECOVERY
      {
      if (yyerrflag==0)
        {
        yyerror("syntax error");
        yynerrs++;
        }
      if (yyerrflag < 3) //low error count?
        {
        yyerrflag = 3;
        while (true)   //do until break
          {
          if (stateptr<0)   //check for under & overflow here
            {
            yyerror("stack underflow. aborting...");  //note lower case 's'
            return 1;
            }
          yyn = yysindex[state_peek(0)];
          if ((yyn != 0) && (yyn += YYERRCODE) >= 0 &&
                    yyn <= YYTABLESIZE && yycheck[yyn] == YYERRCODE)
            {
            if (yydebug)
              debug("state "+state_peek(0)+", error recovery shifting to state "+yytable[yyn]+" ");
            yystate = yytable[yyn];
            state_push(yystate);
            val_push(yylval);
            doaction=false;
            break;
            }
          else
            {
            if (yydebug)
              debug("error recovery discarding state "+state_peek(0)+" ");
            if (stateptr<0)   //check for under & overflow here
              {
              yyerror("Stack underflow. aborting...");  //capital 'S'
              return 1;
              }
            state_pop();
            val_pop();
            }
          }
        }
      else            //discard this token
        {
        if (yychar == 0)
          return 1; //yyabort
        if (yydebug)
          {
          yys = null;
          if (yychar <= YYMAXTOKEN) yys = yyname[yychar];
          if (yys == null) yys = "illegal-symbol";
          debug("state "+yystate+", error recovery discards token "+yychar+" ("+yys+")");
          }
        yychar = -1;  //read another
        }
      }//end error recovery
    }//yyn=0 loop
    if (!doaction)   //any reason not to proceed?
      continue;      //skip action
    yym = yylen[yyn];          //get count of terminals on rhs
    if (yydebug)
      debug("state "+yystate+", reducing "+yym+" by rule "+yyn+" ("+yyrule[yyn]+")");
    if (yym>0)                 //if count of rhs not 'nil'
      yyval = val_peek(yym-1); //get current semantic value
    yyval = dup_yyval(yyval); //duplicate yyval if ParserVal is used as semantic value
    switch(yyn)
      {
//########## USER-SUPPLIED ACTIONS ##########
case 3:
//#line 204 "gramatic.y"
{ 
            addError("Contenido inesperado después del final del programa. ¿Una '}' extra?");
        }
break;
case 4:
//#line 212 "gramatic.y"
{ 
          SymbolEntry se_prog = (SymbolEntry)val_peek(0).entry;
          SymbolTable symTab = symbolTable;
          se_prog.setUso("programa"); 
          symTab.pushScope(se_prog.getLexeme()); 
          symTab.add(se_prog);

          PolacaInversa mainPI = new PolacaInversa(symbolTable);
          polacaGenerada.put(se_prog.getMangledName(), mainPI);
          pilaGestoresPolaca.push(mainPI);

          yyval.entry = val_peek(0).entry; 
      }
break;
case 5:
//#line 228 "gramatic.y"
{
        SymbolEntry se_prog = (SymbolEntry)val_peek(3).entry;
        PolacaInversa polaca = new PolacaInversa(symbolTable);
        polaca.generateFunctionEnd(se_prog);

        SymbolTable symTab = symbolTable;
        symTab.popScope(); 
        pilaGestoresPolaca.pop();

        if (!errorEnProduccion) {
            System.out.println("Línea " + lexer.getContext().getLine() + ": Estructura detectada correctamente: PROGRAMA. Ámbito cerrado.");
        }
    }
break;
case 6:
//#line 242 "gramatic.y"
{
            addError("Falta nombre de programa.");
        }
break;
case 7:
//#line 246 "gramatic.y"
{
            addError("Falta delimitador '}' de cierre del programa.");
        }
break;
case 8:
//#line 250 "gramatic.y"
{
            addError("Falta delimitador '{' de apertura del programa.");
        }
break;
case 10:
//#line 260 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 11:
//#line 262 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 12:
//#line 264 "gramatic.y"
{ 
            yyerror("Error sintáctico en sentencia. Recuperando en ';'."); 
            errorEnProduccion = false; /* REINICIAR*/
        }
break;
case 16:
//#line 275 "gramatic.y"
{ 
            addError("Error en cuerpo de función. Recuperando en ';'."); 
        }
break;
case 19:
//#line 287 "gramatic.y"
{   ArrayList<SymbolEntry> entries = (ArrayList<SymbolEntry>)val_peek(1).obj;
            boolean redeclared = false;

            for (SymbolEntry entry : entries) {
                entry.setUso("variable");
                entry.setTipo("untype"); 
                
                if (symbolTable.add(entry) ) { 
                    
                }else{
                    yyerror("Variable '" + entry.getLexeme() + "' redeclarada en el ámbito actual.", true);
                    redeclared = true;
                }
            }
            if(listaVariablesError || redeclared) {
                errorEnProduccion = true;
            } else {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Declaración de variables registrada en TS.");
            }
            listaVariablesError = false; 
        }
break;
case 20:
//#line 309 "gramatic.y"
{ 
            addError("Falta lista de variables a continuación de var.");
        }
break;
case 21:
//#line 313 "gramatic.y"
{ 
            addError("Error Sintáctico: Falta punto y coma ';' al final de la declaración de variables.");
            
            listaVariablesError = false; 
        }
break;
case 22:
//#line 321 "gramatic.y"
{ 
          yyval.entry = val_peek(0).entry; 
      }
break;
case 23:
//#line 325 "gramatic.y"
{ 
          /* Reportamos el error personalizado */
          yyerror("Error: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter '_' no está permitido en los identificadores.", true);
          
          /* RECUPERACIÓN: Asumimos que el usuario quería usar el primer ID para seguir compilando */
          yyval.entry = val_peek(2).entry; 
      }
break;
case 24:
//#line 335 "gramatic.y"
{    
        yyval.entry = val_peek(0).entry;
    }
break;
case 25:
//#line 338 "gramatic.y"
{
        /* Caso prefijado: MAIN.A*/
        SymbolEntry scopeID = (SymbolEntry)val_peek(2).entry;
        SymbolEntry varID = (SymbolEntry)val_peek(0).entry;
        
        /* Guardamos "MAIN" dentro de "A" para usarlo después en el lookup*/
        varID.setScopePrefix(scopeID.getLexeme());
        
        yyval.entry = varID;
    }
break;
case 26:
//#line 351 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            SymbolEntry se = (SymbolEntry)val_peek(0).entry;
            
            /* VALIDACIÓN: No se puede declarar con prefijo*/
            if (se.getScopePrefix() != null) {
                yyerror("Error sintáctico: No se puede utilizar prefijos ('" + se.getScopePrefix() + "') en la declaración de variables.", true);
            }
            
            list.add(se);
            yyval.obj = list;
        }
break;
case 27:
//#line 364 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            SymbolEntry se = (SymbolEntry)val_peek(0).entry;

            /* VALIDACIÓN*/
            if (se.getScopePrefix() != null) {
                yyerror("Error sintáctico: No se puede utilizar prefijos en la declaración.", true);
            }
            
            if (lista == null) lista = new ArrayList<>();
            lista.add(se);
            yyval.obj = lista; 
        }
break;
case 28:
//#line 378 "gramatic.y"
{ 
            addError("Error Sintáctico: Falta ',' en la declaración de variables.");
            listaVariablesError = true; 
            errorEnProduccion = true; /* Activar*/
        }
break;
case 29:
//#line 385 "gramatic.y"
{
        SymbolEntry entradaParser = (SymbolEntry)val_peek(0).entry;
        String lexema = entradaParser.getLexeme();
        if(lexema.equals("ERROR_IGNORE")){
            yyval.entry=entradaParser;
        }
        else{
            String prefijo = entradaParser.getScopePrefix();
        
        SymbolEntry encontrado = null;

        /* --- LÓGICA TEMA 23 ---*/
        if (prefijo != null) {
            /* Caso 2: Tiene prefijo (ej: MAIN.A) -> Buscar en ámbito específico*/
            /* Usamos el método lookup(lexema, scope) que tenías en SymbolTable*/
            encontrado = symbolTable.lookup(lexema, prefijo);
            
            if (encontrado == null) {
                yyerror("Error: La variable '" + lexema + "' no existe en el ámbito '" + prefijo + "' o no es visible.", true);
            }
        } else {
            /* Caso 1: No tiene prefijo (ej: A) -> Buscar en actual y hacia arriba*/
            encontrado = symbolTable.lookup(lexema);
            
            if (encontrado == null) {
                yyerror("Variable no declarada: '" + lexema + "'", true);
            }
        }
        /* ----------------------*/

        if (encontrado == null) {
             listaVariablesError = true; 
             errorEnProduccion = true; 
             yyval.entry = entradaParser; /* Retornamos el dummy para evitar null pointers*/
        } else {
             yyval.entry = encontrado; /* Retornamos la entrada REAL de la tabla*/
        }
    }
}
break;
case 30:
//#line 427 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)val_peek(0).entry);
            yyval.obj = list;
        }
break;
case 31:
//#line 432 "gramatic.y"
{            
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add(val_peek(0).entry);
            yyval.obj = lista; }
break;
case 32:
//#line 444 "gramatic.y"
{ 
          @SuppressWarnings("unchecked")
          ArrayList<SymbolEntry> tiposRetorno = (ArrayList<SymbolEntry>)val_peek(1).obj;
          SymbolEntry se_func = (SymbolEntry)val_peek(0).entry;
          SymbolTable symTab = symbolTable;
           
          se_func.setUso("funcion");

          if (tiposRetorno != null) {
              for (SymbolEntry tipo : tiposRetorno) {
                  /* Guardamos el lexema ("int", "float") en la definición de la función*/
                  se_func.addTipoRetorno(tipo.getLexeme()); 
              }
          }
           
          if (!symbolTable.add(se_func)) {
                yyerror("Función redeclarada '" + se_func.getLexeme() + "'", true);
            }  

          currentFunctionEntry = se_func;

          symTab.pushScope(se_func.getLexeme()); 
          PolacaInversa funcPI = new PolacaInversa(symbolTable);
          polacaGenerada.put(se_func.getMangledName(), funcPI);
          pilaGestoresPolaca.push(funcPI); 
            
          PI().generateFunctionStart(se_func);

          yyval.entry = val_peek(0).entry; 
      }
break;
case 33:
//#line 477 "gramatic.y"
{
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            PI().generateFunctionEnd(se);

            if (listaTiposError) {
                addError("Falta ',' en lista de tipos de retorno.");
            }
            
            if (!errorEnProduccion) { 
                System.out.println("Línea " + lexer.getContext().getLine() + ": Declaracion de funcion detectada " + se.getLexeme());
            }
            symbolTable.popScope();
            pilaGestoresPolaca.pop();

            listaTiposError = false; 
            currentFunctionEntry = null;
        }
break;
case 34:
//#line 495 "gramatic.y"
{ 
            addError("Falta nombre de función.");
        }
break;
case 35:
//#line 499 "gramatic.y"
{ 
            addError("Se tiene que tener mínimo un parámetro formal.");
        }
break;
case 36:
//#line 503 "gramatic.y"
{ 
            addError("Falta '{' de apertura de función.");
        }
break;
case 37:
//#line 507 "gramatic.y"
{ 
            addError("Falta '}' de cierre de función.");
        }
break;
case 38:
//#line 511 "gramatic.y"
{ 
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            listaTiposError = false; 
            addError("Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
break;
case 39:
//#line 520 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 40:
//#line 526 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 41:
//#line 533 "gramatic.y"
{
           listaTiposError = true;
           errorEnProduccion = true; 
        }
break;
case 42:
//#line 540 "gramatic.y"
{yyval.obj = new SymbolEntry("int");}
break;
case 43:
//#line 541 "gramatic.y"
{yyval.obj = new SymbolEntry("float");}
break;
case 44:
//#line 546 "gramatic.y"
{yyval.obj=val_peek(0).entry;}
break;
case 45:
//#line 548 "gramatic.y"
{
            addError("Falta ',' entre parametros.");
        }
break;
case 47:
//#line 556 "gramatic.y"
{ 
        if (!errorEnProduccion) { 
            SymbolEntry tipo = (SymbolEntry)val_peek(1).obj;
            SymbolEntry se = (SymbolEntry)val_peek(0).entry;

            se.setTipo(tipo.getLexeme());

            se.setUso("parametro");
            
            se.setMecanismoPasaje("cvr"); 
            se.setModoParametro("le");    
            
            /* 1. VINCULAR CON LA FUNCIÓN PADRE (Ya lo tenías) */
            if (currentFunctionEntry != null) {
                currentFunctionEntry.addParametro(se);
            }

            if (!symbolTable.add(se)) {
                yyerror("Error: El parámetro '" + se.getLexeme() + "' ya fue declarado en este ámbito.", true);
            }
            
            System.out.println("Parametro (Defecto CVR): " + se.getLexeme());
        }
    }
break;
case 48:
//#line 581 "gramatic.y"
{ 
        if (!errorEnProduccion) { 
            SymbolEntry tipo = (SymbolEntry)val_peek(1).obj;
            SymbolEntry se = (SymbolEntry)val_peek(0).entry;
            se.setTipo(tipo.getLexeme());
            se.setUso("parametro");
            
            String[] config = (String[])val_peek(2).semantica;
            se.setMecanismoPasaje(config[0]); 
            se.setModoParametro(config[1]);   
            
            if (currentFunctionEntry != null) {
                currentFunctionEntry.addParametro(se);
            }

            if (!symbolTable.add(se)) {
                yyerror("Error: El parámetro '" + se.getLexeme() + "' ya fue declarado en este ámbito.", true);
            }

            System.out.println("Parametro (" + config[0] + " " + config[1] + "): " + se.getLexeme());
        }
    }
break;
case 49:
//#line 604 "gramatic.y"
{
        SymbolEntry se = (SymbolEntry)val_peek(0).entry;
        
        addError("Error Sintáctico: Falta el tipo de dato  para el parámetro '" + se.getLexeme() + "'.");
        
        se.setTipo("error"); 
        se.setUso("parametro");
        
        String[] config = (String[])val_peek(1).semantica;
        se.setMecanismoPasaje(config[0]);
        se.setModoParametro(config[1]);

        if (currentFunctionEntry != null) currentFunctionEntry.addParametro(se);
        symbolTable.add(se); 
    }
break;
case 50:
//#line 620 "gramatic.y"
{
        SymbolEntry se = (SymbolEntry)val_peek(0).entry;
        
        addError("Error Sintáctico: Falta el tipo de dato para el parámetro '" + se.getLexeme() + "'.");
        
        se.setTipo("error");
        se.setUso("parametro");
        se.setMecanismoPasaje("cvr");
        se.setModoParametro("le");

        if (currentFunctionEntry != null) currentFunctionEntry.addParametro(se);
        symbolTable.add(se);
    }
break;
case 51:
//#line 637 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 52:
//#line 641 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 53:
//#line 646 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cv'.");
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 54:
//#line 651 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cr'.");
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 55:
//#line 658 "gramatic.y"
{
            ArrayList<PolacaElement> retornosReales = (ArrayList<PolacaElement>)val_peek(2).obj;
            
            /* Validar si hay errores previos de sintaxis*/
            if (listaExpresionesError) { 
                addError("Falta de ',' en argumentos de return.");
            }

            /* Validar contra la firma de la función actual*/
            if (currentFunctionEntry == null) {
                yyerror("Return fuera de contexto de función.");
            } else {
                List<String> tiposEsperados = currentFunctionEntry.getTiposRetorno();
                
                /* 1. VALIDAR CANTIDAD EXACTA (La función debe cumplir su promesa)*/
                /* Nota: El Tema 20 habla de flexibilidad en la ASIGNACIÓN, no en la DEFINICIÓN.*/
                /* Por ende, un return debe coincidir con la firma.*/
                if (retornosReales.size() != tiposEsperados.size()) {
                    yyerror("Error: La función '" + currentFunctionEntry.getLexeme() + 
                            "' debe retornar exactamente " + tiposEsperados.size() + 
                            " valores (firma declarada), pero retorna " + retornosReales.size() + ".", true);
                } else {
                    /* 2. CHEQUEO DE TIPOS INDIVIDUAL*/
                    for (int i = 0; i < retornosReales.size(); i++) {
                        String tipoReal = retornosReales.get(i).getResultType();
                        String tipoEsperado = tiposEsperados.get(i);
                        
                        if (!codigointermedio.TypeChecker.checkAssignment(tipoEsperado, tipoReal)) {
                            yyerror("Error de Tipo en retorno #" + (i+1) + 
                                    ": Se esperaba '" + tipoEsperado + 
                                    "' pero se encontró '" + tipoReal + "'.", true);
                        }
                    }
                }
            }

            /* Generar instrucción RETURN*/
            PI().generateOperation("RETURN", false);

            if (!errorEnProduccion) { 
                System.out.println("Línea " + lexer.getContext().getLine() + ": Return validado correctamente.");
            }
            listaExpresionesError = false;
        }
break;
case 59:
//#line 708 "gramatic.y"
{ 
        /* Detección específica para print*/
        addError("Error Sintáctico: Falta punto y coma ';' al final de la sentencia PRINT."); 
    }
break;
case 65:
//#line 719 "gramatic.y"
{ 
        addError("Error Sintáctico: Falta punto y coma ';' al final del PRINT (en función)."); 
    }
break;
case 69:
//#line 730 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(1).Polacaelement;
            PI().generatePrint(expr);
            
            if (!errorEnProduccion) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Print detectado");
            }
       }
break;
case 70:
//#line 739 "gramatic.y"
{
            addError("Falta argumento en print.");
       }
break;
case 73:
//#line 748 "gramatic.y"
{ 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
break;
case 75:
//#line 761 "gramatic.y"
{
          /* Recuperamos el salto BF de la pila (guardado en sentencia_if) */
          if (!pilaSaltosBF.isEmpty()) {
              List<Integer> listaBF = pilaSaltosBF.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBF, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF Simple detectado.");
      }
break;
case 76:
//#line 773 "gramatic.y"
{
          /* 1. Generar BI del THEN (Saltar el Else) */
          List<Integer> listaBI = PI().generateUnconditionalJump();
          pilaSaltosElse.push(listaBI);

          /* 2. Parchear el BF de la condición (Entrar al Else) */
          if (!pilaSaltosBF.isEmpty()) {
              List<Integer> listaBF = pilaSaltosBF.pop();
              int target_else = PI().getCurrentAddress();
              PI().backpatch(listaBF, target_else);
          }
      }
break;
case 77:
//#line 786 "gramatic.y"
{

          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado.");
      }
break;
case 78:
//#line 797 "gramatic.y"
{ 
          addError("Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
break;
case 79:
//#line 804 "gramatic.y"
{
          
          PolacaElement cond = (PolacaElement)val_peek(1).Polacaelement;
          pilaSaltosBF.push(cond.getFalseList());
      }
break;
case 81:
//#line 814 "gramatic.y"
{ addError("Falta paréntesis de apertura '(' en IF."); }
break;
case 82:
//#line 816 "gramatic.y"
{ addError("Falta paréntesis de cierre ')' en condición."); }
break;
case 83:
//#line 818 "gramatic.y"
{ addError("Error en el cuerpo de la cláusula then."); }
break;
case 84:
//#line 823 "gramatic.y"
{ 
        PolacaElement cond = (PolacaElement)val_peek(2).Polacaelement;
        List<Integer> listaBF = cond.getFalseList();

        
        if (!errorEnProduccion) { 
            System.out.println("Línea " + lexer.getContext().getLine() + 
                ": If simple detectado (sin backpatch aún)");
        }
        
        yyval.obj = listaBF;
    }
break;
case 85:
//#line 838 "gramatic.y"
{
        SymbolEntry id = (SymbolEntry)val_peek(5).entry;
        PolacaElement cte1 = val_peek(3).Polacaelement; 
        PolacaElement cte2 = val_peek(1).Polacaelement;
        
         if (!id.getTipo().equals("int") && !id.getTipo().equals("untype")) {
             yyerror("La variable del for '" + id.getLexeme() + "' debe ser de tipo 'int'.", true);
             errorEnProduccion = true;
        } else if (id.getTipo().equals("untype")) {
             id.setTipo("int"); 
        }
            
        if (!errorEnProduccion) {
        
        int val1 = 0;
        int val2 = 0;
        try {
             val1 = Integer.parseInt(cte1.getResultEntry().getLexeme());
             val2 = Integer.parseInt(cte2.getResultEntry().getLexeme());
        } catch(Exception e) {
        }
        
        PolacaElement opCte1 = PI().generateOperand(cte1.getResultEntry());
        PI().generateAssignment(id, opCte1);
        
        int labelStart = PI().getCurrentAddress();
        
        String operador;
        boolean esIncremento; 
        
        if (val1 <= val2) {
            operador = "<="; 
            esIncremento = true;
        } else {
            operador = ">="; 
            esIncremento = false;
        }
        
        PolacaElement exprId = PI().generateOperand(id);
        PolacaElement exprCte2 = PI().generateOperand(cte2.getResultEntry());
        
        PolacaElement condicion = PI().generateCondition(exprId, exprCte2, operador, "boolean");
        
        ForContext nuevoContexto = new ForContext(
            id,                     
            labelStart,             
            condicion.getFalseList(), 
            esIncremento            
        );
        
        yyval.contextfor = nuevoContexto;
        }
    }
break;
case 86:
//#line 895 "gramatic.y"
{
        ForContext ctx = val_peek(4).contextfor; 
        if (ctx != null) {
        PolacaElement opId = PI().generateOperand(ctx.variableControl);
        
        SymbolEntry unoConst = new SymbolEntry("1", "constante", "int");
        PolacaElement opUno = PI().generateOperand(unoConst);
        
        String opAritmetico = ctx.esIncremento ? "+" : "-";
        
        PolacaElement resultado = PI().generateOperation(opId, opUno, opAritmetico, "int");
        PI().generateAssignment(ctx.variableControl, resultado);
        
        List<Integer> biList = PI().generateUnconditionalJump();
        PI().backpatch(biList, ctx.labelInicio);
        
        int finDelFor = PI().getCurrentAddress();
        PI().backpatch(ctx.listaBF, finDelFor);
        }
        else {
            addError("Error interno: Contexto del For no disponible en finalización.");
        }    
        if (!errorEnProduccion) {
             System.out.println("Línea " + lexer.getContext().getLine() + ": Fin de sentencia FOR generado. Tipo: " + (ctx.esIncremento ? "Ascendente" : "Descendente"));
        }
    }
break;
case 87:
//#line 922 "gramatic.y"
{ 
            addError("Falta parentesis de apertura en encabezado del for.");
        }
break;
case 88:
//#line 926 "gramatic.y"
{
            addError("Falta nombre de variable en for.");
        }
break;
case 89:
//#line 930 "gramatic.y"
{ 
            addError("Falta palabra clave 'from' en encabezado del for.");
        }
break;
case 90:
//#line 934 "gramatic.y"
{ 
            addError("Falta constante inicial en encabezado del for.");
        }
break;
case 91:
//#line 938 "gramatic.y"
{ 
            addError("Falta palabra clave 'to' en encabezado del for.");
        }
break;
case 92:
//#line 942 "gramatic.y"
{ 
            addError("Falta constante final en encabezado del for.");
        }
break;
case 93:
//#line 946 "gramatic.y"
{ 
            addError("Falta parentesis de cierre en encabezado for.");
        }
break;
case 94:
//#line 950 "gramatic.y"
{ 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
break;
case 95:
//#line 957 "gramatic.y"
{
            PolacaElement op1 = (PolacaElement)val_peek(2).Polacaelement;
            String operator = (String)val_peek(1).sval; 
            PolacaElement op2 = (PolacaElement)val_peek(0).Polacaelement;
            
            String resultType = codigointermedio.TypeChecker.checkArithmetic(op1.getResultType(), op2.getResultType());

            if (resultType.equals("error")) {
                yyerror("Tipos incompatibles: Comparación lógica inválida entre '" + op1.getResultType() + "' y '" + op2.getResultType() + "'.", true);
                yyval.Polacaelement = PI().generateErrorElement("boolean"); 
            } else {
                yyval.Polacaelement = PI().generateCondition(op1, op2, operator, "boolean"); 
            }
        }
break;
case 96:
//#line 972 "gramatic.y"
{ 
            addError("Falta de comparador en comparación.");
        }
break;
case 97:
//#line 976 "gramatic.y"
{ 
            addError("Falta operando izquierdo en comparación.");
        }
break;
case 98:
//#line 980 "gramatic.y"
{ 
            addError("Falta operando derecho en comparación.");
        }
break;
case 99:
//#line 986 "gramatic.y"
{ yyval.sval = "=="; }
break;
case 100:
//#line 987 "gramatic.y"
{ yyval.sval = "=!"; }
break;
case 101:
//#line 988 "gramatic.y"
{ yyval.sval = "<";  }
break;
case 102:
//#line 989 "gramatic.y"
{ yyval.sval = "<="; }
break;
case 103:
//#line 990 "gramatic.y"
{ yyval.sval = ">";  }
break;
case 104:
//#line 991 "gramatic.y"
{ yyval.sval = ">="; }
break;
case 105:
//#line 996 "gramatic.y"
{
            SymbolEntry destino = (SymbolEntry)val_peek(3).entry;
            PolacaElement fuente = (PolacaElement)val_peek(1).Polacaelement;
            if (errorEnProduccion) {
                        errorEnProduccion = false; 
                    } else {
                    if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                        yyerror("El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                    } else if(destino.getUso().equals("variable")){
                        if (destino.getTipo().equals("untype")){
                            /* Si era untype, tomamos el tipo de la fuente*/
                            destino.setTipo(fuente.getResultType());
                        }
                    }

                    if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                        yyerror("Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                    }
                    
                    if (!errorEnProduccion) {
                        destino.setValue(fuente.getResultEntry().getValue());
                        symbolTable.updateValue(destino);
                        System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación simple detectada a " + destino.getLexeme());
                        PI().generateAssignment(destino, fuente);
                    }
                }
        }
break;
case 106:
//#line 1025 "gramatic.y"
{    
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)val_peek(1).obj;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)val_peek(3).obj;

        if(listaVariablesError) {
            addError("Falta ',' en la lista de variables de la asignación.");
        } 
        if(listaExpresionesError) {
            addError("Falta ',' en la lista de expresiones de la asignación.");
        } 

        if (!errorEnProduccion) {    
            boolean esInvocacionMultiple = false;

            if (listaFuentes.size() == 1) {
                PolacaElement fuente = listaFuentes.get(0);
                SymbolEntry symFuente = fuente.getResultEntry(); 
                
                if (symFuente != null) {
                    /* Caso A: Es directamente una función*/
                    if ("funcion".equals(symFuente.getUso()) 
                        && symFuente.getTiposRetorno() != null 
                        && !symFuente.getTiposRetorno().isEmpty()){
                            esInvocacionMultiple = true;
                    }
                    /* Caso B: Es un temporal que viene de una función (Corrección anterior)*/
                    else if (symFuente.getFuncionOrigen() != null) {
                        SymbolEntry funcionReal = symFuente.getFuncionOrigen();
                        if (funcionReal.getTiposRetorno() != null && !funcionReal.getTiposRetorno().isEmpty()) {
                            esInvocacionMultiple = true;
                            symFuente = funcionReal; /* Usamos la función real para los tipos*/
                        }
                    }
                }
                
                /* Lógica específica para Asignación Múltiple de Función*/
                if (esInvocacionMultiple) {
                    List<String> tiposRetorno = symFuente.getTiposRetorno();
                    
                    int numVars = listaDestinos.size();     
                    int numRets = tiposRetorno.size();      

                    if (numRets > numVars) {
                        System.err.println("WARNING (Línea " + lexer.getContext().getLine() + 
                             "): La función retorna " + numRets + " valores pero solo se asignan " + numVars + 
                             ". Los valores sobrantes se descartarán.");
                    } 
                    else if (numRets < numVars) {
                        yyerror("Error Semántico: La función retorna " + numRets + 
                                " valores, insuficiente para cubrir las " + numVars + " variables de destino.", true);
                    }

                    if (numRets >= numVars) {
                        for (int i = 0; i < numVars; i++) {
                            SymbolEntry destino = listaDestinos.get(i);
                            String tipoRetorno = tiposRetorno.get(i);

                            if ("untype".equals(destino.getTipo())) {
                                destino.setTipo(tipoRetorno);
                                if (!errorEnProduccion) {
                                    System.out.println(" Variable " + destino.getLexeme() + 
                                                       "' inferida como '" + tipoRetorno + "'");
                                }
                            }
                            if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), tipoRetorno)) {
                                yyerror("Error de Tipos en variable " + (i+1) + " ('" + destino.getLexeme() + 
                                        "'): Se esperaba compatible con '" + destino.getTipo() + 
                                        "' pero la función retorna '" + tipoRetorno + "'.", true);
                            }
                            
                            /* Generamos la asignación (Destino = Resultado_Pila)*/
                            PI().generateAssignment(destino, fuente);
                            
                            if (!errorEnProduccion) {
                                System.out.println("    -> Asignación múltiple  " + destino.getLexeme() + " (Tipo: " + tipoRetorno + ")");
                            }
                        }
                    }
                }
            } 

            if (!esInvocacionMultiple) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación estándar detectada");
                
                if (listaDestinos.size() != listaFuentes.size()) {
                    yyerror("Asignación múltiple: número de variables (" + listaDestinos.size() + 
                            ") y expresiones (" + listaFuentes.size() + ") no coinciden.", true);
                } else {
                    for (int i = 0; i < listaDestinos.size(); i++) {
                        SymbolEntry destino = listaDestinos.get(i);
                        PolacaElement fuente = listaFuentes.get(i);
                        
                        if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                            yyerror("El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                        } else if(destino.getUso().equals("variable")){
                            if (destino.getTipo().equals("untype")){
                                destino.setTipo("int"); 
                            }
                        }
                        if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                            yyerror("Tipos incompatibles: No se puede asignar '" + fuente.getResultType() 
                                    + "' a la variable '" + destino.getTipo() + "'.", true);
                        } else {
                            destino.setValue(fuente.getResultEntry().getValue());
                            symbolTable.updateValue(destino);
                            System.out.println("    Asignado a " + destino.getLexeme()+ " valor: " + destino.getValue());
                            PI().generateAssignment(destino, fuente);
                        }
                    }
                }
            }
        }
        
        listaVariablesError = false;
        listaExpresionesError = false;
    }
break;
case 107:
//#line 1142 "gramatic.y"
{ 
            addError("Error Sintáctico: Falta punto y coma ';' al final de la asignación.");
            /* Opcional: Ejecutar la lógica de asignación si quieres recuperar la semántica*/
        }
break;
case 108:
//#line 1147 "gramatic.y"
{ 
                addError("Error Sintáctico: Falta punto y coma ';' al final de la asignación múltiple.");
            }
break;
case 109:
//#line 1154 "gramatic.y"
{List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)val_peek(0).Polacaelement);
            yyval.obj = list;
        }
break;
case 110:
//#line 1158 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            List<PolacaElement> lista = (List<PolacaElement>)val_peek(2).obj;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add(val_peek(0).Polacaelement);
            yyval.obj = lista; 
    }
break;
case 111:
//#line 1168 "gramatic.y"
{
            /* 1. Reportamos el error claro y específico*/
            addError("Error Sintáctico: Falta separador ',' entre las expresiones de la lista.");
            
            /* 2. RECUPERACIÓN: Agregamos el elemento de todas formas para no romper la compilación*/
            @SuppressWarnings("unchecked")
            List<PolacaElement> lista = (List<PolacaElement>)val_peek(1).obj;
            if (lista == null) lista = new ArrayList<>();
            lista.add((PolacaElement)val_peek(0).Polacaelement); /* Agregamos la expresión ($2) a la lista*/
            
            /* 3. Devolvemos la lista "reparada"*/
            yyval.obj = lista;
        }
break;
case 112:
//#line 1185 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 113:
//#line 1187 "gramatic.y"
{
            PolacaElement elem1 = val_peek(2).Polacaelement;
            PolacaElement elem2 = val_peek(0).Polacaelement;
            
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Tipos incompatibles para la suma tipo 1: " + elem1.getResultType() + " tipo 2: " + elem2.getResultType(), true);
                yyval.Polacaelement = PI().generateErrorElement("error"); 
            } else {
                yyval.Polacaelement = PI().generateOperation(elem1, elem2, "+", tipoResultante);
            }
        }
break;
case 114:
//#line 1200 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 115:
//#line 1204 "gramatic.y"
{
            PolacaElement elem1 = val_peek(2).Polacaelement;
            PolacaElement elem2 = val_peek(0).Polacaelement;
            
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Tipos incompatibles para la resta tipo 1: " + elem1.getResultType() + " tipo 2: " + elem2.getResultType(), true);
                yyval.Polacaelement = PI().generateErrorElement("error"); 
            } else {
                yyval.Polacaelement = PI().generateOperation(elem1, elem2, "-", tipoResultante);
            }
        }
break;
case 116:
//#line 1217 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 117:
//#line 1224 "gramatic.y"
{
        yyval.Polacaelement = val_peek(0).Polacaelement;
    }
break;
case 118:
//#line 1227 "gramatic.y"
{
        PolacaElement term = val_peek(2).Polacaelement;
        PolacaElement fact = val_peek(0).Polacaelement;
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Tipos incompatibles para la multiplicación tipo 1: " + term.getResultType() + " tipo 2: " + fact.getResultType(), true);
            yyval.Polacaelement = PI().generateErrorElement("error"); 
        } else {
            yyval.Polacaelement = PI().generateOperation(term, fact, "*", tipoResultante);
        }
    }
break;
case 119:
//#line 1239 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 120:
//#line 1243 "gramatic.y"
{
        PolacaElement term = val_peek(2).Polacaelement;
        PolacaElement fact = val_peek(0).Polacaelement;
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Tipos incompatibles para la división tipo 1: " + term.getResultType() + " tipo 2: " + fact.getResultType(), true);
            yyval.Polacaelement = PI().generateErrorElement("error"); 
        } else {
            yyval.Polacaelement = PI().generateOperation(term, fact, "/", tipoResultante);
        }
    }
break;
case 121:
//#line 1255 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 122:
//#line 1262 "gramatic.y"
{ 
            SymbolEntry entradaParser = (SymbolEntry)val_peek(0).entry;
            String lexema = entradaParser.getLexeme();
            if(lexema.equals("ERROR_IGNORE")){
                yyval.Polacaelement=PI().generateErrorElement("error");
            }
            else{
                String prefijo = entradaParser.getScopePrefix();
            SymbolEntry entry = null;

            /* --- LÓGICA DE BÚSQUEDA (Igual que en identificador_destino) ---*/
            if (prefijo != null) {
                entry = symbolTable.lookup(entradaParser.getLexeme(), prefijo);
                if (entry == null) yyerror("Variable '" + entradaParser.getLexeme() + "' no encontrada en ámbito '" + prefijo + "'.", true);
            } else {
                entry = symbolTable.lookup(entradaParser.getLexeme());
                if (entry == null) yyerror("Variable '" + entradaParser.getLexeme() + "' no declarada.", true);
            }
            
            /* Generación de error o código*/
            if (entry == null) {
                 yyval.Polacaelement = PI().generateErrorElement("error");
            } else if (entry.getUso().equals("funcion")) {
                 yyerror("Uso incorrecto de función como variable.", true);
                 yyval.Polacaelement = PI().generateErrorElement("error");
            } else {
                 yyval.Polacaelement = PI().generateOperand(entry);
            }
        }
    }
break;
case 123:
//#line 1293 "gramatic.y"
{
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 124:
//#line 1300 "gramatic.y"
{ 
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 125:
//#line 1307 "gramatic.y"
{ 
          /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
          SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
          symbolTable.add(se_const);
          yyval.Polacaelement = PI().generateOperand(val_peek(0).entry); 
      }
break;
case 126:
//#line 1314 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 127:
//#line 1316 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 128:
//#line 1322 "gramatic.y"
{   
        SymbolEntry funcion = (SymbolEntry)val_peek(3).entry;
        
        /* 1. Validar que sea una función*/
        if (!"funcion".equals(funcion.getUso())) {
            yyerror("El identificador '" + funcion.getLexeme() + "' no es una función.", true);
            yyval.Polacaelement = PI().generateErrorElement("error");
        } else {
            @SuppressWarnings("unchecked")
            ArrayList<ParametroInvocacion> reales = (ArrayList<ParametroInvocacion>)val_peek(1).listParamInv;
            if (reales == null) reales = new ArrayList<>();

            List<SymbolEntry> formales = funcion.getParametros(); /* Lista ordenada de params formales*/
            
            /* 2. Validar Cantidad*/
            if (reales.size() != formales.size()) {
                 yyerror("La función '" + funcion.getLexeme() + "' espera " + formales.size() + 
                         " parámetros, pero se recibieron " + reales.size() + ".", true);
            }
            
            for (SymbolEntry formal : formales) {
                boolean encontrado = false;
                ParametroInvocacion paramRealMatch = null;

                /* Buscamos en la lista desordenada de la invocación*/
                for (ParametroInvocacion real : reales) {
                    if (real.getNombreDestino().equals(formal.getLexeme())) {
                        encontrado = true;
                        paramRealMatch = real;
                        break;
                    }
                }

                if (!encontrado) {
                    yyerror("Error: Falta el parámetro obligatorio '" + formal.getLexeme() + 
                            "' en la invocación de '" + funcion.getLexeme() + "'.", true);
                } else {

                    if ("string".equals(paramRealMatch.getValor().getResultType())) {
                    yyerror("Error Semántico: No se permite pasar cadenas de caracteres (STRING) como parámetros a funciones.", true);
                    }
                    
                    if (!codigointermedio.TypeChecker.checkAssignment(formal.getTipo(), paramRealMatch.getValor().getResultType())) {
                         yyerror("Error de Tipo en parámetro '" + formal.getLexeme() + 
                                 "'. Se esperaba '" + formal.getTipo() + 
                                 "' pero se recibió '" + paramRealMatch.getValor().getResultType() + "'.", true);
                    }
                    
                   PI().generateAssignment(formal, paramRealMatch.getValor());
                }
            }

            /* 6. Generar la llamada a la función (CALL)*/
            yyval.Polacaelement = PI().generateCall(funcion); 

            for (SymbolEntry formal : formales) {
                ParametroInvocacion paramRealMatch = null;
                for (ParametroInvocacion real : reales) {
                    if (real.getNombreDestino().equals(formal.getLexeme())) {
                        paramRealMatch = real;
                        break;
                    }
                }

                if (paramRealMatch != null) {
                    String mecanismo = formal.getMecanismoPasaje(); 
                    
                    if ("cr".equals(mecanismo) || "cvr".equals(mecanismo)) {
                        
                        SymbolEntry realSym = paramRealMatch.getValor().getResultEntry();
                        
                        if (realSym != null && "variable".equals(realSym.getUso())) {
                            
                            PolacaElement valorFormal = new PolacaElement(formal.getTipo(), formal);
                            PI().generateAssignment(realSym, valorFormal);
                            
                            if (!errorEnProduccion) {
                                System.out.println("   -> Copia-Resultado (Tema 26): " + realSym.getLexeme() + " actualizado con " + formal.getLexeme());
                            }
                        } else {
                             yyerror("Error Semántico: El parámetro '" + formal.getLexeme() + 
                                     "' es de tipo Copia-Resultado, por lo que el parámetro real debe ser una variable, no una constante o expresión.", true);
                        }
                    }
                }
            }
            
            if (!errorEnProduccion) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Invocación correcta a " + funcion.getLexeme());
            }
        }
    }
break;
case 129:
//#line 1418 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(1).Polacaelement;
            
            if (!expr.getResultType().equals("int")) {
                 yyerror("Se intentó convertir a float una expresión que es de tipo '" + expr.getResultType() + "'.", true);
                 yyval.Polacaelement = expr; 
            } else {
                yyval.Polacaelement = PI().generateTOF(expr);
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ":  Conversión explícita TOF generada correctamente."); 
                }
            }
        }
break;
case 130:
//#line 1432 "gramatic.y"
{ 
            addError("Falta el '(' en la conversión explícita.");
        }
break;
case 131:
//#line 1436 "gramatic.y"
{ 
            addError("Falta el ')' en la conversión explícita.");
        }
break;
case 132:
//#line 1443 "gramatic.y"
{
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 133:
//#line 1450 "gramatic.y"
{
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)val_peek(2).listParamInv;
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 134:
//#line 1461 "gramatic.y"
{
        PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
        SymbolEntry idParam = (SymbolEntry)val_peek(0).entry;
        
        yyval.paramInv = new ParametroInvocacion(idParam.getLexeme(), expr);
        
        if (!errorEnProduccion) { 
            System.out.println("   -> Parametro nombrado detectado: " + idParam.getLexeme());
        }
    }
break;
case 135:
//#line 1472 "gramatic.y"
{ 
        addError("Sintaxis incorrecta en parámetro. Se espera 'valor -> nombre'.");
        yyval.paramInv = new ParametroInvocacion("error", (PolacaElement)val_peek(1).Polacaelement); /* Dummy para no romper todo*/
    }
break;
case 136:
//#line 1480 "gramatic.y"
{ 
        SymbolEntry tipoFormal = (SymbolEntry)val_peek(2).obj;
        SymbolEntry formal = (SymbolEntry)val_peek(1).entry;
        
        formal.setTipo(tipoFormal.getLexeme());
        formal.setUso("parametro_lambda");

        String uniqueScopeName = "LAMBDA_ANON_" + (++lambdaCounter);
        symbolTable.pushScope(uniqueScopeName);
        symbolTable.add(formal);

        currentLambdaFormal = formal;
        
        lambdaBodyBuffer = new PolacaInversa(symbolTable);
        
        /* Cambiar el gestor activo a la Polaca temporal */
        pilaGestoresPolaca.push(lambdaBodyBuffer);
        
        yyval.obj = formal;
    }
break;
case 137:
//#line 1503 "gramatic.y"
{
        SymbolEntry formal = currentLambdaFormal;
        PolacaElement real = (PolacaElement)val_peek(1).Polacaelement; /* lambda_argumento*/
    
        @SuppressWarnings("unchecked")
        List<Integer> listaBF = (List<Integer>)val_peek(4).obj; /* if_simple retorna la lista*/
        

        pilaGestoresPolaca.pop(); /* Sacamos lambdaBodyBuffer*/
        
        lambdaAssignBuffer = new PolacaInversa(symbolTable);
        
        /* Generar la asignación en el buffer temporal */
        lambdaAssignBuffer.generateAssignment(formal, real);
        
        
        PolacaInversa mainPI = PI(); /* Polaca principal (MAINLAMBDATEST)*/
        
        /* 4.1: Añadir la asignación primero */
        int offsetAsignacion = mainPI.getCurrentAddress();
        mainPI.appendPolaca(lambdaAssignBuffer);
        
        int offsetCuerpo = mainPI.getCurrentAddress();
        mainPI.appendPolaca(lambdaBodyBuffer);
        
        
        int desplazamientoAsignacion = lambdaAssignBuffer.getSize();
        
        List<Integer> listaBFAjustada = new ArrayList<>();
        for (int bf : listaBF) {
            listaBFAjustada.add(bf + offsetAsignacion + desplazamientoAsignacion - 1);
        }
        
        int targetEndIf = mainPI.getCurrentAddress();
        
        mainPI.backpatch(listaBFAjustada, targetEndIf);
        
        lambdaBodyBuffer = null;
        lambdaAssignBuffer = null;
        currentLambdaFormal = null;
        
        symbolTable.popScope();
        
        if (!errorEnProduccion) {
            System.out.println("Línea " + lexer.getContext().getLine() + 
                ": Lambda correcta para " + formal.getLexeme());
        }
        
        yyval.Polacaelement = new PolacaElement("void");
    }
break;
case 138:
//#line 1555 "gramatic.y"
{ 
        addError("Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 139:
//#line 1564 "gramatic.y"
{ 
        addError("Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 140:
//#line 1574 "gramatic.y"
{ 
        addError("Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 141:
//#line 1583 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 142:
//#line 1587 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 143:
//#line 1591 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
//#line 2437 "Parser.java"
//########## END OF USER-SUPPLIED ACTIONS ##########
    }//switch
    //#### Now let's reduce... ####
    if (yydebug) debug("reduce");
    state_drop(yym);             //we just reduced yylen states
    yystate = state_peek(0);     //get new state
    val_drop(yym);               //corresponding value drop
    yym = yylhs[yyn];            //select next TERMINAL(on lhs)
    if (yystate == 0 && yym == 0)//done? 'rest' state and at first TERMINAL
      {
      if (yydebug) debug("After reduction, shifting from state 0 to state "+YYFINAL+"");
      yystate = YYFINAL;         //explicitly say we're done
      state_push(YYFINAL);       //and save it
      val_push(yyval);           //also save the semantic value of parsing
      if (yychar < 0)            //we want another character?
        {
        yychar = yylex();        //get next character
        if (yychar<0) yychar=0;  //clean, if necessary
        if (yydebug)
          yylexdebug(yystate,yychar);
        }
      if (yychar == 0)          //Good exit (if lex returns 0 ;-)
         break;                 //quit the loop--all DONE
      }//if yystate
    else                        //else not done yet
      {                         //get next state and push, for next yydefred[]
      yyn = yygindex[yym];      //find out where to go
      if ((yyn != 0) && (yyn += yystate) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yystate)
        yystate = yytable[yyn]; //get new state
      else
        yystate = yydgoto[yym]; //else go to new defred
      if (yydebug) debug("after reduction, shifting from state "+state_peek(0)+" to state "+yystate+"");
      state_push(yystate);     //going again, so push state & val...
      val_push(yyval);         //for next action
      }
    }//main loop
  return 0;//yyaccept!!
}
//## end of method parse() ######################################



//## run() --- for Thread #######################################
/**
 * A default run method, used for operating this parser
 * object in the background.  It is intended for extending Thread
 * or implementing Runnable.  Turn off with -Jnorun .
 */
public void run()
{
  yyparse();
}
//## end of method run() ########################################





/**
 * Create a parser, setting the debug to true or false.
 * @param debugMe true for debugging, false for no debug.
 */
public Parser(boolean debugMe)
{
  yydebug=debugMe;
}
//###############################################################



}
//################### END OF CLASS ##############################
