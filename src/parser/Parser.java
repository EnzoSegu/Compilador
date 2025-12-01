//### This file created by BYACC 1.8(/Java extension  1.15)
//### Java capabilities added 7 Jan 97, Bob Jamison
//### Updated : 27 Nov 97  -- Bob Jamison, Joe Nieten
//###           01 Jan 98  -- Bob Jamison -- fixed generic semantic constructor
//###           01 Jun 99  -- Bob Jamison -- added Runnable support
//###           06 Aug 00  -- Bob Jamison -- made state variables class-global
//###           03 Jan 01  -- Bob Jamison -- improved flags, tracing
//###           16 May 01  -- Bob Jamison -- added custom stack sizing
//###           04 Mar 02  -- Yuval Oren  -- improved java performance, added options
//###           14 Mar 02  -- Tomas Hurka -- -d support, static initializer workaround
//### Please send bug reports to tom@hukatronic.cz
//### static char yysccsid[] = "@(#)yaccpar	1.8 (Berkeley) 01/20/90";


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
    0,    0,    0,   11,   27,   27,   27,   27,   28,   28,
   28,   28,   31,   31,   31,   31,   29,   29,   34,   34,
   34,   10,   10,   15,   15,   15,   12,   19,   19,   13,
   33,   33,   33,   33,   33,   33,   20,   20,   20,   16,
   16,   17,   17,   17,   14,   14,   14,   14,   24,   24,
   24,   24,   35,   30,   30,   30,   30,   30,   30,   32,
   32,   32,   32,   32,   37,   37,   40,   40,   40,   41,
   42,   43,   42,   42,   44,   38,   38,   38,   38,   21,
    1,   39,   39,   39,   39,   39,   39,   39,   39,   39,
    7,    7,    7,    7,   23,   23,   23,   23,   23,   23,
   36,   36,   18,   18,   18,    2,    2,    2,    2,    2,
    3,    3,    3,    3,    3,    4,    4,    4,    4,    4,
    4,    6,    5,    5,    5,   25,   25,   26,   26,   22,
    9,    9,    9,    9,    8,    8,    8,
};
final static short yylen[] = {                            2,
    0,    1,    2,    1,    4,    4,    4,    3,    0,    2,
    2,    3,    0,    2,    2,    3,    1,    1,    3,    2,
    3,    1,    3,    1,    3,    3,    1,    1,    3,    2,
    9,   10,    9,    9,    9,    9,    1,    3,    2,    1,
    1,    1,    3,    3,    2,    3,    2,    1,    2,    2,
    2,    2,    5,    2,    1,    2,    1,    1,    1,    2,
    2,    1,    1,    1,    4,    4,    0,    2,    3,    3,
    2,    0,    5,    1,    0,    7,    6,    6,    6,    5,
    8,    5,   10,   10,   10,   10,   10,   10,   10,    3,
    3,    2,    3,    3,    1,    1,    1,    1,    1,    1,
    3,    3,    1,    3,    3,    1,    3,    3,    3,    3,
    1,    3,    3,    3,    3,    1,    1,    1,    1,    1,
    1,    4,    4,    4,    4,    1,    3,    3,    2,    4,
    7,    7,    7,    7,    1,    1,    1,
};
final static short yydefred[] = {                         0,
    0,    4,    0,    0,    0,    9,    9,    0,    3,    0,
    0,    0,   22,    0,    0,    0,    0,    0,    0,   40,
   41,    0,   59,    0,    0,    0,   37,    0,    0,    0,
   10,   11,   17,   18,   55,    0,    0,   57,   58,    0,
    6,    0,    5,   12,    0,    0,    0,    0,    0,   20,
    0,    0,    0,    0,    0,   67,    0,    0,    0,    0,
    0,    0,   30,    0,    0,   39,    0,    0,   54,   56,
    0,    0,  119,  117,  118,    0,    0,    0,  111,  121,
  120,    0,    0,    0,    0,    0,    0,    0,   21,    0,
   19,    0,   90,    0,    0,    0,    0,    0,   23,    0,
    0,    0,   48,    0,    0,   42,    0,    0,    0,    0,
   29,    0,   38,    0,    0,    0,   95,   96,   97,   98,
   99,  100,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,   66,   65,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  130,    0,    0,   68,
    0,    0,   51,   49,   52,   50,   45,    0,    0,    0,
   47,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  126,    0,    0,  108,    0,  110,    0,   94,    0,
  113,  112,  115,  114,   67,    0,    0,    0,    0,    0,
   53,    0,    0,    0,    0,    0,    0,   69,   82,    0,
   13,   13,   43,   13,   13,   44,   46,   13,    0,    0,
    0,    0,  129,    0,  122,    0,  124,  125,  123,    0,
   74,   72,    0,   77,   78,   79,    0,    0,    0,    0,
    0,    0,    0,   13,    0,    0,    0,    0,    0,    0,
  135,  136,  137,    0,    0,    0,  128,  127,   70,    0,
   71,   76,    0,    0,    0,    0,    0,    0,    0,    0,
    0,   64,   14,   15,    9,    0,    0,   62,   63,    9,
    9,    9,    9,   80,  132,  133,  134,  131,    0,    0,
    0,    0,    0,    0,    0,    0,   81,    9,   16,    0,
   60,   61,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,   33,   36,   34,   31,    0,
   73,   83,   84,   85,   86,   87,   88,   89,   32,
};
final static short yydgoto[] = {                          3,
   22,   77,   78,   79,   80,   81,   82,  244,   23,   83,
    4,   25,   26,  106,   52,   27,  108,   88,   28,   29,
  115,   30,  123,  109,  171,  172,    5,    8,   31,   32,
  235,  264,   33,   34,   35,   36,   37,   38,   39,   98,
  186,  224,  250,  189,
};
final static short yysindex[] = {                        54,
 -256,    0,    0, -128, -223,    0,    0,   61,    0, -190,
 -152, -193,    0, -156, -171, -164, -243,  -91,   93,    0,
    0, -108,    0,  -50,  -69,  -85,    0, -259,  -93,  -81,
    0,    0,    0,    0,    0,  -45,  -29,    0,    0, -193,
    0, -193,    0,    0, -209, -209, -120, -116,  -24,    0,
  -50, -160, -232,  158,   13,    0,   50, -116,   60, -239,
 -116,   97,    0, -228,   93,    0,   16,   16,    0,    0,
  428,   82,    0,    0,    0,   -1,  329,  159,    0,    0,
    0,   90,  -50,    9,  112, -251,  199, -173,    0,   97,
    0,   97,    0,  111,  133, -229,  143,  355,    0,  199,
 -228,  163,    0, -225, -213,    0,  208,   -5, -166, -154,
    0,   40,    0,  205,  207, -157,    0,    0,    0,    0,
    0,    0, -116, -116, -116, -116,  -49,   78,  199,  128,
  146,  185,  209,  209,  244,    0,    0, -116,  215, -116,
  -50,  -50, -116, -116, -116,  224,    0,  217,  221,    0,
  100,  225,    0,    0,    0,    0,    0, -219,  -66, -228,
    0,  253, -228,  227, -209,  243,  248,  250,  199,  -34,
  -30,    0,  220,   99,    0,  159,    0,  159,    0,  199,
    0,    0,    0,    0,    0,  191,  191,  191,  209,  199,
    0,  199,  270,  272,  273,  278,  -35,    0,    0,  259,
    0,    0,    0,    0,    0,    0,    0,    0,  265,  204,
  204,  204,    0,  292,    0, -116,    0,    0,    0,  369,
    0,    0,  276,    0,    0,    0,  191, -116, -116, -116,
 -116, -116,  263,    0,   83,   83,   83,   83,   83,  374,
    0,    0,    0,  267,  277,   20,    0,    0,    0,  209,
    0,    0,  282,  285,  286,  287,  288,  295,   37,   83,
  293,    0,    0,    0,    0,  294,  304,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  298,  209,
  209,  209,  209,  209,  209,  209,    0,    0,    0,  -98,
    0,    0,  -84,  -41,  -18,   96,  306,  309,  310,  314,
  315,  316,  317,  326,   34,    0,    0,    0,    0, -193,
    0,    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yyrindex[] = {                       559,
    0,    0,    0,  110,  582,    0,    0,    0,    0,    0,
    0,    4,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,   -6, -104,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    5,    0,    0,    0,    0,    0,    0,    0,    0,
  -94,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  140,    0,    0,    0,    0,    0,  218,    0,    0,
    0,    0,  179,    0,    0,    0,  -95,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,  327,
    0,    0,    0,    0,    0,    0,    0,    0,    0,  328,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,   38,    0,
    0,    0,    0,    0,  333,    0,    0,    0,    0,    0,
  -70,  -54,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,   68,    0,
    0,    0,    0,    0,    0,  257,    0,  296,    0,   85,
    0,    0,    0,    0,    0,    0,    0,    0,    0,   19,
    0,  275,    0,    0,    0,    0,    0,    0,    0,    0,
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
    0,    0,    0,    0,    0,    0,    0,    0,    0,   48,
    0,    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
    0,  -37,  344, -109,    0,    0,  -40,  274,  407,   -8,
    0,  371,    0,  366,    0,   -3,   -4,  557,    0,    0,
  553,    0,  550,    0,    0,  412,    0,    1,  422,  -86,
  390,    0,    0,    0,  427,  433,  438,  453,  464,  444,
 -133, -146,    0,    0,
};
final static int YYTABLESIZE=724;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         24,
  187,   24,   24,    8,    7,   84,   10,   11,   51,   86,
   87,  150,   49,   13,   61,   55,  102,  103,  127,  128,
  100,  182,  184,   87,   13,   66,  145,    6,  103,   62,
  153,  137,    9,  193,  194,  195,  197,  103,  146,  129,
  225,  226,  155,   50,   24,   24,   71,   72,   73,   74,
   75,  104,  105,   24,   93,  227,  107,   20,   21,  112,
  107,  113,  104,  105,  202,   40,   13,  154,   20,   21,
   14,  104,  105,   15,   16,   17,   18,   20,   21,  156,
  252,  141,  138,  142,   76,  169,  170,  173,  174,   24,
  161,   19,  180,   44,   41,   90,  151,  107,  167,   45,
  190,  138,  192,   42,   13,  162,   20,   21,   14,  139,
   47,   15,   16,   17,   18,  140,  279,   48,  253,  254,
  255,  256,  257,  259,  209,   46,   91,  168,   92,   19,
   20,   21,   43,  150,  140,   85,   72,   73,   74,   75,
   72,   73,   74,   75,   20,   21,  298,  299,  300,  301,
  302,  303,  304,  274,  107,    7,  107,   40,   13,  107,
  103,   24,   14,   63,   53,   15,   16,   17,   18,   28,
   59,   40,   13,   76,   67,   56,   14,   76,  170,   15,
   16,   17,   18,   19,   28,   26,  306,  103,   64,  204,
   54,  103,   24,  103,   24,   65,   60,   19,   20,   21,
  307,   25,   68,   20,   21,   58,  175,   72,   73,   74,
   75,   24,   20,   21,   40,   13,   26,  205,   26,   14,
  232,  213,   15,   16,   17,   18,   24,   24,   24,   24,
   24,   24,   25,  233,   25,  127,  128,   40,   13,   57,
   19,   69,   14,  308,   76,   15,   16,   17,   18,   27,
  158,   24,  215,  214,  125,   20,   21,   70,  216,    8,
    7,   27,   89,   19,  134,  290,  309,   27,   27,   97,
  293,  294,  295,  296,  105,  277,  114,  159,   20,   21,
  126,   24,   27,  160,   24,   24,   24,   24,  305,   40,
   13,  135,  286,   92,   14,  163,   24,   15,   16,   17,
   18,  105,  278,   35,   35,  105,   99,  105,   35,    1,
    2,   35,   35,   35,   35,   19,   12,   13,  319,  287,
   92,   14,  164,   93,   15,   16,   17,   18,  160,   35,
   20,   21,   35,  177,   72,   73,   74,   75,  261,   13,
   91,  101,   19,   14,   35,   35,   15,   16,   17,   18,
   93,  310,   13,   13,  218,  163,   14,   20,   21,   15,
   16,   17,   18,  124,   19,    9,    9,   91,  127,  128,
    9,   76,  133,    9,    9,    9,    9,   19,  143,   20,
   21,  219,  200,  179,   72,   73,   74,   75,  160,   20,
   21,    9,   20,   21,  136,   22,   22,   22,   22,   22,
  144,  181,   72,   73,   74,   75,    9,    9,   22,   22,
   22,   22,   22,   95,   13,   22,   22,   22,   22,   22,
   22,   76,   22,   94,   96,  147,   22,   22,   22,   22,
  131,  132,  111,   22,  116,  116,  116,  116,  116,   76,
  183,   72,   73,   74,   75,  152,  221,  116,  116,  116,
  116,  116,  222,  223,  116,  116,  116,  116,  116,  116,
  241,  116,  242,  243,  157,  116,  116,  116,  127,  128,
  176,  178,  116,  106,  106,  106,  106,  106,   76,  196,
   72,   73,   74,   75,  245,  246,  165,  106,  106,  127,
  128,  166,  185,  106,  106,  106,  106,  106,  106,  188,
  106,  191,  217,  198,  106,  106,  106,  199,  201,  207,
  208,  106,  107,  107,  107,  107,  107,   76,  258,   72,
   73,   74,   75,  203,  210,  206,  107,  107,  203,  211,
  104,  212,  107,  107,  107,  107,  107,  107,  228,  107,
  229,  230,  234,  107,  107,  107,  231,  240,  247,  275,
  107,  109,  109,  109,  109,  109,   76,  104,    1,  276,
  297,  104,  251,  104,  280,  109,  109,  281,  282,  283,
  284,  109,  109,  109,  109,  109,  109,  285,  109,  289,
  291,    2,  109,  109,  109,   72,   73,   74,   75,  109,
  292,  236,  311,  237,  238,  312,  313,  239,  127,  128,
  314,  315,  316,  317,  117,  118,  119,  120,  121,  122,
  148,   13,  318,  101,  102,   14,   75,  110,   15,   16,
  116,   18,   76,  260,  148,   13,  130,  248,  220,   14,
   13,    0,   15,   16,   14,   18,   19,   15,   16,  149,
   18,  262,  262,  262,  262,  262,    0,    0,    0,    0,
   19,    0,    0,  249,    0,   19,  263,  263,  263,  263,
  263,  265,  270,  271,  272,  273,  262,  266,  266,  266,
  266,  266,  267,  267,  267,  267,  267,    0,    0,    0,
    0,  263,    0,    0,    0,    0,  288,  268,  268,  268,
  268,  268,  266,    0,    0,    0,    0,  267,  269,  269,
  269,  269,  269,  117,  118,  119,  120,  121,  122,    0,
    0,    0,  268,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  269,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                          8,
  134,   10,   11,    0,    0,   46,    6,    7,   17,   47,
   48,   98,  256,  257,  274,   19,  256,  257,  270,  271,
   58,  131,  132,   61,  257,   29,  256,  284,  257,  289,
  256,  283,  256,  143,  144,  145,  146,  257,  268,   77,
  187,  188,  256,  287,   53,   54,  256,  257,  258,  259,
  260,  291,  292,   62,  287,  189,   60,  297,  298,   64,
   64,   65,  291,  292,  284,  256,  257,  293,  297,  298,
  261,  291,  292,  264,  265,  266,  267,  297,  298,  293,
  227,   90,  256,   92,  294,  123,  124,  125,  126,   98,
  257,  282,  130,  287,  285,  256,  101,  101,  256,  256,
  138,  256,  140,  256,  257,  109,  297,  298,  261,  283,
  282,  264,  265,  266,  267,  289,  250,  282,  228,  229,
  230,  231,  232,  233,  165,  282,  287,  285,  289,  282,
  297,  298,  285,  220,  289,  256,  257,  258,  259,  260,
  257,  258,  259,  260,  297,  298,  280,  281,  282,  283,
  284,  285,  286,  240,  158,  284,  160,  256,  257,  163,
  256,  256,  261,  257,  256,  264,  265,  266,  267,  274,
  256,  256,  257,  294,  256,  284,  261,  294,  216,  264,
  265,  266,  267,  282,  289,  256,  285,  283,  282,  256,
  282,  287,  287,  289,  289,  289,  282,  282,  297,  298,
  285,  256,  284,  297,  298,  275,  256,  257,  258,  259,
  260,  220,  297,  298,  256,  257,  287,  284,  289,  261,
  256,  256,  264,  265,  266,  267,  235,  236,  237,  238,
  239,  240,  287,  269,  289,  270,  271,  256,  257,  290,
  282,  287,  261,  285,  294,  264,  265,  266,  267,  256,
  256,  260,  283,  288,  256,  297,  298,  287,  289,  256,
  256,  268,  287,  282,  256,  265,  285,  274,  275,  257,
  270,  271,  272,  273,  256,  256,  261,  283,  297,  298,
  282,  290,  289,  289,  293,  294,  295,  296,  288,  256,
  257,  283,  256,  256,  261,  256,  305,  264,  265,  266,
  267,  283,  283,  256,  257,  287,  257,  289,  261,  256,
  257,  264,  265,  266,  267,  282,  256,  257,  285,  283,
  283,  261,  283,  256,  264,  265,  266,  267,  289,  282,
  297,  298,  285,  256,  257,  258,  259,  260,  256,  257,
  256,  282,  282,  261,  297,  298,  264,  265,  266,  267,
  283,  256,  257,  257,  256,  256,  261,  297,  298,  264,
  265,  266,  267,  282,  282,  256,  257,  283,  270,  271,
  261,  294,  283,  264,  265,  266,  267,  282,  268,  297,
  298,  283,  283,  256,  257,  258,  259,  260,  289,  297,
  298,  282,  297,  298,  283,  256,  257,  258,  259,  260,
  268,  256,  257,  258,  259,  260,  297,  298,  269,  270,
  271,  272,  273,  256,  257,  276,  277,  278,  279,  280,
  281,  294,  283,   53,   54,  283,  287,  288,  289,  290,
  272,  273,   62,  294,  256,  257,  258,  259,  260,  294,
  256,  257,  258,  259,  260,  283,  256,  269,  270,  271,
  272,  273,  262,  263,  276,  277,  278,  279,  280,  281,
  257,  283,  259,  260,  257,  287,  288,  289,  270,  271,
  127,  128,  294,  256,  257,  258,  259,  260,  294,  256,
  257,  258,  259,  260,  211,  212,  282,  270,  271,  270,
  271,  285,  284,  276,  277,  278,  279,  280,  281,  256,
  283,  287,  283,  287,  287,  288,  289,  287,  284,  257,
  284,  294,  256,  257,  258,  259,  260,  294,  256,  257,
  258,  259,  260,  158,  282,  160,  270,  271,  163,  282,
  256,  282,  276,  277,  278,  279,  280,  281,  269,  283,
  269,  269,  284,  287,  288,  289,  269,  283,  257,  283,
  294,  256,  257,  258,  259,  260,  294,  283,    0,  283,
  263,  287,  287,  289,  283,  270,  271,  283,  283,  283,
  283,  276,  277,  278,  279,  280,  281,  283,  283,  287,
  287,    0,  287,  288,  289,  257,  258,  259,  260,  294,
  287,  202,  287,  204,  205,  287,  287,  208,  270,  271,
  287,  287,  287,  287,  276,  277,  278,  279,  280,  281,
  256,  257,  287,  287,  287,  261,  284,   61,  264,  265,
   68,  267,  294,  234,  256,  257,   77,  216,  185,  261,
  257,   -1,  264,  265,  261,  267,  282,  264,  265,  285,
  267,  235,  236,  237,  238,  239,   -1,   -1,   -1,   -1,
  282,   -1,   -1,  285,   -1,  282,  235,  236,  237,  238,
  239,  235,  236,  237,  238,  239,  260,  235,  236,  237,
  238,  239,  235,  236,  237,  238,  239,   -1,   -1,   -1,
   -1,  260,   -1,   -1,   -1,   -1,  260,  235,  236,  237,
  238,  239,  260,   -1,   -1,   -1,   -1,  260,  235,  236,
  237,  238,  239,  276,  277,  278,  279,  280,  281,   -1,
   -1,   -1,  260,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,  260,
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
"inicio_programa : ID",
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
"declaracion_variable : VAR error SEMICOLON",
"identificador_completo : ID",
"identificador_completo : identificador_completo POINT ID",
"lista_variables : identificador_completo",
"lista_variables : lista_variables COMMA identificador_completo",
"lista_variables : lista_variables error identificador_completo",
"identificador_destino : identificador_completo",
"lista_variables_destino : identificador_destino",
"lista_variables_destino : lista_variables_destino COMMA identificador_destino",
"inicio_funcion : lista_tipos ID",
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
"sentencia_ejecutable : asignacion SEMICOLON",
"sentencia_ejecutable : sentencia_return",
"sentencia_ejecutable : sentencia_print SEMICOLON",
"sentencia_ejecutable : sentencia_if",
"sentencia_ejecutable : sentencia_for",
"sentencia_ejecutable : lambda_expresion",
"sentencia_ejecutable_sin_return : asignacion SEMICOLON",
"sentencia_ejecutable_sin_return : sentencia_print SEMICOLON",
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
"asignacion : identificador_destino ASSIGN_COLON expresion",
"asignacion : lista_variables_destino ASSIGN lista_expresiones",
"lista_expresiones : expresion",
"lista_expresiones : lista_expresiones COMMA expresion",
"lista_expresiones : lista_expresiones error expresion",
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

//#line 1547 "gramatic.y"

/* ======= Código Java adicional (opcional) ======= */
//#line 769 "Parser.java"
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
//#line 203 "gramatic.y"
{ 
            addError("Contenido inesperado después del final del programa. ¿Una '}' extra?");
        }
break;
case 4:
//#line 211 "gramatic.y"
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
//#line 227 "gramatic.y"
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
//#line 241 "gramatic.y"
{
            addError("Falta nombre de programa.");
        }
break;
case 7:
//#line 245 "gramatic.y"
{
            addError("Falta delimitador '}' de cierre del programa.");
        }
break;
case 8:
//#line 249 "gramatic.y"
{
            addError("Falta delimitador '{' de apertura del programa.");
        }
break;
case 10:
//#line 259 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 11:
//#line 261 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 12:
//#line 263 "gramatic.y"
{ 
            yyerror("Error sintáctico en sentencia. Recuperando en ';'."); 
            errorEnProduccion = false; /* REINICIAR*/
        }
break;
case 16:
//#line 274 "gramatic.y"
{ 
            addError("Error en cuerpo de función. Recuperando en ';'."); 
        }
break;
case 19:
//#line 286 "gramatic.y"
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
//#line 308 "gramatic.y"
{ 
            addError("Falta lista de variables a continuación de var.");
        }
break;
case 21:
//#line 312 "gramatic.y"
{ 
            addError("Error grave en la 'lista_variables'. Recuperando en ';'."); 
            listaVariablesError = false; 
        }
break;
case 22:
//#line 320 "gramatic.y"
{    
        yyval.entry = val_peek(0).entry;
    }
break;
case 23:
//#line 323 "gramatic.y"
{
        /* Caso prefijado: MAIN.A*/
        SymbolEntry scopeID = (SymbolEntry)val_peek(2).entry;
        SymbolEntry varID = (SymbolEntry)val_peek(0).entry;
        
        /* Guardamos "MAIN" dentro de "A" para usarlo después en el lookup*/
        varID.setScopePrefix(scopeID.getLexeme());
        
        yyval.entry = varID;
    }
break;
case 24:
//#line 336 "gramatic.y"
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
case 25:
//#line 349 "gramatic.y"
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
case 26:
//#line 363 "gramatic.y"
{ 
            listaVariablesError = true; 
            errorEnProduccion = true; /* Activar*/
        }
break;
case 27:
//#line 369 "gramatic.y"
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
case 28:
//#line 411 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)val_peek(0).entry);
            yyval.obj = list;
        }
break;
case 29:
//#line 416 "gramatic.y"
{            
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add(val_peek(0).entry);
            yyval.obj = lista; }
break;
case 30:
//#line 428 "gramatic.y"
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
case 31:
//#line 461 "gramatic.y"
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
case 32:
//#line 479 "gramatic.y"
{ 
            addError("Falta nombre de función.");
        }
break;
case 33:
//#line 483 "gramatic.y"
{ 
            addError("Se tiene que tener mínimo un parámetro formal.");
        }
break;
case 34:
//#line 487 "gramatic.y"
{ 
            addError("Falta '{' de apertura de función.");
        }
break;
case 35:
//#line 491 "gramatic.y"
{ 
            addError("Falta '}' de cierre de función.");
        }
break;
case 36:
//#line 495 "gramatic.y"
{ 
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            listaTiposError = false; 
            addError("Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
break;
case 37:
//#line 504 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 38:
//#line 510 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 39:
//#line 517 "gramatic.y"
{
           listaTiposError = true;
           errorEnProduccion = true; 
        }
break;
case 40:
//#line 524 "gramatic.y"
{yyval.obj = new SymbolEntry("int");}
break;
case 41:
//#line 525 "gramatic.y"
{yyval.obj = new SymbolEntry("float");}
break;
case 42:
//#line 530 "gramatic.y"
{yyval.obj=val_peek(0).entry;}
break;
case 43:
//#line 532 "gramatic.y"
{
            addError("Falta ',' entre parametros.");
        }
break;
case 45:
//#line 540 "gramatic.y"
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
case 46:
//#line 565 "gramatic.y"
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
case 47:
//#line 588 "gramatic.y"
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
case 48:
//#line 604 "gramatic.y"
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
case 49:
//#line 621 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 50:
//#line 625 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 51:
//#line 630 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cv'.");
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 52:
//#line 635 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cr'.");
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 53:
//#line 642 "gramatic.y"
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
case 65:
//#line 707 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(1).Polacaelement;
            PI().generatePrint(expr);
            
            if (!errorEnProduccion) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Print detectado");
            }
       }
break;
case 66:
//#line 716 "gramatic.y"
{
            addError("Falta argumento en print.");
       }
break;
case 69:
//#line 725 "gramatic.y"
{ 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
break;
case 71:
//#line 738 "gramatic.y"
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
case 72:
//#line 750 "gramatic.y"
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
case 73:
//#line 763 "gramatic.y"
{

          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado.");
      }
break;
case 74:
//#line 774 "gramatic.y"
{ 
          addError("Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
break;
case 75:
//#line 781 "gramatic.y"
{
          
          PolacaElement cond = (PolacaElement)val_peek(1).Polacaelement;
          pilaSaltosBF.push(cond.getFalseList());
      }
break;
case 77:
//#line 791 "gramatic.y"
{ addError("Falta paréntesis de apertura '(' en IF."); }
break;
case 78:
//#line 793 "gramatic.y"
{ addError("Falta paréntesis de cierre ')' en condición."); }
break;
case 79:
//#line 795 "gramatic.y"
{ addError("Error en el cuerpo de la cláusula then."); }
break;
case 80:
//#line 800 "gramatic.y"
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
case 81:
//#line 815 "gramatic.y"
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
case 82:
//#line 872 "gramatic.y"
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
case 83:
//#line 899 "gramatic.y"
{ 
            addError("Falta parentesis de apertura en encabezado del for.");
        }
break;
case 84:
//#line 903 "gramatic.y"
{
            addError("Falta nombre de variable en for.");
        }
break;
case 85:
//#line 907 "gramatic.y"
{ 
            addError("Falta palabra clave 'from' en encabezado del for.");
        }
break;
case 86:
//#line 911 "gramatic.y"
{ 
            addError("Falta constante inicial en encabezado del for.");
        }
break;
case 87:
//#line 915 "gramatic.y"
{ 
            addError("Falta palabra clave 'to' en encabezado del for.");
        }
break;
case 88:
//#line 919 "gramatic.y"
{ 
            addError("Falta constante final en encabezado del for.");
        }
break;
case 89:
//#line 923 "gramatic.y"
{ 
            addError("Falta parentesis de cierre en encabezado for.");
        }
break;
case 90:
//#line 927 "gramatic.y"
{ 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
break;
case 91:
//#line 934 "gramatic.y"
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
case 92:
//#line 949 "gramatic.y"
{ 
            addError("Falta de comparador en comparación.");
        }
break;
case 93:
//#line 953 "gramatic.y"
{ 
            addError("Falta operando izquierdo en comparación.");
        }
break;
case 94:
//#line 957 "gramatic.y"
{ 
            addError("Falta operando derecho en comparación.");
        }
break;
case 95:
//#line 963 "gramatic.y"
{ yyval.sval = "=="; }
break;
case 96:
//#line 964 "gramatic.y"
{ yyval.sval = "=!"; }
break;
case 97:
//#line 965 "gramatic.y"
{ yyval.sval = "<";  }
break;
case 98:
//#line 966 "gramatic.y"
{ yyval.sval = "<="; }
break;
case 99:
//#line 967 "gramatic.y"
{ yyval.sval = ">";  }
break;
case 100:
//#line 968 "gramatic.y"
{ yyval.sval = ">="; }
break;
case 101:
//#line 974 "gramatic.y"
{ 
            SymbolEntry destino = (SymbolEntry)val_peek(2).entry;
            PolacaElement fuente = (PolacaElement)val_peek(0).Polacaelement;

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
case 102:
//#line 1004 "gramatic.y"
{    
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)val_peek(0).obj;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)val_peek(2).obj;

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
case 103:
//#line 1123 "gramatic.y"
{List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)val_peek(0).Polacaelement);
            yyval.obj = list;
        }
break;
case 104:
//#line 1127 "gramatic.y"
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
case 105:
//#line 1137 "gramatic.y"
{ 
            listaExpresionesError = true; 
            errorEnProduccion = true; /* Activar*/
        }
break;
case 106:
//#line 1145 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 107:
//#line 1147 "gramatic.y"
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
case 108:
//#line 1160 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 109:
//#line 1164 "gramatic.y"
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
case 110:
//#line 1177 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 111:
//#line 1184 "gramatic.y"
{
        yyval.Polacaelement = val_peek(0).Polacaelement;
    }
break;
case 112:
//#line 1187 "gramatic.y"
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
case 113:
//#line 1199 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 114:
//#line 1203 "gramatic.y"
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
case 115:
//#line 1215 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 116:
//#line 1222 "gramatic.y"
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
case 117:
//#line 1253 "gramatic.y"
{
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 118:
//#line 1257 "gramatic.y"
{ 
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 119:
//#line 1261 "gramatic.y"
{ 
          yyval.Polacaelement = PI().generateOperand(val_peek(0).entry); 
      }
break;
case 120:
//#line 1265 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 121:
//#line 1267 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 122:
//#line 1273 "gramatic.y"
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
case 123:
//#line 1369 "gramatic.y"
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
case 124:
//#line 1383 "gramatic.y"
{ 
            addError("Falta el '(' en la conversión explícita.");
        }
break;
case 125:
//#line 1387 "gramatic.y"
{ 
            addError("Falta el ')' en la conversión explícita.");
        }
break;
case 126:
//#line 1394 "gramatic.y"
{
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 127:
//#line 1401 "gramatic.y"
{
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)val_peek(2).listParamInv;
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 128:
//#line 1412 "gramatic.y"
{
        PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
        SymbolEntry idParam = (SymbolEntry)val_peek(0).entry;
        
        yyval.paramInv = new ParametroInvocacion(idParam.getLexeme(), expr);
        
        if (!errorEnProduccion) { 
            System.out.println("   -> Parametro nombrado detectado: " + idParam.getLexeme());
        }
    }
break;
case 129:
//#line 1423 "gramatic.y"
{ 
        addError("Sintaxis incorrecta en parámetro. Se espera 'valor -> nombre'.");
        yyval.paramInv = new ParametroInvocacion("error", (PolacaElement)val_peek(1).Polacaelement); /* Dummy para no romper todo*/
    }
break;
case 130:
//#line 1431 "gramatic.y"
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
case 131:
//#line 1454 "gramatic.y"
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
case 132:
//#line 1506 "gramatic.y"
{ 
        addError("Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 133:
//#line 1515 "gramatic.y"
{ 
        addError("Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 134:
//#line 1525 "gramatic.y"
{ 
        addError("Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 135:
//#line 1534 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 136:
//#line 1538 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 137:
//#line 1542 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
//#line 2339 "Parser.java"
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
