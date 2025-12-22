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
    private boolean  errorfuncion = false;
    
    /* --- LISTA DONDE SE GUARDAN LOS MENSAJES ---*/
    private List<String> listaErrores = new ArrayList<>();
    private List<String> listaWarnings = new ArrayList<>();

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
    public List<String> getWarnings() { return listaWarnings; }

    /* =========================================================================*/
    /*  MÉTODOS DE ERROR (Sustituyen a System.err.println)*/
    /* =========================================================================*/

    private void removeLastGenericError() {
        if (listaErrores.isEmpty()) return;
    
    /* El mensaje genérico de BYACC/J siempre contiene el prefijo "Error Sintactico" y "syntax error"*/
        String lastError = listaErrores.get(listaErrores.size() - 1);
    
        if  (lastError.contains("syntax error")) {
            listaErrores.remove(listaErrores.size() - 1);
        /* NO se toca errorEnProduccion aquí, ya que el error existe.*/
        }
}

    /* Método para los errores manuales en la gramática (Sintácticos)*/
    private void addError(String mensaje) {
        removeLastGenericError();
        int linea=lexer.getContext().getLine();
        String error = "Línea " + linea + " " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }


    public void addWarning(String s) {
    String mensaje = "Línea " + lexer.getContext().getLine() + " - WARNING: " + s;
    listaWarnings.add(mensaje);
    }


    public void addErrorSemicolon(String msg) {
    removeLastGenericError();
    addWarning(msg); /* Redirige a warning, no bloquea compilación*/
    }


    private void addErrorLex(String mensaje, int linea) {
        removeLastGenericError();
        String error = "Línea " + linea + ": " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }


    /*  Método para errores SEMÁNTICOS (Tipos, declaraciones)*/
    /* Se usa: yyerror("Mensaje", true);*/
    public void yyerror(String s, boolean semantico) {
        if(!errorEnProduccion){
            String error = "Línea " + lexer.getContext().getLine()+" "+ s;
            listaErrores.add(error);
        }
        errorEnProduccion = true;
    }

    /* 3. Método automático de BYACC (Sintácticos por defecto)*/
    public void yyerror(String s) {
        if(!errorEnProduccion){
            String error = "Línea " + lexer.getContext().getLine()+" " + s;
            listaErrores.add(error);
        }
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
                addErrorLex(tok.getLexeme(), tok.getLine()); 
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
public final static short PRIORIDAD_BAJA_ASIGNACION=257;
public final static short ID=258;
public final static short INT16=259;
public final static short FLOAT32=260;
public final static short STRING=261;
public final static short PRIORIDAD_ID=262;
public final static short LPAREN=263;
public final static short IF=264;
public final static short ELSE=265;
public final static short ENDIF=266;
public final static short PRINT=267;
public final static short RETURN=268;
public final static short VAR=269;
public final static short FOR=270;
public final static short FROM=271;
public final static short TO=272;
public final static short PLUS=273;
public final static short MINUS=274;
public final static short STAR=275;
public final static short SLASH=276;
public final static short ASSIGN=277;
public final static short ASSIGN_COLON=278;
public final static short EQ=279;
public final static short NEQ=280;
public final static short LT=281;
public final static short LE_OP=282;
public final static short GT=283;
public final static short GE=284;
public final static short RPAREN=285;
public final static short LBRACE=286;
public final static short RBRACE=287;
public final static short UNDERSCORE=288;
public final static short SEMICOLON=289;
public final static short ARROW=290;
public final static short COMMA=291;
public final static short POINT=292;
public final static short CV=293;
public final static short CR=294;
public final static short LE_KW=295;
public final static short TOF=296;
public final static short ERROR=297;
public final static short EOF=298;
public final static short INT_KW=299;
public final static short FLOAT_KW=300;
public final static short STRING_KW=301;
public final static short YYERRCODE=256;
final static short yylhs[] = {                           -1,
    0,    0,    0,   12,   28,   28,   29,   29,   29,   29,
   32,   32,   32,   32,   30,   30,   35,   35,   35,    1,
    1,    1,   11,   11,   16,   16,   16,   13,   20,   20,
   20,   14,   14,   34,   34,   34,   34,   34,   21,   21,
   21,   17,   17,   18,   18,   18,   15,   15,   15,   15,
   15,   15,   25,   25,   25,   25,   36,   36,   36,   36,
   31,   31,   31,   31,   31,   31,   33,   33,   33,   33,
   33,   38,   38,   38,   41,   41,   41,   42,   43,   43,
   44,   44,   45,   44,   44,   46,   39,   39,   39,   39,
   22,    2,   40,   40,   40,   40,   40,   40,   40,   40,
   40,   40,    8,    8,    8,    8,   24,   24,   24,   24,
   24,   24,   37,   37,   37,   37,   19,   19,   19,    3,
    3,    3,    3,    3,    4,    4,    4,    4,    4,    5,
    5,    5,    5,    5,    5,    7,    6,    6,    6,   26,
   26,   27,   27,   23,   10,   10,   10,   10,    9,    9,
    9,
};
final static short yylen[] = {                            2,
    0,    1,    2,    1,    4,    4,    0,    2,    2,    2,
    0,    2,    2,    2,    1,    1,    3,    2,    2,    1,
    3,    3,    1,    3,    1,    3,    2,    1,    1,    3,
    2,    2,    2,    9,    9,    9,    9,    7,    1,    3,
    2,    1,    1,    1,    3,    3,    2,    3,    2,    1,
    1,    2,    2,    2,    2,    2,    5,    4,    5,    4,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    5,    5,    5,    1,    2,    3,    3,    2,    1,
    2,    1,    0,    4,    1,    0,    7,    6,    5,    6,
    5,    8,    5,   10,   10,    9,   10,   10,   10,   10,
    3,    4,    3,    2,    3,    3,    1,    1,    1,    1,
    1,    1,    4,    4,    3,    3,    1,    3,    2,    1,
    3,    3,    3,    3,    1,    3,    3,    3,    3,    1,
    1,    1,    1,    1,    1,    4,    4,    4,    4,    1,
    3,    3,    2,    4,    7,    7,    7,    7,    1,    1,
    1,
};
final static short yydefred[] = {                         0,
    0,    0,    0,    4,    0,    0,    7,    0,    0,    7,
    3,    0,   21,   22,    0,   10,    0,    0,    0,    0,
    0,    0,    6,   42,   43,   23,    0,   66,    0,    0,
    0,   39,    0,    0,    0,    8,    9,   15,   16,   62,
   61,   63,   64,   65,    5,    0,    0,    0,    0,    0,
  131,  132,  133,    0,    0,    0,    0,  125,  135,  134,
    0,    0,   18,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,   31,   33,    0,   32,   41,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,   17,    0,
    0,  101,    0,    0,    0,   75,    0,   24,    0,    0,
   50,    0,    0,   44,    0,    0,    0,    0,   30,   40,
    0,    0,    0,  144,  107,  108,  109,  110,  111,  112,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  140,    0,    0,    0,    0,  122,    0,  124,    0,
  127,  126,  129,  128,   58,    0,    0,    0,    0,    0,
    0,    0,    0,   76,  113,    0,   55,   53,   56,   54,
   47,    0,    0,    0,   49,    0,  114,    0,    0,    0,
    0,    0,  106,    0,    0,    0,    0,    0,   85,   83,
    0,   82,   89,   73,   74,   72,  143,    0,  136,    0,
   59,   57,  138,  139,  137,    0,    0,    0,    0,    0,
   77,   93,   11,   11,   45,   11,   11,   46,   48,    0,
    0,    0,    0,   88,   90,    0,   78,    0,   81,  142,
  141,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,  149,  150,  151,    0,    0,    0,   87,    0,
    0,    0,    0,    0,    0,    0,    0,   14,   71,   12,
   13,    7,   67,   68,   69,   70,    7,    7,   38,    7,
   91,  146,  147,  148,  145,    0,   80,   84,    0,    0,
    0,    0,    0,    0,   92,    0,    0,    0,    0,    0,
   79,    0,    0,    0,    0,    0,    0,   96,   35,   37,
   36,   34,   94,   95,   97,   98,   99,  100,
};
final static short yydgoto[] = {                          3,
   26,   27,   56,   57,   58,   59,   60,   84,  246,   28,
   61,    5,   30,   31,  114,   65,   32,  116,   62,   33,
   34,  122,   35,  131,  117,  141,  142,    6,   12,   36,
   37,  238,  261,   38,   39,   40,   41,   42,   43,   44,
  107,  137,  278,  193,  228,  187,
};
final static short yysindex[] = {                       -25,
 -273, -153,    0,    0, -262, -218,    0, -166, -148,    0,
    0,  399,    0,    0,  414,    0,  -37, -237, -174,  433,
 -192,    4,    0,    0,    0,    0,  -99,    0,  -74,  -31,
   -7,    0, -131, -209, -238,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,   27, -224, -224,  -24, -180,
    0,    0,    0,  472,   55,  -96,   -2,    0,    0,    0,
  -74,  627,    0,  -74, -132, -184,  -17,  166,   31,  472,
 -248,  472,   31,    0,    0,  -37,    0,    0,   30,   30,
   32,  490,  621,   40,   35,   45,  -19,  472,  268,  472,
  472,  -16,  -10,  302,  331,   50,  472,  -96,    0,   31,
  -74,    0,  104,  108,  261,    0,  664,    0,   89,   61,
    0, -236, -194,    0,  112, -252, -215,  335,    0,    0,
  128,  114, -175,    0,    0,    0,    0,    0,    0,    0,
  472,  -96,  384,  129,  164,  166,  100,  134, -225, -125,
 -144,    0,  146,  157,  195,   60,    0,   -2,    0,   -2,
    0,    0,    0,    0,    0,  -96,  -74,  472,  472,  415,
  188,  176,  189,    0,    0,  193,    0,    0,    0,    0,
    0, -200, -122, -171,    0,  223,    0, -224,  228,  237,
  238,  -96,    0,  -96,  100,  100,  129,  679,    0,    0,
  221,    0,    0,    0,    0,    0,    0,  247,    0,  472,
    0,    0,    0,    0,    0,  241,  242,  251,  -90,  472,
    0,    0,    0,    0,    0,    0,    0,    0,    0,  240,
  -85,  -85,  -85,    0,    0,  100,    0,  129,    0,    0,
    0,  472,  472,  472,  472,  429,  245,  599,  599,  599,
  439,  166,    0,    0,    0,  246,  249, -245,    0,  -76,
  269,  270,  271,  280,  281,  -94,  129,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,  279,    0,    0,  129,  129,
  129,  129,  129,  129,    0,  299,  454,  479,  494,  519,
    0,  308,  310,  311,  313,  315,  320,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yyrindex[] = {                       536,
    0,  -75,    0,    0,    0,  570,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  648,  -39,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,   23,
    0,    0,    0,    0,    0,  248,  113,    0,    0,    0,
   68,    0,    0, -149,    1,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,  282,    0,    0,
  350,    0,    0,    0,    0,    0,    0,    0,  534,    0,
    0,    0,    0,    0, -189,    0,    0,  584,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,   75,    0,    0,  324,    0,    0,    0,    0,    0,
    0,    0,    0,  559,    0,    0,    0,  158,    0,  203,
    0,    0,    0,    0,    0,  316,  365,    0,    0,    0,
    0,    0,  574,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0, -152,    0,    0,    0,    0,
    0,   99,    0,  125,    0,    0,    0,    0,    0,    0,
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
    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
    7,    0,  -20,  363,  -80,    0,    0,  -46,  252,  167,
  -12,    0,   70,    0,  216,    0,  -11,    0,  -42,    0,
    0,  531,    0,  539,    0,    0,  425,    0,   -9,  212,
  -63,    6,    0,    0,    0,  257,  420,  569,  573,  637,
  500, -112,    0, -169,    0,    0,
};
final static int YYTABLESIZE=966;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         29,
   15,   85,   29,  172,  106,   46,    4,  110,   64,  111,
  274,   89,    7,  152,  154,  224,  225,   79,   47,  167,
   29,  185,   78,   10,  161,   48,   83,   83,   87,  118,
  195,   82,  173,   50,   51,   52,   53,   11,  174,  275,
   77,   98,  175,  164,  112,  113,   75,   80,    2,  109,
   24,   25,  101,   29,   29,   29,  249,  111,  168,  115,
   29,  169,  132,  196,  120,    2,   51,  140,   98,  145,
  146,   55,  106,    2,  226,  108,  156,  206,  207,  209,
  180,   76,   88,   24,   25,  214,  111,  157,   49,   24,
   25,   13,  112,  113,   29,   51,   63,   98,   24,   25,
  170,   51,   74,   52,  102,  176,   25,    8,   25,   14,
  182,  181,  184,   25,   25,  250,    9,   25,   25,   25,
   25,  112,  113,   29,  164,    2,    2,   24,   25,  237,
  197,  220,   52,  216,    8,  103,  105,   25,   52,   25,
  199,   25,  119,    9,  286,   72,  200,   92,   93,   25,
   25,  251,  252,  253,  254,  256,   99,   83,  100,   73,
  115,  284,  115,  217,  198,  235,  292,  293,  294,  295,
  296,  297,  243,  244,  245,   29,   92,   93,  271,  140,
   20,  236,   20,   20,   20,   20,   68,   20,   20,  276,
  285,   20,   20,   20,   20,   20,   20,   20,   20,   20,
   20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
   20,   20,  277,   20,   20,   20,   20,   69,   29,  239,
   20,  240,  241,   20,   20,   29,   29,   29,   29,   29,
    1,   86,    2,   50,   51,   52,   53,   29,  104,  147,
    2,   50,   51,   52,   53,  149,   70,   50,   51,   52,
   53,   29,  287,   92,   93,   71,   19,  288,  289,   66,
  290,   24,   25,   19,   19,  139,   67,   19,   19,   19,
   19,   55,   94,   95,   29,   29,   29,   29,   20,   55,
   20,   20,   20,   20,   81,   55,   20,   19,    2,   20,
   20,   20,   20,  121,   20,   20,   20,   20,   20,   19,
   19,   20,   20,   20,   20,   20,   20,   20,   20,   20,
   90,   20,   20,   20,   20,  204,  124,   91,   20,  135,
  136,   20,   20,  130,  134,  130,  130,  130,  130,  138,
  130,  130,   92,   93,  130,  130,  130,  130,  155,  130,
  130,  130,  130,  130,  205,  166,  130,  130,  130,  130,
  130,  130,  130,  130,  130,  189,  130,  130,  130,  104,
  104,   92,   93,  130,  190,  191,  130,  130,  120,  171,
  120,  120,  120,  120,  158,  120,  120,  165,  159,  120,
  120,  120,  120,  105,  105,  120,  120,  215,  192,  218,
  178,  120,  120,  120,  120,  120,  120,  120,  120,  120,
  179,  120,  120,  120,  259,  259,  259,  259,  120,  103,
  103,  120,  120,  121,  136,  121,  121,  121,  121,  186,
  121,  121,  194,    2,  121,  121,  121,  121,   17,   18,
  121,  121,   19,   20,  201,   22,  121,  121,  121,  121,
  121,  121,  121,  121,  121,  202,  121,  121,  121,  260,
  260,  260,  260,  121,  148,  150,  121,  121,  123,  210,
  123,  123,  123,  123,  211,  123,  123,   92,   93,  123,
  123,  123,  123,  247,  248,  123,  123,  212,  213,  203,
  219,  123,  123,  123,  123,  123,  123,  123,  123,  123,
  221,  123,  123,  123,  262,  267,  268,  270,  123,  222,
  223,  123,  123,  117,  230,  117,  117,  117,  117,  229,
  117,  117,  232,  233,  117,  117,  117,  117,   50,   51,
   52,   53,  234,  143,  242,   50,   51,   52,   53,  257,
  272,  160,  117,  273,  117,    1,  117,  119,  117,  119,
  119,  119,  119,  117,  119,  119,  117,  117,  119,  119,
  119,  119,  144,  279,  280,  281,   55,  151,   97,   50,
   51,   52,   53,   55,  282,  283,  119,  291,  119,    2,
  119,  118,  119,  118,  118,  118,  118,  119,  118,  118,
  119,  119,  118,  118,  118,  118,  153,  298,   50,   51,
   52,   53,   50,   51,   52,   53,  303,   55,  304,  305,
  118,  306,  118,  307,  118,   27,  118,   27,  308,   86,
  123,  118,   27,   27,  118,  118,   27,   27,   27,   27,
   26,  133,   26,  177,  231,   97,   55,   26,   26,    0,
   55,   26,   26,   26,   26,  188,   27,    0,   27,  183,
   27,   50,   51,   52,   53,    0,    0,    0,   27,   27,
    0,   26,    0,   26,   16,   26,    2,  263,  263,  263,
  263,   17,   18,   26,   26,   19,   20,   21,   22,   16,
  208,    2,   50,   51,   52,   53,   17,   18,    0,   55,
   19,   20,   21,   22,  255,   23,   50,   51,   52,   53,
   50,   51,   52,   53,  258,   54,    2,   24,   25,    0,
   45,   17,   18,    0,    0,   19,   20,   21,   22,   16,
   55,    2,   24,   25,    0,    0,   17,   18,    0,    0,
   19,   20,   21,   22,   55,  269,    0,    0,   55,   50,
   51,   52,   53,    0,   16,    0,    2,   24,   25,    0,
  299,   17,   18,    0,    0,   19,   20,   21,   22,   16,
    0,    2,   24,   25,    0,    0,   17,   18,    0,    0,
   19,   20,   21,   22,    0,  300,    0,   55,  125,  126,
  127,  128,  129,  130,   16,    0,    2,   24,   25,    0,
  301,   17,   18,    0,    0,   19,   20,   21,   22,  115,
    0,  115,   24,   25,    0,    0,  115,  115,    0,    0,
  115,  115,  115,  115,    0,  302,  264,  264,  264,  264,
  265,  265,  265,  265,   60,    0,   60,   24,   25,    0,
  115,   60,   60,    0,    0,   60,   60,   60,   60,  102,
    0,  102,  115,  115,    0,    0,  102,  102,    0,  116,
  102,  102,  102,  102,    0,   60,  116,  116,    0,    0,
  116,  116,  116,  116,  258,    0,    2,   60,   60,    0,
  102,   17,   18,    0,    0,   19,   20,   21,   22,    0,
  116,    0,  102,  102,  266,  266,  266,  266,   50,   51,
   52,   53,  116,  116,   50,   51,   52,   53,    0,    0,
    0,    0,    0,   92,   93,    0,    0,   24,   25,  125,
  126,  127,  128,  129,  130,   28,   28,   28,   28,    0,
    0,   96,    0,    0,    0,    0,   55,   97,   28,  162,
    0,    2,   55,    0,   28,   28,   17,   18,    0,    0,
   19,   20,    0,   22,  162,    0,    2,    0,   28,    0,
    0,   17,   18,   28,    0,   19,   20,    0,   22,    0,
  163,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,  227,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         12,
   10,   48,   15,  256,   68,   17,    0,  256,   21,  258,
  256,   54,  286,   94,   95,  185,  186,  256,  256,  256,
   33,  134,   34,  286,  105,  263,   47,   48,   49,   72,
  256,  256,  285,  258,  259,  260,  261,  256,  291,  285,
   34,   62,  258,  107,  293,  294,  256,  286,  258,   70,
  299,  300,   65,   66,   67,   68,  226,  258,  295,   71,
   73,  256,   83,  289,   76,  258,  256,   88,   89,   90,
   91,  296,  136,  258,  187,   69,   97,  158,  159,  160,
  256,  291,  263,  299,  300,  286,  258,  100,  263,  299,
  300,  258,  293,  294,  107,  285,  289,  118,  299,  300,
  295,  291,   33,  256,  289,  117,  256,  288,  258,  258,
  131,  287,  133,  263,  264,  228,  297,  267,  268,  269,
  270,  293,  294,  136,  188,  258,  258,  299,  300,  210,
  256,  178,  285,  256,  288,   66,   67,  287,  291,  289,
  285,  291,   73,  297,  257,  277,  291,  273,  274,  299,
  300,  232,  233,  234,  235,  236,  289,  178,  291,  291,
  172,  256,  174,  286,  290,  256,  279,  280,  281,  282,
  283,  284,  258,  259,  260,  188,  273,  274,  242,  200,
  256,  272,  258,  259,  260,  261,  286,  263,  264,  266,
  285,  267,  268,  269,  270,  271,  272,  273,  274,  275,
  276,  277,  278,  279,  280,  281,  282,  283,  284,  285,
  286,  287,  289,  289,  290,  291,  292,  292,  258,  214,
  296,  216,  217,  299,  300,  238,  239,  240,  241,  242,
  256,  256,  258,  258,  259,  260,  261,  277,  256,  256,
  258,  258,  259,  260,  261,  256,  278,  258,  259,  260,
  261,  291,  262,  273,  274,  263,  256,  267,  268,  256,
  270,  299,  300,  263,  264,  285,  263,  267,  268,  269,
  270,  296,  275,  276,  287,  288,  289,  290,  256,  296,
  258,  259,  260,  261,  258,  296,  264,  287,  258,  267,
  268,  269,  270,  264,  272,  273,  274,  275,  276,  299,
  300,  279,  280,  281,  282,  283,  284,  285,  286,  287,
  256,  289,  290,  291,  292,  256,  285,  263,  296,  285,
  286,  299,  300,  256,  285,  258,  259,  260,  261,  285,
  263,  264,  273,  274,  267,  268,  269,  270,  289,  272,
  273,  274,  275,  276,  285,  285,  279,  280,  281,  282,
  283,  284,  285,  286,  287,  256,  289,  290,  291,  285,
  286,  273,  274,  296,  265,  266,  299,  300,  256,  258,
  258,  259,  260,  261,  271,  263,  264,  289,  271,  267,
  268,  269,  270,  285,  286,  273,  274,  172,  289,  174,
  263,  279,  280,  281,  282,  283,  284,  285,  286,  287,
  287,  289,  290,  291,  238,  239,  240,  241,  296,  285,
  286,  299,  300,  256,  286,  258,  259,  260,  261,  256,
  263,  264,  289,  258,  267,  268,  269,  270,  263,  264,
  273,  274,  267,  268,  289,  270,  279,  280,  281,  282,
  283,  284,  285,  286,  287,  289,  289,  290,  291,  238,
  239,  240,  241,  296,   92,   93,  299,  300,  256,  272,
  258,  259,  260,  261,  289,  263,  264,  273,  274,  267,
  268,  269,  270,  222,  223,  273,  274,  289,  286,  285,
  258,  279,  280,  281,  282,  283,  284,  285,  286,  287,
  263,  289,  290,  291,  238,  239,  240,  241,  296,  263,
  263,  299,  300,  256,  258,  258,  259,  260,  261,  289,
  263,  264,  272,  272,  267,  268,  269,  270,  258,  259,
  260,  261,  272,  256,  285,  258,  259,  260,  261,  285,
  285,  271,  285,  285,  287,    0,  289,  256,  291,  258,
  259,  260,  261,  296,  263,  264,  299,  300,  267,  268,
  269,  270,  285,  285,  285,  285,  296,  256,  291,  258,
  259,  260,  261,  296,  285,  285,  285,  289,  287,    0,
  289,  256,  291,  258,  259,  260,  261,  296,  263,  264,
  299,  300,  267,  268,  269,  270,  256,  289,  258,  259,
  260,  261,  258,  259,  260,  261,  289,  296,  289,  289,
  285,  289,  287,  289,  289,  256,  291,  258,  289,  286,
   80,  296,  263,  264,  299,  300,  267,  268,  269,  270,
  256,   83,  258,  289,  200,  291,  296,  263,  264,   -1,
  296,  267,  268,  269,  270,  136,  287,   -1,  289,  256,
  291,  258,  259,  260,  261,   -1,   -1,   -1,  299,  300,
   -1,  287,   -1,  289,  256,  291,  258,  238,  239,  240,
  241,  263,  264,  299,  300,  267,  268,  269,  270,  256,
  256,  258,  258,  259,  260,  261,  263,  264,   -1,  296,
  267,  268,  269,  270,  256,  287,  258,  259,  260,  261,
  258,  259,  260,  261,  256,  263,  258,  299,  300,   -1,
  287,  263,  264,   -1,   -1,  267,  268,  269,  270,  256,
  296,  258,  299,  300,   -1,   -1,  263,  264,   -1,   -1,
  267,  268,  269,  270,  296,  287,   -1,   -1,  296,  258,
  259,  260,  261,   -1,  256,   -1,  258,  299,  300,   -1,
  287,  263,  264,   -1,   -1,  267,  268,  269,  270,  256,
   -1,  258,  299,  300,   -1,   -1,  263,  264,   -1,   -1,
  267,  268,  269,  270,   -1,  287,   -1,  296,  279,  280,
  281,  282,  283,  284,  256,   -1,  258,  299,  300,   -1,
  287,  263,  264,   -1,   -1,  267,  268,  269,  270,  256,
   -1,  258,  299,  300,   -1,   -1,  263,  264,   -1,   -1,
  267,  268,  269,  270,   -1,  287,  238,  239,  240,  241,
  238,  239,  240,  241,  256,   -1,  258,  299,  300,   -1,
  287,  263,  264,   -1,   -1,  267,  268,  269,  270,  256,
   -1,  258,  299,  300,   -1,   -1,  263,  264,   -1,  256,
  267,  268,  269,  270,   -1,  287,  263,  264,   -1,   -1,
  267,  268,  269,  270,  256,   -1,  258,  299,  300,   -1,
  287,  263,  264,   -1,   -1,  267,  268,  269,  270,   -1,
  287,   -1,  299,  300,  238,  239,  240,  241,  258,  259,
  260,  261,  299,  300,  258,  259,  260,  261,   -1,   -1,
   -1,   -1,   -1,  273,  274,   -1,   -1,  299,  300,  279,
  280,  281,  282,  283,  284,  258,  259,  260,  261,   -1,
   -1,  285,   -1,   -1,   -1,   -1,  296,  291,  271,  256,
   -1,  258,  296,   -1,  277,  278,  263,  264,   -1,   -1,
  267,  268,   -1,  270,  256,   -1,  258,   -1,  291,   -1,
   -1,  263,  264,  296,   -1,  267,  268,   -1,  270,   -1,
  287,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,   -1,  287,
};
}
final static short YYFINAL=3;
final static short YYMAXTOKEN=301;
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
null,null,null,"PRIORIDAD_BAJA_ASIGNACION","ID","INT16","FLOAT32","STRING",
"PRIORIDAD_ID","LPAREN","IF","ELSE","ENDIF","PRINT","RETURN","VAR","FOR","FROM",
"TO","PLUS","MINUS","STAR","SLASH","ASSIGN","ASSIGN_COLON","EQ","NEQ","LT",
"LE_OP","GT","GE","RPAREN","LBRACE","RBRACE","UNDERSCORE","SEMICOLON","ARROW",
"COMMA","POINT","CV","CR","LE_KW","TOF","ERROR","EOF","INT_KW","FLOAT_KW",
"STRING_KW",
};
final static String yyrule[] = {
"$accept : input",
"input :",
"input : programa",
"input : programa error",
"inicio_programa : identificador",
"programa : inicio_programa LBRACE lista_sentencias RBRACE",
"programa : error LBRACE lista_sentencias RBRACE",
"lista_sentencias :",
"lista_sentencias : lista_sentencias sentencia_declarativa",
"lista_sentencias : lista_sentencias sentencia_ejecutable",
"lista_sentencias : lista_sentencias error",
"lista_sentencias_sin_return :",
"lista_sentencias_sin_return : lista_sentencias_sin_return sentencia_declarativa",
"lista_sentencias_sin_return : lista_sentencias_sin_return sentencia_ejecutable_sin_return",
"lista_sentencias_sin_return : lista_sentencias_sin_return error",
"sentencia_declarativa : declaracion_funcion",
"sentencia_declarativa : declaracion_variable",
"declaracion_variable : VAR lista_variables SEMICOLON",
"declaracion_variable : VAR SEMICOLON",
"declaracion_variable : VAR lista_variables",
"identificador : ID",
"identificador : ID UNDERSCORE ID",
"identificador : ID ERROR ID",
"identificador_completo : identificador",
"identificador_completo : identificador_completo POINT identificador",
"lista_variables : identificador_completo",
"lista_variables : lista_variables COMMA identificador_completo",
"lista_variables : lista_variables identificador_completo",
"identificador_destino : identificador_completo",
"lista_variables_destino : identificador_destino",
"lista_variables_destino : lista_variables_destino COMMA identificador_destino",
"lista_variables_destino : lista_variables_destino identificador_destino",
"inicio_funcion : lista_tipos identificador",
"inicio_funcion : lista_tipos error",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion LPAREN error RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales RPAREN error lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales error LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE",
"declaracion_funcion : inicio_funcion LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return RBRACE",
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
"parametro_formal : tipo",
"parametro_formal : sem_pasaje tipo",
"sem_pasaje : CV LE_KW",
"sem_pasaje : CR LE_KW",
"sem_pasaje : CV error",
"sem_pasaje : CR error",
"sentencia_return : RETURN LPAREN lista_expresiones RPAREN SEMICOLON",
"sentencia_return : RETURN lista_expresiones RPAREN SEMICOLON",
"sentencia_return : RETURN LPAREN lista_expresiones error SEMICOLON",
"sentencia_return : RETURN LPAREN lista_expresiones RPAREN",
"sentencia_ejecutable : asignacion",
"sentencia_ejecutable : sentencia_return",
"sentencia_ejecutable : sentencia_print",
"sentencia_ejecutable : sentencia_if",
"sentencia_ejecutable : sentencia_for",
"sentencia_ejecutable : lambda_expresion",
"sentencia_ejecutable_sin_return : asignacion",
"sentencia_ejecutable_sin_return : sentencia_print",
"sentencia_ejecutable_sin_return : sentencia_if",
"sentencia_ejecutable_sin_return : sentencia_for",
"sentencia_ejecutable_sin_return : lambda_expresion",
"sentencia_print : PRINT LPAREN expresion RPAREN SEMICOLON",
"sentencia_print : PRINT LPAREN error RPAREN SEMICOLON",
"sentencia_print : PRINT LPAREN expresion RPAREN error",
"lista_sentencias_ejecutables : sentencia_ejecutable",
"lista_sentencias_ejecutables : lista_sentencias_ejecutables sentencia_ejecutable",
"lista_sentencias_ejecutables : lista_sentencias_ejecutables error SEMICOLON",
"bloque_sentencias_ejecutables : LBRACE lista_sentencias_ejecutables RBRACE",
"fin_else : ENDIF SEMICOLON",
"fin_else : SEMICOLON",
"resto_if : ENDIF SEMICOLON",
"resto_if : SEMICOLON",
"$$1 :",
"resto_if : ELSE $$1 bloque_sentencias_ejecutables fin_else",
"resto_if : error",
"$$2 :",
"sentencia_if : IF LPAREN condicion RPAREN $$2 bloque_sentencias_ejecutables resto_if",
"sentencia_if : IF error condicion RPAREN bloque_sentencias_ejecutables resto_if",
"sentencia_if : IF LPAREN condicion bloque_sentencias_ejecutables resto_if",
"sentencia_if : IF LPAREN condicion RPAREN error resto_if",
"if_simple : IF LPAREN condicion RPAREN sentencia_ejecutable",
"encabezado_for : FOR LPAREN identificador_destino FROM factor TO factor RPAREN",
"sentencia_for : encabezado_for LBRACE lista_sentencias_ejecutables RBRACE SEMICOLON",
"sentencia_for : FOR error identificador_destino FROM factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN error FROM factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM error TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM factor error factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM factor TO error RPAREN bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR LPAREN identificador_destino FROM factor TO factor error bloque_sentencias_ejecutables SEMICOLON",
"sentencia_for : FOR error SEMICOLON",
"sentencia_for : encabezado_for LBRACE lista_sentencias_ejecutables RBRACE",
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
"asignacion : identificador_destino ASSIGN_COLON expresion",
"asignacion : lista_variables_destino ASSIGN lista_expresiones",
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

//#line 1876 "gramatic.y"

/* ======= Código Java adicional (opcional) ======= */
//#line 877 "Parser.java"
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
//#line 251 "gramatic.y"
{ 
            addError("Contenido inesperado después del final del programa. ¿Una '}' extra?");
        }
break;
case 4:
//#line 259 "gramatic.y"
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
//#line 275 "gramatic.y"
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
//#line 289 "gramatic.y"
{
            addError("Error Sintactico: Falta nombre de programa.");
        }
break;
case 8:
//#line 299 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 9:
//#line 302 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 10:
//#line 305 "gramatic.y"
{ 
            /* 1. Reinicia la bandera PRIMERO para permitir que se guarde este nuevo mensaje*/
            errorEnProduccion = false; 
            
            /* 2. Ahora llama a yyerror*/
            yyerror("Error sintáctico en sentencia. Se esperaba un ';' o inicio de sentencia válido.");
            
            removeLastGenericError(); 
        }
break;
case 14:
//#line 321 "gramatic.y"
{ 
            addError("Error en cuerpo de función. Recuperando en ';'."); 
        }
break;
case 17:
//#line 333 "gramatic.y"
{   ArrayList<SymbolEntry> entries = (ArrayList<SymbolEntry>)val_peek(1).obj;
            if(!errorfuncion){
            boolean redeclared = false;

            for (SymbolEntry entry : entries) {
                entry.setUso("variable");
                entry.setTipo("untype"); 
                
                if (symbolTable.add(entry) ) { 
                    
                }else{
                    yyerror("Error Semantico: Variable '" + entry.getLexeme() + "' redeclarada en el ámbito actual.", true);
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
        }
break;
case 18:
//#line 357 "gramatic.y"
{ 
            addError("Error Sintactico: Falta lista de variables a continuación de var.");
        }
break;
case 19:
//#line 361 "gramatic.y"
{ 
            addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la declaración de variables (Se procede a ignorarlo y continuar).");
            
            /* --- RECUPERACIÓN: PROCESAR VARIABLES --- */
            /* Copiamos la lógica de la regla correcta */
            ArrayList<SymbolEntry> entries = (ArrayList<SymbolEntry>)val_peek(0).obj;
            if(!errorfuncion && entries != null){
                boolean redeclared = false;
                for (SymbolEntry entry : entries) {
                    entry.setUso("variable");
                    entry.setTipo("untype"); 
                    
                    if (!symbolTable.add(entry)) { 
                        yyerror("Error Semantico: Variable '" + entry.getLexeme() + "' redeclarada en el ámbito actual.", true);
                        redeclared = true;
                    }
                }
                if(!listaVariablesError && !redeclared) {
                     System.out.println("Línea " + lexer.getContext().getLine() + ": Declaración de variables registrada (Recuperación).");
                }
            }
            listaVariablesError = false; 
        }
break;
case 20:
//#line 387 "gramatic.y"
{ 
          yyval.entry = val_peek(0).entry; 
      }
break;
case 21:
//#line 391 "gramatic.y"
{ 
          /* Reportamos el error personalizado */
          yyerror("Error Lexico: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter '_' no está permitido en los identificadores.", true);
          
          /* RECUPERACIÓN: Asumimos que el usuario quería usar el primer ID para seguir compilando */
          yyval.entry = val_peek(2).entry; 
      }
break;
case 22:
//#line 398 "gramatic.y"
{
            yyerror("Error Lexico: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter $2.getLexeme() no está permitido en los identificadores.", true);
    }
break;
case 23:
//#line 404 "gramatic.y"
{    
        yyval.entry = val_peek(0).entry;
    }
break;
case 24:
//#line 407 "gramatic.y"
{
        /* Caso prefijado: MAIN.A*/
        SymbolEntry scopeID = (SymbolEntry)val_peek(2).entry;
        SymbolEntry varID = (SymbolEntry)val_peek(0).entry;
        
        /* Guardamos "MAIN" dentro de "A" para usarlo después en el lookup*/
        varID.setScopePrefix(scopeID.getLexeme());
        
        yyval.entry = varID;
    }
break;
case 25:
//#line 420 "gramatic.y"
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
case 26:
//#line 433 "gramatic.y"
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
case 27:
//#line 447 "gramatic.y"
{
            addError("Error Sintáctico: Falta separador ',' en la declaración de variables.");
            
            /* Recuperación para que siga funcionando*/
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(1).obj;
            if (lista == null) lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).entry); /* $2 es el identificador*/
            yyval.obj = lista;
        }
break;
case 28:
//#line 459 "gramatic.y"
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
                yyerror("Error Semantico: La variable '" + lexema + "' no existe en el ámbito '" + prefijo + "' o no es visible.", true);
            }
        } else {
            /* Caso 1: No tiene prefijo (ej: A) -> Buscar en actual y hacia arriba*/
            encontrado = symbolTable.lookup(lexema);
            
            if (encontrado == null) {
                yyerror("Error Semantico: Variable no declarada: '" + lexema + "'", true);
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
case 29:
//#line 501 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)val_peek(0).entry);
            yyval.obj = list;
        }
break;
case 30:
//#line 506 "gramatic.y"
{            
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add(val_peek(0).entry);
            yyval.obj = lista; }
break;
case 31:
//#line 514 "gramatic.y"
{
            addError(" Error sintáctico: Se esperaba una ',' en la asignacion de variables.");
    }
break;
case 32:
//#line 521 "gramatic.y"
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
                yyerror("Error Semantico: Función redeclarada '" + se_func.getLexeme() + "'", true);
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
//#line 551 "gramatic.y"
{
        addError("Error Sintactico: Falta nombre de funcion");
        errorfuncion=true;
        yyval.entry = null;
    }
break;
case 34:
//#line 559 "gramatic.y"
{
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
                if(se == null){
                errorfuncion= false;
            }else{
                PI().generateFunctionEnd(se);

                if (listaTiposError) {
                    addError("Error Sintactico: Falta ',' en lista de tipos de retorno.");
                }
            
                if (!errorEnProduccion) { 
                    System.out.println("Línea " + lexer.getContext().getLine() + ": Declaracion de funcion detectada " + se.getLexeme());
            }
                symbolTable.popScope();
                pilaGestoresPolaca.pop();

                listaTiposError = false; 
                currentFunctionEntry = null;
        }
    }
break;
case 35:
//#line 581 "gramatic.y"
{ 
            addError("Error Sintactico: Se tiene que tener mínimo un parámetro formal.");
        }
break;
case 36:
//#line 585 "gramatic.y"
{ 
            addError("Error Sintactico: Falta '{' de apertura de función.");
        }
break;
case 37:
//#line 589 "gramatic.y"
{ 
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            listaTiposError = false; 
            addError("Error Sintactico: Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
break;
case 38:
//#line 595 "gramatic.y"
{
        SymbolEntry se = (SymbolEntry)val_peek(6).entry;
        
        /* ** ERROR SINTÁCTICO/OBLIGATORIEDAD ***/
        addError("Error Sintactico: la sentencia RETURN es obligatoria.");
        
        PI().generateFunctionEnd(se);
        symbolTable.popScope();
        pilaGestoresPolaca.pop();
        currentFunctionEntry = null;
        
    }
break;
case 39:
//#line 611 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 40:
//#line 617 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 41:
//#line 624 "gramatic.y"
{
           listaTiposError = true;
           errorEnProduccion = true; 
        }
break;
case 42:
//#line 631 "gramatic.y"
{yyval.obj = new SymbolEntry("int");}
break;
case 43:
//#line 632 "gramatic.y"
{yyval.obj = new SymbolEntry("float");}
break;
case 44:
//#line 637 "gramatic.y"
{yyval.obj=val_peek(0).entry;}
break;
case 45:
//#line 639 "gramatic.y"
{
            addError("Falta ',' entre parametros.");
        }
break;
case 47:
//#line 647 "gramatic.y"
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
                yyerror("Error Semantico: El parámetro '" + se.getLexeme() + "' ya fue declarado en este ámbito.", true);
            }
            
            System.out.println("Parametro (Defecto CVR): " + se.getLexeme());
        }
    }
break;
case 48:
//#line 672 "gramatic.y"
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
                yyerror("Error Semantico: El parámetro '" + se.getLexeme() + "' ya fue declarado en este ámbito.", true);
            }

            System.out.println("Parametro (" + config[0] + " " + config[1] + "): " + se.getLexeme());
        }
    }
break;
case 49:
//#line 695 "gramatic.y"
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
//#line 711 "gramatic.y"
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
//#line 724 "gramatic.y"
{
        addError("Error Sintáctico: Falta de nombre de parámetro formal en declaración de función.");
    }
break;
case 52:
//#line 727 "gramatic.y"
{
        addError("Error Sintáctico: Falta de nombre de parámetro formal en declaración de función.");
        }
break;
case 53:
//#line 734 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 54:
//#line 738 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 55:
//#line 743 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cv'.");
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 56:
//#line 748 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cr'.");
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 57:
//#line 755 "gramatic.y"
{
            ArrayList<PolacaElement> retornosReales = (ArrayList<PolacaElement>)val_peek(2).obj;
            if (!errorfuncion){
            /* Validar si hay errores previos de sintaxis*/
            if (listaExpresionesError) { 
                addError("Error Sintactico: Falta de ',' en argumentos de return.");
            }

            /* Validar contra la firma de la función actual*/
            if (currentFunctionEntry == null) {
                yyerror("Error semantico: Return fuera de contexto de función.");
            } else {
                List<String> tiposEsperados = currentFunctionEntry.getTiposRetorno();
                
                /* 1. VALIDAR CANTIDAD EXACTA (La función debe cumplir su promesa)*/
                /* Nota: El Tema 20 habla de flexibilidad en la ASIGNACIÓN, no en la DEFINICIÓN.*/
                /* Por ende, un return debe coincidir con la firma.*/
                if (retornosReales.size() != tiposEsperados.size()) {
                    yyerror("Error Semantico: La función '" + currentFunctionEntry.getLexeme() + 
                            "' debe retornar exactamente " + tiposEsperados.size() + 
                            " valores (firma declarada), pero retorna " + retornosReales.size() + ".", true);
                } else {
                    /* 2. CHEQUEO DE TIPOS INDIVIDUAL*/
                    for (int i = 0; i < retornosReales.size(); i++) {
                        String tipoReal = retornosReales.get(i).getResultType();
                        String tipoEsperado = tiposEsperados.get(i);
                        
                        if (!codigointermedio.TypeChecker.checkAssignment(tipoEsperado, tipoReal)) {
                            yyerror("Error Semantico: Error de Tipo en retorno #" + (i+1) + 
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
    }
break;
case 58:
//#line 800 "gramatic.y"
{
        addError("Error Sintactico: Falta de '(' en return.");
    }
break;
case 59:
//#line 803 "gramatic.y"
{
        addError("Error Sintactico: Falta de ')' en return.");
    }
break;
case 60:
//#line 806 "gramatic.y"
{
        addErrorSemicolon("Error Sintactico: Falta ; al final de la sentencia return (Se procede a ignorarlo)");
        
        /* --- RECUPERACIÓN: GENERAR RETURN --- */
        ArrayList<PolacaElement> retornosReales = (ArrayList<PolacaElement>)val_peek(1).obj;
        if (!errorfuncion && !errorEnProduccion){
             /* Validamos cantidad y tipos (copia simplificada de la lógica principal)*/
             if (currentFunctionEntry != null) {
                List<String> tiposEsperados = currentFunctionEntry.getTiposRetorno();
                if (retornosReales.size() == tiposEsperados.size()) {
                     /* Generamos el RETURN*/
                     PI().generateOperation("RETURN", false);
                     System.out.println("Línea " + lexer.getContext().getLine() + ": Return generado (Warning).");
                }
             }
        }
    }
break;
case 72:
//#line 845 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
            if(!errorfuncion){
            /* Verificamos que la expresión sea válida antes de generar*/
            if (expr != null && expr.getResultEntry() != null && !"error".equals(expr.getResultType())) {
                
                /* Agregamos la instrucción a la Polaca SIEMPRE*/
                PI().generatePrint(expr); 
                
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ": Print generado para " + expr.getResultEntry().getLexeme());
                }
            }
        }
        }
break;
case 73:
//#line 861 "gramatic.y"
{
            addError("Error Sintactico: Falta argumento en print.");
       }
break;
case 74:
//#line 864 "gramatic.y"
{ 
        addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final del PRINT.");
        
        /* --- RECUPERACIÓN: GENERAR PRINT --- */
        PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
        if(!errorfuncion && !errorEnProduccion){
            if (expr != null && expr.getResultEntry() != null && !"error".equals(expr.getResultType())) {
                PI().generatePrint(expr);
                System.out.println("Línea " + lexer.getContext().getLine() + ": Print generado (Warning).");
            }
        }
    }
break;
case 76:
//#line 882 "gramatic.y"
{
    }
break;
case 77:
//#line 885 "gramatic.y"
{ 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
break;
case 79:
//#line 896 "gramatic.y"
{
          /* CASO CORRECTO */
          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado correctamente.");
      }
break;
case 80:
//#line 906 "gramatic.y"
{
          /* CASO ERROR: Falta 'ENDIF' pero hay ';' */
          addWarning("Error Sintactico: Falta palabra clave 'endif' al finalizar la selección (else).");
          
          /* Lógica de recuperación: Hacemos el backpatch igual para que el programa siga funcionando */
          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado (recuperado por falta de ENDIF).");
      }
break;
case 81:
//#line 923 "gramatic.y"
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
case 82:
//#line 933 "gramatic.y"
{
        addWarning ("Error Sintactico: Falta palabra clave 'endif' al finalizar la selección.");
          /* Recuperamos el salto BF de la pila (guardado en sentencia_if) */
          if (!pilaSaltosBF.isEmpty()) {
              List<Integer> listaBF = pilaSaltosBF.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBF, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF Simple detectado.");
      }
break;
case 83:
//#line 945 "gramatic.y"
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
case 84:
//#line 958 "gramatic.y"
{

          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado.");
      }
break;
case 85:
//#line 968 "gramatic.y"
{ 
          addError("Error Sintactico: Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
break;
case 86:
//#line 975 "gramatic.y"
{
          
          PolacaElement cond = (PolacaElement)val_peek(1).Polacaelement;
          pilaSaltosBF.push(cond.getFalseList());
      }
break;
case 88:
//#line 985 "gramatic.y"
{ addError("Error Sintactico: Falta paréntesis de apertura '(' en IF."); }
break;
case 89:
//#line 987 "gramatic.y"
{ addError("Error Sintactico: Falta paréntesis de cierre ')' en condición."); }
break;
case 90:
//#line 989 "gramatic.y"
{ addError("Error Sintactico: Error en el cuerpo de la cláusula then."); }
break;
case 91:
//#line 994 "gramatic.y"
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
case 92:
//#line 1009 "gramatic.y"
{
        SymbolEntry id = (SymbolEntry)val_peek(5).entry;
        PolacaElement cte1 = val_peek(3).Polacaelement; 
        PolacaElement cte2 = val_peek(1).Polacaelement;
        
    if (symbolTable.lookup(id.getLexeme()) != null) {
         if (!id.getTipo().equals("int") && !id.getTipo().equals("untype")) {
             yyerror("Error Semantico: La variable del for '" + id.getLexeme() + "' debe ser de tipo 'int'.", true);
             errorEnProduccion = true;
        } else if (id.getTipo().equals("untype")) {
             id.setTipo("int");
        }
            
        if (!errorEnProduccion) {
        
        int val1 = 0;
        int val2 = 0;
        try {
             /* [CORRECCIÓN APLICADA AQUÍ] Eliminamos el sufijo 'I' o 'i' antes de parsear.*/
             String lexeme1 = cte1.getResultEntry().getLexeme().replaceAll("[Ii]", "");
             String lexeme2 = cte2.getResultEntry().getLexeme().replaceAll("[Ii]", "");
             
             val1 = Integer.parseInt(lexeme1);
             val2 = Integer.parseInt(lexeme2);
             
        } catch(Exception e) {
             /* Esto se dejaría para errores no capturados por el léxico.*/
        }
        
        PolacaElement opCte1 = PI().generateOperand(cte1.getResultEntry());
        PI().generateAssignment(id, opCte1);
        
        int labelStart = PI().getCurrentAddress();
        
        String operador;
        boolean esIncremento;
        
        /* La lógica de comparación val1 <= val2 determina si es ascendente o descendente.*/
        if (val1 <= val2) {
            operador = "<=";
            esIncremento = true;
        } else {
            operador = ">="; /* Correcto para descendente*/
            esIncremento = false; /* Correcto para decremento (-)*/
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
    else {
            addError("Error Semantico: La variable del for '" + id.getLexeme() + "' no está declarada en el ámbito actual.");
            yyval.contextfor = null; 
        }
    }
break;
case 93:
//#line 1078 "gramatic.y"
{
        ForContext ctx = val_peek(4).contextfor; 
       
            if(!errorfuncion){
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
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ": Fin de sentencia FOR generado. Tipo: " + (ctx.esIncremento ? "Ascendente" : "Descendente"));
                }
            }
    
    }
break;
case 94:
//#line 1106 "gramatic.y"
{ 
            addError("Error Sintactico: Falta parentesis de apertura en encabezado del for.");
        }
break;
case 95:
//#line 1110 "gramatic.y"
{
            addError("Error Sintactico:: Falta nombre de variable en for.");
        }
break;
case 96:
//#line 1114 "gramatic.y"
{ 
            addError("Error  Sintactico: Falta palabra clave 'from' en encabezado del for.");
        }
break;
case 97:
//#line 1118 "gramatic.y"
{ 
            addError("Error Sintactico: Falta constante inicial en encabezado del for.");
        }
break;
case 98:
//#line 1122 "gramatic.y"
{ 
            addError("Error Sintactico: Falta palabra clave 'to' en encabezado del for.");
        }
break;
case 99:
//#line 1126 "gramatic.y"
{ 
            addError("Error Sintactico: Falta constante final en encabezado del for.");
        }
break;
case 100:
//#line 1130 "gramatic.y"
{ 
            addError("Error Sintactico: Falta parentesis de cierre en encabezado for.");
        }
break;
case 101:
//#line 1134 "gramatic.y"
{ 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
break;
case 102:
//#line 1138 "gramatic.y"
{
        addErrorSemicolon("Error Sintactico: Falta ; al final de for (Se procede a ignorarlo)");
        
        /* --- RECUPERACIÓN: CERRAR EL FOR --- */
        ForContext ctx = val_peek(3).contextfor;
        if(!errorfuncion && !errorEnProduccion && ctx != null){
            /* 1. Generar incremento/decremento*/
            PolacaElement opId = PI().generateOperand(ctx.variableControl);
            SymbolEntry unoConst = new SymbolEntry("1", "constante", "int");
            symbolTable.add(unoConst); /* Asegurarnos que esté en la tabla*/
            PolacaElement opUno = PI().generateOperand(unoConst);
            
            String opAritmetico = ctx.esIncremento ? "+" : "-";
            PolacaElement resultado = PI().generateOperation(opId, opUno, opAritmetico, "int");
            PI().generateAssignment(ctx.variableControl, resultado);
            
            /* 2. Salto incondicional al inicio (loop)*/
            List<Integer> biList = PI().generateUnconditionalJump();
            PI().backpatch(biList, ctx.labelInicio);
            
            /* 3. Backpatch de la condición falsa (salida)*/
            int finDelFor = PI().getCurrentAddress();
            PI().backpatch(ctx.listaBF, finDelFor);
            
            System.out.println("Línea " + lexer.getContext().getLine() + ": Fin de FOR generado (Warning).");
        }
    }
break;
case 103:
//#line 1169 "gramatic.y"
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
case 104:
//#line 1184 "gramatic.y"
{ 
            addError("Error Sintactico: Falta de comparador en comparación.");
        }
break;
case 105:
//#line 1188 "gramatic.y"
{ 
            addError("Error Sintactico: Falta operando izquierdo en comparación.");
        }
break;
case 106:
//#line 1192 "gramatic.y"
{ 
            addError("Error Sintactico: Falta operando derecho en comparación.");
        }
break;
case 107:
//#line 1198 "gramatic.y"
{ yyval.sval = "=="; }
break;
case 108:
//#line 1199 "gramatic.y"
{ yyval.sval = "=!"; }
break;
case 109:
//#line 1200 "gramatic.y"
{ yyval.sval = "<";  }
break;
case 110:
//#line 1201 "gramatic.y"
{ yyval.sval = "<="; }
break;
case 111:
//#line 1202 "gramatic.y"
{ yyval.sval = ">";  }
break;
case 112:
//#line 1203 "gramatic.y"
{ yyval.sval = ">="; }
break;
case 113:
//#line 1209 "gramatic.y"
{
            SymbolEntry destino = (SymbolEntry)val_peek(3).entry;
            PolacaElement fuente = (PolacaElement)val_peek(1).Polacaelement;
            if(!errorfuncion){
            if (errorEnProduccion) {
                        errorEnProduccion = false; 
                    } else {
                    if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                        yyerror("Error Semantico: El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                    } else if(destino.getUso().equals("variable")){
                        if (destino.getTipo().equals("untype")){
                            /* Si era untype, tomamos el tipo de la fuente*/
                            destino.setTipo(fuente.getResultType());
                        }
                    }

                    if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                        yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
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
    }
break;
case 114:
//#line 1240 "gramatic.y"
{    
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)val_peek(1).obj;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)val_peek(3).obj;
       if(!errorfuncion && !errorEnProduccion && listaFuentes != null && listaDestinos != null){
            /* Reutilizamos la lógica. Para no duplicar tanto código en Java, */
            /* idealmente esto debería estar en una función auxiliar en el parser, */
            /* pero aquí lo expandimos para que funcione directo en el .y*/
            
            boolean esInvocacionMultiple = false;
            if (listaFuentes.size() == 1) {
                PolacaElement fuente = listaFuentes.get(0);
                SymbolEntry symFuente = fuente.getResultEntry(); 
                if (symFuente != null && "funcion".equals(symFuente.getUso()) 
                    && symFuente.getTiposRetorno() != null && !symFuente.getTiposRetorno().isEmpty()){
                        esInvocacionMultiple = true;
                }
                /* ... (lógica de detección de función origen temporal omitida por brevedad, es igual a la regla principal) ...*/
                else if (symFuente != null && symFuente.getFuncionOrigen() != null) {
                        SymbolEntry funcionReal = symFuente.getFuncionOrigen();
                        if (funcionReal.getTiposRetorno() != null && !funcionReal.getTiposRetorno().isEmpty()) {
                            esInvocacionMultiple = true;
                            symFuente = funcionReal;
                        }
                }
                
                if (esInvocacionMultiple) {
                    /* Lógica de función múltiple*/
                    List<String> tiposRetorno = symFuente.getTiposRetorno();
                    int numVars = listaDestinos.size();     
                    int numRets = tiposRetorno.size();  
                    
                    if (numRets >= numVars) {
                        for (int i = 0; i < numVars; i++) {
                            SymbolEntry destino = listaDestinos.get(i);
                            String tipoRetorno = tiposRetorno.get(i);
                            if ("untype".equals(destino.getTipo())) destino.setTipo(tipoRetorno);
                            
                            if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), tipoRetorno)) {
                                PI().generateAssignment(destino, fuente);
                            }
                        }
                    }
                }
            }
            
            if (!esInvocacionMultiple) {
                /* Asignación estándar A,B = C,D*/
                if (listaDestinos.size() == listaFuentes.size()) {
                    for (int i = 0; i < listaDestinos.size(); i++) {
                        SymbolEntry destino = listaDestinos.get(i);
                        PolacaElement fuente = listaFuentes.get(i);
                        
                        if ("untype".equals(destino.getTipo())){
                            destino.setTipo(fuente.getResultType());
                        }
                        
                        if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                            PI().generateAssignment(destino, fuente);
                        }
                        else{
                             yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                        }
                    }
                }
            }
            System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación múltiple recuperada.");
        }
        
    }
break;
case 115:
//#line 1311 "gramatic.y"
{ 
            addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la asignación.(Se procede a ignorarlo)");
            SymbolEntry destino = (SymbolEntry)val_peek(2).entry;
            PolacaElement fuente = (PolacaElement)val_peek(0).Polacaelement;
        
        if(!errorfuncion){
            if (errorEnProduccion) {
                        errorEnProduccion = false; 
                    } else {
                    if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                        yyerror("Error Semantico: El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                    } else if(destino.getUso().equals("variable")){
                        if (destino.getTipo().equals("untype")){
                            /* Si era untype, tomamos el tipo de la fuente*/
                            destino.setTipo(fuente.getResultType());
                        }
                    }

                    if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                        yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
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
            
        }
break;
case 116:
//#line 1345 "gramatic.y"
{ 
        addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la asignación múltiple.(Se procede a ignorarlo)");
        
        /* --- RECUPERACIÓN: ASIGNACIÓN MÚLTIPLE --- */
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)val_peek(0).obj;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)val_peek(2).obj;
        
        if(!errorfuncion && !errorEnProduccion && listaFuentes != null && listaDestinos != null){
            /* Reutilizamos la lógica. Para no duplicar tanto código en Java, */
            /* idealmente esto debería estar en una función auxiliar en el parser, */
            /* pero aquí lo expandimos para que funcione directo en el .y*/
            
            boolean esInvocacionMultiple = false;
            if (listaFuentes.size() == 1) {
                PolacaElement fuente = listaFuentes.get(0);
                SymbolEntry symFuente = fuente.getResultEntry(); 
                if (symFuente != null && "funcion".equals(symFuente.getUso()) 
                    && symFuente.getTiposRetorno() != null && !symFuente.getTiposRetorno().isEmpty()){
                        esInvocacionMultiple = true;
                }
                /* ... (lógica de detección de función origen temporal omitida por brevedad, es igual a la regla principal) ...*/
                else if (symFuente != null && symFuente.getFuncionOrigen() != null) {
                        SymbolEntry funcionReal = symFuente.getFuncionOrigen();
                        if (funcionReal.getTiposRetorno() != null && !funcionReal.getTiposRetorno().isEmpty()) {
                            esInvocacionMultiple = true;
                            symFuente = funcionReal;
                        }
                }
                
                if (esInvocacionMultiple) {
                    /* Lógica de función múltiple*/
                    List<String> tiposRetorno = symFuente.getTiposRetorno();
                    int numVars = listaDestinos.size();     
                    int numRets = tiposRetorno.size();  
                    
                    if (numRets >= numVars) {
                        for (int i = 0; i < numVars; i++) {
                            SymbolEntry destino = listaDestinos.get(i);
                            String tipoRetorno = tiposRetorno.get(i);
                            if ("untype".equals(destino.getTipo())) destino.setTipo(tipoRetorno);
                            
                            if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), tipoRetorno)) {
                                PI().generateAssignment(destino, fuente);
                            }
                            
                        }
                    }
                }
            }
            
            if (!esInvocacionMultiple) {
                /* Asignación estándar A,B = C,D*/
                if (listaDestinos.size() == listaFuentes.size()) {
                    for (int i = 0; i < listaDestinos.size(); i++) {
                        SymbolEntry destino = listaDestinos.get(i);
                        PolacaElement fuente = listaFuentes.get(i);
                        
                        if ("untype".equals(destino.getTipo())){
                            destino.setTipo(fuente.getResultType());
                        }
                        
                        if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                            PI().generateAssignment(destino, fuente);
                        }
                        else{
                             yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                        }
                    }
                }
            }
            System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación múltiple recuperada.");
        }
        yyerrflag = 0; 
    }
break;
case 117:
//#line 1423 "gramatic.y"
{List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)val_peek(0).Polacaelement);
            yyval.obj = list;
        }
break;
case 118:
//#line 1427 "gramatic.y"
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
case 119:
//#line 1437 "gramatic.y"
{
            /* 1. Reportamos el error*/
            addError("Error Sintáctico: Falta separador ',' entre las expresiones de la lista.");
            
            /* 2. RECUPERACIÓN:*/
            @SuppressWarnings("unchecked")
            List<PolacaElement> lista = (List<PolacaElement>)val_peek(1).obj;
            if (lista == null) lista = new ArrayList<>();
            
            /* ¡OJO! Ahora la expresión es $2, porque quitamos el token 'error' del medio*/
            lista.add((PolacaElement)val_peek(0).Polacaelement); 
            
            /* 3. Retorno*/
            yyval.obj = lista;
        }
break;
case 120:
//#line 1456 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 121:
//#line 1458 "gramatic.y"
{   
            PolacaElement elem1 = val_peek(2).Polacaelement;
            PolacaElement elem2 = val_peek(0).Polacaelement;
            if (!errorfuncion){
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Error Semantico: Tipos incompatibles para la suma " + "elem1" + ": " + elem1.getResultType() + "elem2" + ": " + elem2.getResultType(), true);
                yyval.Polacaelement = PI().generateErrorElement("error"); 
            } else {
                yyval.Polacaelement = PI().generateOperation(elem1, elem2, "+", tipoResultante);
            }
        }
        }
break;
case 122:
//#line 1472 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 123:
//#line 1476 "gramatic.y"
{
            PolacaElement elem1 = val_peek(2).Polacaelement;
            PolacaElement elem2 = val_peek(0).Polacaelement;
            if (!errorfuncion){
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Error Semantico: Tipos incompatibles para la resta " + "elem1" + ": " + elem1.getResultType() + "elem2" + ": " + elem2.getResultType(), true);
                yyval.Polacaelement = PI().generateErrorElement("error"); 
            } else {
                yyval.Polacaelement = PI().generateOperation(elem1, elem2, "-", tipoResultante);
            }
            }
        }
break;
case 124:
//#line 1490 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 125:
//#line 1497 "gramatic.y"
{
        yyval.Polacaelement = val_peek(0).Polacaelement;
    }
break;
case 126:
//#line 1500 "gramatic.y"
{
        PolacaElement term = val_peek(2).Polacaelement;
        PolacaElement fact = val_peek(0).Polacaelement;
        if (!errorfuncion){
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Error Semantico: Tipos incompatibles para la multiplicación " + "elem1" + ": " + term.getResultType() + "elem2" + ": " + fact.getResultType(), true);
            yyval.Polacaelement = PI().generateErrorElement("error"); 
        } else {
            yyval.Polacaelement = PI().generateOperation(term, fact, "*", tipoResultante);
        }
        }
    }
break;
case 127:
//#line 1514 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 128:
//#line 1518 "gramatic.y"
{
        PolacaElement term = val_peek(2).Polacaelement;
        PolacaElement fact = val_peek(0).Polacaelement;
        if (!errorfuncion){
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Error Semantico: Tipos incompatibles para la división  " + "elem1" + ": " + term.getResultType() + "elem2" + ": " + fact.getResultType(), true);
            yyval.Polacaelement = PI().generateErrorElement("error"); 
        } else {
            yyval.Polacaelement = PI().generateOperation(term, fact, "/", tipoResultante);
        }
        }
    }
break;
case 129:
//#line 1532 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 130:
//#line 1539 "gramatic.y"
{ 
            
            SymbolEntry entradaParser = (SymbolEntry)val_peek(0).entry;
            String lexema = entradaParser.getLexeme();
            if (!errorfuncion){
            if(lexema.equals("ERROR_IGNORE")){
                yyval.Polacaelement=PI().generateErrorElement("error");
            }
            else{
                String prefijo = entradaParser.getScopePrefix();
            SymbolEntry entry = null;

            /* --- LÓGICA DE BÚSQUEDA (Igual que en identificador_destino) ---*/
            if (prefijo != null) {
                entry = symbolTable.lookup(entradaParser.getLexeme(), prefijo);
                if (entry == null) yyerror("Error Semantico: Variable '" + entradaParser.getLexeme() + "' no encontrada en ámbito '" + prefijo + "'.", true);
            } else {
                entry = symbolTable.lookup(entradaParser.getLexeme());
                if (entry == null) yyerror("Error Semantico: Variable '" + entradaParser.getLexeme() + "' no declarada.", true);
            }
            
            /* Generación de error o código*/
            if (entry == null) {
                 yyval.Polacaelement = PI().generateErrorElement("error");
            } else if (entry.getUso().equals("funcion")) {
                 yyerror("Error Semantico: Uso incorrecto de función como variable.", true);
                 yyval.Polacaelement = PI().generateErrorElement("error");
            } else {
                 yyval.Polacaelement = PI().generateOperand(entry);
            }
        }
    }
        }
break;
case 131:
//#line 1573 "gramatic.y"
{
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 132:
//#line 1580 "gramatic.y"
{ 
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 133:
//#line 1587 "gramatic.y"
{ 
          /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
          SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
          symbolTable.add(se_const);
          yyval.Polacaelement = PI().generateOperand(val_peek(0).entry); 
      }
break;
case 134:
//#line 1594 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 135:
//#line 1596 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 136:
//#line 1602 "gramatic.y"
{   
        SymbolEntry funcion = (SymbolEntry)val_peek(3).entry;
        
        /* 1. Validar que sea una función*/
        if (!"funcion".equals(funcion.getUso())) {
            yyerror("Error Semantico; El identificador '" + funcion.getLexeme() + "' no es una función.", true);
            yyval.Polacaelement = PI().generateErrorElement("error");
        } else {
            @SuppressWarnings("unchecked")
            ArrayList<ParametroInvocacion> reales = (ArrayList<ParametroInvocacion>)val_peek(1).listParamInv;
            if (reales == null) reales = new ArrayList<>();

            List<SymbolEntry> formales = funcion.getParametros(); /* Lista ordenada de params formales*/
            
            /* 2. Validar Cantidad*/
            if (reales.size() != formales.size()) {
                 yyerror("Error Semantico: La función '" + funcion.getLexeme() + "' espera " + formales.size() + 
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
                    yyerror("Erro Semantico: Falta el parámetro obligatorio '" + formal.getLexeme() + 
                            "' en la invocación de '" + funcion.getLexeme() + "'.", true);
                } else {

                    if ("string".equals(paramRealMatch.getValor().getResultType())) {
                    yyerror("Error Semántico: No se permite pasar cadenas de caracteres (STRING) como parámetros a funciones.", true);
                    }
                    
                    if (!codigointermedio.TypeChecker.checkAssignment(formal.getTipo(), paramRealMatch.getValor().getResultType())) {
                         yyerror("Error Semantico: Error de Tipo en parámetro '" + formal.getLexeme() + 
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
case 137:
//#line 1698 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(1).Polacaelement;
            
            if (!expr.getResultType().equals("int")) {
                 yyerror("Error Semantico: Se intentó convertir a float una expresión que es de tipo '" + expr.getResultType() + "'.", true);
                 yyval.Polacaelement = expr; 
            } else {
                yyval.Polacaelement = PI().generateTOF(expr);
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ":  Conversión explícita TOF generada correctamente."); 
                }
            }
        }
break;
case 138:
//#line 1712 "gramatic.y"
{ 
            addError("Error Sintactico: Falta el '(' en la conversión explícita.");
        }
break;
case 139:
//#line 1716 "gramatic.y"
{ 
            addError("Error Sintactico: Falta el ')' en la conversión explícita.");
        }
break;
case 140:
//#line 1723 "gramatic.y"
{
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 141:
//#line 1730 "gramatic.y"
{
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)val_peek(2).listParamInv;
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 142:
//#line 1741 "gramatic.y"
{
        PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
        SymbolEntry idParam = (SymbolEntry)val_peek(0).entry;
        
        yyval.paramInv = new ParametroInvocacion(idParam.getLexeme(), expr);
        
        if (!errorEnProduccion) { 
            System.out.println("   -> Parametro nombrado detectado: " + idParam.getLexeme());
        }
    }
break;
case 143:
//#line 1752 "gramatic.y"
{ 
        addError("Error Sintactico: declaracion incorrecta del parámetro real. Se espera 'valor -> nombre'.");
        yyval.paramInv = new ParametroInvocacion("error", (PolacaElement)val_peek(1).Polacaelement); /* Dummy para no romper todo*/
    }
break;
case 144:
//#line 1760 "gramatic.y"
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
case 145:
//#line 1783 "gramatic.y"
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
case 146:
//#line 1835 "gramatic.y"
{ 
        addError("Error Sintactico: Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 147:
//#line 1844 "gramatic.y"
{ 
        addError("Error Sintactico: Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 148:
//#line 1854 "gramatic.y"
{ 
        addError("Error Sintactico: Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 149:
//#line 1863 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 150:
//#line 1867 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 151:
//#line 1871 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
//#line 2763 "Parser.java"
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
