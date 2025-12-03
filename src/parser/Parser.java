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






//#line 6 "gramatic.y"
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
   24,   24,   24,   24,   37,   46,   37,   37,   19,   19,
   19,    3,    3,    3,    3,    3,    4,    4,    4,    4,
    4,    5,    5,    5,    5,    5,    5,    7,    6,    6,
    6,   26,   26,   27,   27,   23,   10,   10,   10,   10,
    9,    9,    9,
};
final static short yylen[] = {                            2,
    0,    1,    2,    1,    4,    4,    4,    3,    0,    2,
    2,    3,    0,    2,    2,    3,    1,    1,    3,    2,
    3,    1,    3,    1,    3,    1,    3,    3,    1,    1,
    3,    2,    9,   10,    9,    9,    9,    9,    1,    3,
    2,    1,    1,    1,    3,    3,    2,    3,    2,    1,
    2,    2,    2,    2,    5,    1,    1,    2,    2,    1,
    1,    1,    1,    2,    2,    1,    1,    1,    4,    4,
    0,    2,    3,    3,    2,    0,    5,    1,    0,    7,
    6,    6,    6,    5,    8,    5,   10,   10,   10,   10,
   10,   10,   10,    3,    3,    2,    3,    3,    1,    1,
    1,    1,    1,    1,    4,    0,    9,    4,    1,    3,
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
    0,    0,   20,    0,    0,    0,    0,    0,   71,    0,
    0,    0,    0,    0,    0,    0,    0,   32,   41,    0,
    0,   59,   58,    0,    0,  125,  123,  124,    0,    0,
    0,  117,  127,  126,    0,    0,    0,    0,    0,    0,
    0,   21,    0,   19,    0,   94,    0,    0,    0,    0,
    0,   25,    0,    0,    0,   50,    0,    0,   44,    0,
    0,    0,    0,   31,    0,   40,    0,    0,    0,   99,
  100,  101,  102,  103,  104,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,   70,   69,
    0,    0,    0,    0,    0,    0,    0,    0,    0,  136,
    0,    0,   72,  105,    0,    0,   53,   51,   54,   52,
   47,    0,    0,    0,   49,    0,  108,  106,    0,    0,
    0,    0,    0,    0,    0,    0,    0,  132,    0,    0,
  114,    0,  116,    0,   98,    0,  119,  118,  121,  120,
   71,    0,    0,    0,    0,   55,    0,    0,    0,    0,
    0,    0,   73,   86,    0,   13,   13,   45,   13,   13,
   46,   48,    0,   13,    0,    0,    0,    0,  135,    0,
  128,    0,  130,  131,  129,    0,   78,   76,    0,   81,
   82,   83,    0,    0,    0,    0,    0,    0,    0,   13,
    0,    0,    0,    0,    0,    0,    0,  141,  142,  143,
    0,    0,    0,  134,  133,   74,    0,   75,   80,    0,
    0,    0,    0,    0,    0,    0,    0,    0,   68,   14,
   15,    9,   63,    0,   66,   67,    9,    9,    9,    0,
    9,   84,  138,  139,  140,  137,    0,    0,    0,    0,
    0,    0,    0,    0,   85,    9,   16,    0,   65,   64,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,   35,   38,   36,   33,  107,    0,
   77,   87,   88,   89,   90,   91,   92,   93,   34,
};
final static short yydgoto[] = {                          3,
   24,   25,   80,   81,   82,   83,   84,   85,  251,   26,
   86,    5,   28,   29,  109,   55,   30,  111,   91,   31,
   32,  118,   33,  126,  112,  177,  178,    6,   10,   34,
   35,  241,  271,   36,   37,   38,   39,   40,   41,   42,
  101,  192,  230,  257,  195,  213,
};
final static short yysindex[] = {                        92,
 -265, -259,    0,    0, -247, -217,    0, -134,    0, -152,
    0, -223,    0,  -80, -208, -179, -129, -113, -108,  -81,
  135,    0,    0,    0,  -94,    0, -119,  -71,   78,    0,
  -39, -237,  -65,    0,    0,    0,    0,    0,    0, -162,
    0,    0, -208,    0, -208,    0,    0, -116, -116,  -50,
  301,  -95,    0, -119, -207, -231,  189,  -46,    0,  -42,
  301,  -26, -160,  301,  -42, -191,  135,    0,    0,  -16,
  -16,    0,    0,  411, -147,    0,    0,    0,  138,  459,
  185,    0,    0,    0,  -23, -119, -202,    3,  -58,  221,
  438,    0,  -42,    0,  -42,    0,   -3,    9, -178,   44,
  342,    0, -115, -191,   53,    0, -221, -164,    0,   26,
 -132, -229,  426,    0, -109,    0,   63,   67, -120,    0,
    0,    0,    0,    0,    0,  301,  301,  301,  301,  -36,
  107,  221,  120,  169,  208,   75,   75,  141,    0,    0,
  115,  301,  221, -119, -119,  301,  301,  301,  223,    0,
  155,  160,    0,    0,  -89,  131,    0,    0,    0,    0,
    0, -234,  114, -191,    0,  203,    0,    0, -191,  186,
 -116,  196,  248,  249,  221,  -29,  110,    0,   -2,  364,
    0,  185,    0,  185,    0,  221,    0,    0,    0,    0,
    0,  199,  199,  199,   75,    0,  221,  230,  273,  277,
  278,   18,    0,    0,  264,    0,    0,    0,    0,    0,
    0,    0,  -42,    0,  266,  281,  281,  281,    0,  300,
    0,  301,    0,    0,    0,  490,    0,    0,  275,    0,
    0,    0,  199,  301,  301,  301,  301,  301,  262,    0,
  -68,  -68,  -68,  -68,  288,  -68,  549,    0,    0,    0,
  284,  285,   20,    0,    0,    0,   75,    0,    0,  286,
  287,  294,  296,  297,  298,   70,  -68,  299,    0,    0,
    0,    0,    0, -104,    0,    0,    0,    0,    0,  301,
    0,    0,    0,    0,    0,    0,  322,   75,   75,   75,
   75,   75,   75,   75,    0,    0,    0,  -18,    0,    0,
   76,   90,  125, -172,  152,  309,  310,  313,  314,  315,
  321,  329,  331,  139,    0,    0,    0,    0,    0, -208,
    0,    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yyrindex[] = {                       564,
    0,   41,    0,    0,  187,  587,    0,    0,    0,    0,
    0,    0,    0,    0,    5,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,   94,   -4,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    6,    0,    0,    0,    0,    0,
    0,    0,    0, -161,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  217,    0,    0,    0,    0,    0,
  295,    0,    0,    0,    0,  256,    0,    0,    0,   -5,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,   88,    0,    0,    0,    0,    0,  304,    0,    0,
    0,    0,  381,  -61,    7,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  128,    0,    0,    0,    0,    0,
    0,  334,    0,  373,    0,  129,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  420,    0,    0,    0,
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
    0,    0,    0,    0,    0,    0,    0,    0,    0,  174,
    0,    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
   16,    0,  -40,  379, -117,    0,    0,  -37,  306,  458,
  -10,    0,  -43,    0,  339,    0,    4,  -51,  555,    0,
    0,  554,    0,  546,    0,    0,  414,    0,   -6,  517,
  -93,  534,    0,    0,    0,  523,  535,  545,  551,  557,
  451, -130,  -83,    0,    0,    0,
};
final static int YYTABLESIZE=831;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         27,
   12,   27,   14,   27,    8,    7,  193,  153,   54,   89,
   90,   87,   97,   99,  115,    4,  188,  190,    7,    2,
  103,  114,  106,   90,   58,    2,    8,  165,  198,  199,
  200,  202,   43,    2,  157,   69,    9,   16,   11,  132,
   17,   18,   19,   20,   66,   27,   27,   68,   93,  207,
  143,   67,  155,  137,   27,   96,  107,  108,   21,   22,
   23,   44,   22,   23,  233,  106,  110,   22,   23,  110,
  116,  158,  143,   22,   23,  102,   48,  148,   47,   94,
  138,   95,  144,  319,  145,  175,  176,  179,  180,  149,
   27,  159,  186,   72,   26,  105,  106,  130,  131,  107,
  108,  197,   49,   15,    2,   22,   23,  110,   16,  231,
  232,   17,   18,   19,   20,  166,  260,  261,  262,  263,
  264,  266,   13,  162,   73,   26,  287,   26,  160,   21,
  107,  108,  153,  215,  127,  173,   22,   23,    8,   74,
   75,   76,   77,   78,   22,   23,  169,   52,    2,  259,
  163,  299,   50,  282,  130,  131,  164,  307,  308,  309,
  310,  311,  312,  313,  174,  110,  169,  110,   51,  245,
   60,  154,  110,  170,   56,   45,    2,   79,   53,  164,
   16,  176,  300,   17,   18,   19,   20,  268,    2,   59,
   70,   92,   16,  205,   28,   17,   18,   19,   20,  164,
   57,   21,   27,   61,   46,   88,   75,   76,   77,   78,
  100,  130,  131,   21,    2,   27,   22,   23,   71,  181,
   75,   76,   77,   78,  140,   28,  219,   28,   22,   23,
   27,   27,   27,   27,   64,   27,   27,   43,    2,  304,
  130,  131,   16,   79,  117,   17,   18,   19,   20,   65,
  109,  109,  109,  109,  109,  104,   27,   79,  220,  136,
    8,    7,   27,   21,  146,  298,  315,  130,  131,   30,
  301,  302,  303,  238,  305,  285,  147,  109,   22,   23,
  223,  109,  161,  109,   30,  139,  239,   27,  109,  314,
   27,   27,   27,   27,   27,   27,   22,   22,   22,   22,
   22,   22,  286,   27,   22,   22,   22,   22,   22,   22,
   22,   22,   22,   22,   22,   22,   22,   22,   22,   22,
   22,   22,   22,   22,   22,  294,  150,   22,   22,   22,
   22,   43,    2,   62,   22,  156,   16,   22,   22,   17,
   18,   19,   20,   96,  171,   43,    2,    1,    2,   29,
   16,  172,  295,   17,   18,   19,   20,   21,  191,   63,
  316,   29,  183,   75,   76,   77,   78,   29,   29,  209,
   96,   21,   22,   23,  317,  185,   75,   76,   77,   78,
   43,    2,   29,   97,   95,   16,   22,   23,   17,   18,
   19,   20,  221,  128,   43,    2,  194,  210,  222,   16,
   79,  196,   17,   18,   19,   20,   21,  320,    2,  318,
   97,   95,   16,   79,  206,   17,   18,   19,   20,  129,
   21,   22,   23,  329,  187,   75,   76,   77,   78,   37,
   37,   22,   23,   21,   37,   22,   23,   37,   37,   37,
   37,  203,    9,    9,   98,    2,  204,    9,   22,   23,
    9,    9,    9,    9,  227,   37,  134,  135,   37,  212,
  228,  229,   79,  189,   75,   76,   77,   78,    9,  214,
   37,   37,   22,   22,   22,   22,   22,  216,  201,   75,
   76,   77,   78,    9,    9,   22,   22,   22,   22,   22,
  130,  131,   22,   22,   22,   22,   22,   22,  234,   22,
  208,   79,  211,   22,   22,   22,   22,  208,  182,  184,
   22,  122,  122,  122,  122,  122,   79,  265,   75,   76,
   77,   78,  252,  253,  122,  122,  122,  122,  122,  217,
  218,  122,  122,  122,  122,  122,  122,  248,  122,  249,
  250,  235,  122,  122,  122,  236,  237,  240,  247,  122,
  112,  112,  112,  112,  112,   79,  254,   75,   76,   77,
   78,  258,  280,    1,  112,  112,  283,  284,  288,  289,
  112,  112,  112,  112,  112,  112,  290,  112,  291,  292,
  293,  112,  112,  112,  306,  297,    2,   79,  112,  113,
  113,  113,  113,  113,   79,  321,  322,  151,    2,  323,
  324,  325,   16,  113,  113,   17,   18,  326,   20,  113,
  113,  113,  113,  113,  113,  327,  113,  328,  113,  224,
  113,  113,  113,   21,  119,  133,  152,  113,  115,  115,
  115,  115,  115,  130,  131,  255,  111,  111,  111,  111,
  111,  226,  115,  115,    0,    0,  225,    0,  115,  115,
  115,  115,  115,  115,    0,  115,    0,    0,    0,  115,
  115,  115,    0,  111,    0,    0,  115,  111,    0,  111,
    0,    0,    0,    0,  111,  110,  110,  110,  110,  110,
    0,  167,   75,   76,   77,   78,  120,  121,  122,  123,
  124,  125,    0,    0,   75,   76,   77,   78,  269,  269,
  269,  269,  110,  269,    0,    0,  110,    0,  110,    0,
    0,    0,  168,  110,  142,   75,   76,   77,   78,   79,
  141,    0,    0,    0,  269,    0,  142,    0,  130,  131,
    0,   79,    0,    0,  120,  121,  122,  123,  124,  125,
  242,    0,  243,  244,    0,  151,    2,  246,    0,    0,
   16,    0,   79,   17,   18,    0,   20,  270,  270,  270,
  270,    0,  270,  272,  277,  278,  279,    0,  281,    0,
    0,   21,    0,  267,  256,  273,  273,  273,  273,    0,
  273,    0,    0,  270,    0,  274,  274,  274,  274,  296,
  274,  275,  275,  275,  275,    0,  275,  276,  276,  276,
  276,  273,  276,    0,    0,    2,    0,    0,    0,   16,
    0,  274,   17,   18,    0,   20,    0,  275,    0,    0,
    0,    0,    0,  276,    0,    0,    0,    0,    0,    0,
   21,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         10,
    7,   12,    9,   14,    0,    0,  137,  101,   19,   50,
   51,   49,   56,   57,   66,    0,  134,  135,  284,  257,
   61,   65,  257,   64,   21,  257,  286,  257,  146,  147,
  148,  149,  256,  257,  256,   32,  284,  261,  256,   80,
  264,  265,  266,  267,  282,   56,   57,   32,  256,  284,
   91,  289,  104,  256,   65,  287,  291,  292,  282,  297,
  298,  285,  297,  298,  195,  257,   63,  297,  298,   66,
   67,  293,  113,  297,  298,   60,  256,  256,  287,  287,
  283,  289,   93,  256,   95,  126,  127,  128,  129,  268,
  101,  256,  133,  256,  256,  256,  257,  270,  271,  291,
  292,  142,  282,  256,  257,  297,  298,  104,  261,  193,
  194,  264,  265,  266,  267,  112,  234,  235,  236,  237,
  238,  239,  257,  256,  287,  287,  257,  289,  293,  282,
  291,  292,  226,  171,  282,  256,  297,  298,  286,  256,
  257,  258,  259,  260,  297,  298,  256,  256,  257,  233,
  283,  256,  282,  247,  270,  271,  289,  288,  289,  290,
  291,  292,  293,  294,  285,  162,  256,  164,  282,  213,
  290,  287,  169,  283,  256,  256,  257,  294,  287,  289,
  261,  222,  287,  264,  265,  266,  267,  256,  257,  284,
  256,  287,  261,  283,  256,  264,  265,  266,  267,  289,
  282,  282,  213,  275,  285,  256,  257,  258,  259,  260,
  257,  270,  271,  282,  257,  226,  297,  298,  284,  256,
  257,  258,  259,  260,  283,  287,  256,  289,  297,  298,
  241,  242,  243,  244,  274,  246,  247,  256,  257,  280,
  270,  271,  261,  294,  261,  264,  265,  266,  267,  289,
  256,  257,  258,  259,  260,  282,  267,  294,  288,  283,
  256,  256,  256,  282,  268,  272,  285,  270,  271,  274,
  277,  278,  279,  256,  281,  256,  268,  283,  297,  298,
  283,  287,  257,  289,  289,  283,  269,  298,  294,  296,
  301,  302,  303,  287,  305,  289,  256,  257,  258,  259,
  260,  261,  283,  314,  264,  265,  266,  267,  268,  269,
  270,  271,  272,  273,  274,  275,  276,  277,  278,  279,
  280,  281,  282,  283,  284,  256,  283,  287,  288,  289,
  290,  256,  257,  256,  294,  283,  261,  297,  298,  264,
  265,  266,  267,  256,  282,  256,  257,  256,  257,  256,
  261,  285,  283,  264,  265,  266,  267,  282,  284,  282,
  285,  268,  256,  257,  258,  259,  260,  274,  275,  256,
  283,  282,  297,  298,  285,  256,  257,  258,  259,  260,
  256,  257,  289,  256,  256,  261,  297,  298,  264,  265,
  266,  267,  283,  256,  256,  257,  256,  284,  289,  261,
  294,  287,  264,  265,  266,  267,  282,  256,  257,  285,
  283,  283,  261,  294,  284,  264,  265,  266,  267,  282,
  282,  297,  298,  285,  256,  257,  258,  259,  260,  256,
  257,  297,  298,  282,  261,  297,  298,  264,  265,  266,
  267,  287,  256,  257,  256,  257,  287,  261,  297,  298,
  264,  265,  266,  267,  256,  282,  272,  273,  285,  257,
  262,  263,  294,  256,  257,  258,  259,  260,  282,  284,
  297,  298,  256,  257,  258,  259,  260,  282,  256,  257,
  258,  259,  260,  297,  298,  269,  270,  271,  272,  273,
  270,  271,  276,  277,  278,  279,  280,  281,  269,  283,
  162,  294,  164,  287,  288,  289,  290,  169,  130,  131,
  294,  256,  257,  258,  259,  260,  294,  256,  257,  258,
  259,  260,  217,  218,  269,  270,  271,  272,  273,  282,
  282,  276,  277,  278,  279,  280,  281,  257,  283,  259,
  260,  269,  287,  288,  289,  269,  269,  284,  283,  294,
  256,  257,  258,  259,  260,  294,  257,  257,  258,  259,
  260,  287,  275,    0,  270,  271,  283,  283,  283,  283,
  276,  277,  278,  279,  280,  281,  283,  283,  283,  283,
  283,  287,  288,  289,  263,  287,    0,  284,  294,  256,
  257,  258,  259,  260,  294,  287,  287,  256,  257,  287,
  287,  287,  261,  270,  271,  264,  265,  287,  267,  276,
  277,  278,  279,  280,  281,  287,  283,  287,   64,  256,
  287,  288,  289,  282,   71,   80,  285,  294,  256,  257,
  258,  259,  260,  270,  271,  222,  256,  257,  258,  259,
  260,  191,  270,  271,   -1,   -1,  283,   -1,  276,  277,
  278,  279,  280,  281,   -1,  283,   -1,   -1,   -1,  287,
  288,  289,   -1,  283,   -1,   -1,  294,  287,   -1,  289,
   -1,   -1,   -1,   -1,  294,  256,  257,  258,  259,  260,
   -1,  256,  257,  258,  259,  260,  276,  277,  278,  279,
  280,  281,   -1,   -1,  257,  258,  259,  260,  241,  242,
  243,  244,  283,  246,   -1,   -1,  287,   -1,  289,   -1,
   -1,   -1,  287,  294,  289,  257,  258,  259,  260,  294,
  283,   -1,   -1,   -1,  267,   -1,  289,   -1,  270,  271,
   -1,  294,   -1,   -1,  276,  277,  278,  279,  280,  281,
  207,   -1,  209,  210,   -1,  256,  257,  214,   -1,   -1,
  261,   -1,  294,  264,  265,   -1,  267,  241,  242,  243,
  244,   -1,  246,  241,  242,  243,  244,   -1,  246,   -1,
   -1,  282,   -1,  240,  285,  241,  242,  243,  244,   -1,
  246,   -1,   -1,  267,   -1,  241,  242,  243,  244,  267,
  246,  241,  242,  243,  244,   -1,  246,  241,  242,  243,
  244,  267,  246,   -1,   -1,  257,   -1,   -1,   -1,  261,
   -1,  267,  264,  265,   -1,  267,   -1,  267,   -1,   -1,
   -1,   -1,   -1,  267,   -1,   -1,   -1,   -1,   -1,   -1,
  282,
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
"declaracion_variable : VAR error SEMICOLON",
"identificador : ID",
"identificador : ID UNDERSCORE ID",
"identificador_completo : identificador",
"identificador_completo : identificador_completo POINT identificador",
"lista_variables : identificador_completo",
"lista_variables : lista_variables COMMA identificador_completo",
"lista_variables : lista_variables error identificador_completo",
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
"$$3 :",
"asignacion : lista_variables_destino ASSIGN lista_expresiones SEMICOLON $$3 identificador_destino ASSIGN_COLON expresion error",
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

//#line 1594 "gramatic.y"

/* ======= Código Java adicional (opcional) ======= */
//#line 802 "Parser.java"
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
//#line 324 "gramatic.y"
{ 
          /* Reportamos el error personalizado */
          yyerror("Error: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter '_' no está permitido en los identificadores.", true);
          
          /* RECUPERACIÓN: Asumimos que el usuario quería usar el primer ID para seguir compilando */
          yyval.entry = val_peek(2).entry; 
      }
break;
case 24:
//#line 334 "gramatic.y"
{    
        yyval.entry = val_peek(0).entry;
    }
break;
case 25:
//#line 337 "gramatic.y"
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
//#line 350 "gramatic.y"
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
//#line 363 "gramatic.y"
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
//#line 377 "gramatic.y"
{ 
            listaVariablesError = true; 
            errorEnProduccion = true; /* Activar*/
        }
break;
case 29:
//#line 383 "gramatic.y"
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
//#line 425 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)val_peek(0).entry);
            yyval.obj = list;
        }
break;
case 31:
//#line 430 "gramatic.y"
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
//#line 442 "gramatic.y"
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
//#line 475 "gramatic.y"
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
//#line 493 "gramatic.y"
{ 
            addError("Falta nombre de función.");
        }
break;
case 35:
//#line 497 "gramatic.y"
{ 
            addError("Se tiene que tener mínimo un parámetro formal.");
        }
break;
case 36:
//#line 501 "gramatic.y"
{ 
            addError("Falta '{' de apertura de función.");
        }
break;
case 37:
//#line 505 "gramatic.y"
{ 
            addError("Falta '}' de cierre de función.");
        }
break;
case 38:
//#line 509 "gramatic.y"
{ 
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            listaTiposError = false; 
            addError("Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
break;
case 39:
//#line 518 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 40:
//#line 524 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 41:
//#line 531 "gramatic.y"
{
           listaTiposError = true;
           errorEnProduccion = true; 
        }
break;
case 42:
//#line 538 "gramatic.y"
{yyval.obj = new SymbolEntry("int");}
break;
case 43:
//#line 539 "gramatic.y"
{yyval.obj = new SymbolEntry("float");}
break;
case 44:
//#line 544 "gramatic.y"
{yyval.obj=val_peek(0).entry;}
break;
case 45:
//#line 546 "gramatic.y"
{
            addError("Falta ',' entre parametros.");
        }
break;
case 47:
//#line 554 "gramatic.y"
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
//#line 579 "gramatic.y"
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
//#line 602 "gramatic.y"
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
//#line 618 "gramatic.y"
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
//#line 635 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 52:
//#line 639 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 53:
//#line 644 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cv'.");
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 54:
//#line 649 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cr'.");
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 55:
//#line 656 "gramatic.y"
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
//#line 706 "gramatic.y"
{ 
        /* Detección específica para print*/
        addError("Error Sintáctico: Falta punto y coma ';' al final de la sentencia PRINT."); 
    }
break;
case 65:
//#line 717 "gramatic.y"
{ 
        addError("Error Sintáctico: Falta punto y coma ';' al final del PRINT (en función)."); 
    }
break;
case 69:
//#line 728 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(1).Polacaelement;
            PI().generatePrint(expr);
            
            if (!errorEnProduccion) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Print detectado");
            }
       }
break;
case 70:
//#line 737 "gramatic.y"
{
            addError("Falta argumento en print.");
       }
break;
case 73:
//#line 746 "gramatic.y"
{ 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
break;
case 75:
//#line 759 "gramatic.y"
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
//#line 771 "gramatic.y"
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
//#line 784 "gramatic.y"
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
//#line 795 "gramatic.y"
{ 
          addError("Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
break;
case 79:
//#line 802 "gramatic.y"
{
          
          PolacaElement cond = (PolacaElement)val_peek(1).Polacaelement;
          pilaSaltosBF.push(cond.getFalseList());
      }
break;
case 81:
//#line 812 "gramatic.y"
{ addError("Falta paréntesis de apertura '(' en IF."); }
break;
case 82:
//#line 814 "gramatic.y"
{ addError("Falta paréntesis de cierre ')' en condición."); }
break;
case 83:
//#line 816 "gramatic.y"
{ addError("Error en el cuerpo de la cláusula then."); }
break;
case 84:
//#line 821 "gramatic.y"
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
//#line 836 "gramatic.y"
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
//#line 893 "gramatic.y"
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
//#line 920 "gramatic.y"
{ 
            addError("Falta parentesis de apertura en encabezado del for.");
        }
break;
case 88:
//#line 924 "gramatic.y"
{
            addError("Falta nombre de variable en for.");
        }
break;
case 89:
//#line 928 "gramatic.y"
{ 
            addError("Falta palabra clave 'from' en encabezado del for.");
        }
break;
case 90:
//#line 932 "gramatic.y"
{ 
            addError("Falta constante inicial en encabezado del for.");
        }
break;
case 91:
//#line 936 "gramatic.y"
{ 
            addError("Falta palabra clave 'to' en encabezado del for.");
        }
break;
case 92:
//#line 940 "gramatic.y"
{ 
            addError("Falta constante final en encabezado del for.");
        }
break;
case 93:
//#line 944 "gramatic.y"
{ 
            addError("Falta parentesis de cierre en encabezado for.");
        }
break;
case 94:
//#line 948 "gramatic.y"
{ 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
break;
case 95:
//#line 955 "gramatic.y"
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
//#line 970 "gramatic.y"
{ 
            addError("Falta de comparador en comparación.");
        }
break;
case 97:
//#line 974 "gramatic.y"
{ 
            addError("Falta operando izquierdo en comparación.");
        }
break;
case 98:
//#line 978 "gramatic.y"
{ 
            addError("Falta operando derecho en comparación.");
        }
break;
case 99:
//#line 984 "gramatic.y"
{ yyval.sval = "=="; }
break;
case 100:
//#line 985 "gramatic.y"
{ yyval.sval = "=!"; }
break;
case 101:
//#line 986 "gramatic.y"
{ yyval.sval = "<";  }
break;
case 102:
//#line 987 "gramatic.y"
{ yyval.sval = "<="; }
break;
case 103:
//#line 988 "gramatic.y"
{ yyval.sval = ">";  }
break;
case 104:
//#line 989 "gramatic.y"
{ yyval.sval = ">="; }
break;
case 105:
//#line 994 "gramatic.y"
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
//#line 1023 "gramatic.y"
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
//#line 1140 "gramatic.y"
{ 
            addError("Error Sintáctico: Falta punto y coma ';' al final de la asignación.");
            /* Opcional: Ejecutar la lógica de asignación si quieres recuperar la semántica*/
        }
break;
case 108:
//#line 1145 "gramatic.y"
{ 
                addError("Error Sintáctico: Falta punto y coma ';' al final de la asignación múltiple.");
            }
break;
case 109:
//#line 1152 "gramatic.y"
{List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)val_peek(0).Polacaelement);
            yyval.obj = list;
        }
break;
case 110:
//#line 1156 "gramatic.y"
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
//#line 1166 "gramatic.y"
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
//#line 1183 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 113:
//#line 1185 "gramatic.y"
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
//#line 1198 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 115:
//#line 1202 "gramatic.y"
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
//#line 1215 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 117:
//#line 1222 "gramatic.y"
{
        yyval.Polacaelement = val_peek(0).Polacaelement;
    }
break;
case 118:
//#line 1225 "gramatic.y"
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
//#line 1237 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 120:
//#line 1241 "gramatic.y"
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
//#line 1253 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 122:
//#line 1260 "gramatic.y"
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
//#line 1291 "gramatic.y"
{
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 124:
//#line 1298 "gramatic.y"
{ 
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 125:
//#line 1305 "gramatic.y"
{ 
          /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
          SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
          symbolTable.add(se_const);
          yyval.Polacaelement = PI().generateOperand(val_peek(0).entry); 
      }
break;
case 126:
//#line 1312 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 127:
//#line 1314 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 128:
//#line 1320 "gramatic.y"
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
//#line 1416 "gramatic.y"
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
//#line 1430 "gramatic.y"
{ 
            addError("Falta el '(' en la conversión explícita.");
        }
break;
case 131:
//#line 1434 "gramatic.y"
{ 
            addError("Falta el ')' en la conversión explícita.");
        }
break;
case 132:
//#line 1441 "gramatic.y"
{
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 133:
//#line 1448 "gramatic.y"
{
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)val_peek(2).listParamInv;
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 134:
//#line 1459 "gramatic.y"
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
//#line 1470 "gramatic.y"
{ 
        addError("Sintaxis incorrecta en parámetro. Se espera 'valor -> nombre'.");
        yyval.paramInv = new ParametroInvocacion("error", (PolacaElement)val_peek(1).Polacaelement); /* Dummy para no romper todo*/
    }
break;
case 136:
//#line 1478 "gramatic.y"
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
//#line 1501 "gramatic.y"
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
//#line 1553 "gramatic.y"
{ 
        addError("Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 139:
//#line 1562 "gramatic.y"
{ 
        addError("Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 140:
//#line 1572 "gramatic.y"
{ 
        addError("Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 141:
//#line 1581 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 142:
//#line 1585 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 143:
//#line 1589 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
//#line 2431 "Parser.java"
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



//## Constructors ###############################################
/**
 * Default constructor.  Turn off with -Jnoconstruct .

 */


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
