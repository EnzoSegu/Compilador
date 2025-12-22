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
    private boolean error_comparacion = false;
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
   32,   32,   32,   32,   30,   30,   35,   35,   35,   35,
    1,    1,    1,   11,   11,   16,   16,   16,   13,   20,
   20,   20,   14,   14,   34,   34,   34,   34,   34,   21,
   21,   21,   17,   17,   18,   18,   18,   15,   15,   15,
   15,   15,   15,   15,   25,   25,   25,   25,   36,   36,
   36,   36,   31,   31,   31,   31,   31,   31,   33,   33,
   33,   33,   33,   38,   38,   38,   41,   41,   41,   42,
   42,   42,   43,   43,   44,   44,   45,   44,   44,   46,
   39,   39,   39,   39,   22,    2,   40,   40,   40,   40,
   40,   40,   40,   40,   40,   40,    8,    8,    8,    8,
   24,   24,   24,   24,   24,   24,   37,   37,   37,   37,
   19,   19,   19,    3,    3,    3,    3,    3,    4,    4,
    4,    4,    4,    5,    5,    5,    5,    5,    5,    7,
    6,    6,    6,   26,   26,   27,   27,   23,   10,   10,
   10,   10,    9,    9,    9,
};
final static short yylen[] = {                            2,
    0,    1,    2,    1,    4,    4,    0,    2,    2,    2,
    0,    2,    2,    2,    1,    1,    3,    2,    2,    2,
    1,    3,    3,    1,    3,    1,    3,    2,    1,    1,
    3,    2,    2,    2,    9,    9,    9,    9,    7,    1,
    3,    2,    1,    1,    1,    3,    3,    2,    3,    2,
    1,    1,    2,    2,    2,    2,    2,    2,    5,    4,
    5,    4,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    5,    5,    5,    1,    2,    3,    3,
    2,    3,    2,    1,    2,    1,    0,    4,    1,    0,
    7,    6,    5,    6,    5,    8,    3,   10,   10,    9,
   10,   10,   10,   10,    3,    4,    3,    2,    3,    3,
    1,    1,    1,    1,    1,    1,    4,    4,    3,    3,
    1,    3,    2,    1,    3,    3,    3,    3,    1,    3,
    3,    3,    3,    1,    1,    1,    1,    1,    1,    4,
    4,    4,    4,    1,    3,    3,    2,    4,    7,    7,
    7,    7,    1,    1,    1,
};
final static short yydefred[] = {                         0,
    0,    0,    0,    4,    0,    0,    7,    0,    0,    7,
    3,    0,   22,   23,    0,   10,    0,    0,    0,    0,
    0,    0,    6,   43,   44,   24,    0,   68,    0,    0,
    0,   40,    0,    0,    0,    8,    9,   15,   16,   64,
   63,   65,   66,   67,    5,    0,    0,    0,    0,    0,
  135,  136,  137,    0,    0,    0,    0,  129,  139,  138,
    0,    0,   20,   18,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,   32,   34,    0,   33,   42,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
   17,    0,    0,  105,    0,    0,    0,    0,   81,   77,
    0,   97,   25,    0,    0,   51,    0,    0,   45,    0,
    0,    0,    0,   31,   41,    0,    0,    0,  148,  111,
  112,  113,  114,  115,  116,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  144,    0,    0,    0,
    0,  126,    0,  128,    0,  131,  130,  133,  132,   60,
    0,    0,    0,    0,    0,    0,   82,    0,    0,   78,
  117,    0,   54,   57,   55,   58,   56,   48,    0,    0,
    0,   50,    0,  118,    0,    0,    0,    0,    0,  110,
    0,    0,    0,    0,    0,   89,   87,    0,   86,   93,
   75,   76,   74,  147,    0,  140,    0,   61,   59,  142,
  143,  141,    0,    0,    0,    0,    0,   79,   11,    0,
   11,   46,   11,   11,   47,   49,    0,    0,    0,    0,
   92,   94,    0,   80,    0,   85,  146,  145,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,  153,
  154,  155,    0,    0,    0,   91,    0,    0,    0,    0,
    0,    0,    0,    0,   14,   73,   12,   13,    7,   69,
   70,   71,   72,    7,    7,   39,    7,   95,  150,  151,
  152,  149,    0,   84,   88,    0,    0,    0,    0,    0,
    0,   96,    0,    0,    0,    0,    0,   83,    0,    0,
    0,    0,    0,    0,  100,   36,   38,   37,   35,   98,
   99,  101,  102,  103,  104,
};
final static short yydgoto[] = {                          3,
   26,   27,   56,   57,   58,   59,   60,   86,  253,   28,
   61,    5,   30,   31,  119,   66,   32,  121,   62,   33,
   34,  127,   35,  136,  122,  146,  147,    6,   12,   36,
   37,  245,  268,   38,   39,   40,   41,   42,   43,   44,
  111,   70,  285,  200,  235,  194,
};
final static short yysindex[] = {                        -3,
 -266,   -1,    0,    0, -254, -133,    0, -185, -114,    0,
    0, -163,    0,    0,  432,    0,   24, -238,  -92,  645,
  -89,   62,    0,    0,    0,    0,  -81,    0,  -98,  -76,
  -33,    0, -183, -239,  -95,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    3,  315,  315,  321, -175,
    0,    0,    0,  -84,   90,   54,   93,    0,    0,    0,
  -98,  -18,    0,    0,  -98, -179, -200,  328,  672,   43,
   21,  -84, -208,  -84,   21,    0,    0,   24,    0,    0,
   28,   28,   78,  697,  614,   92,   52,   97,   67,  -84,
  -37,  -84,  -84,  349,  402,  433,  449,  102,  -84,   54,
    0,   21,  -98,    0,  127,  137,  662,  135,    0,    0,
  687,    0,    0, -258,  -68,    0, -246, -218,    0,  169,
 -248, -216,  620,    0,    0,  179,  143, -147,    0,    0,
    0,    0,    0,    0,    0,  -84,   54,  490,  181,  197,
  672,    6,  183, -244,  -10,  -59,    0,  209,  223,  112,
   -4,    0,   93,    0,   93,    0,    0,    0,    0,    0,
   54,  -98,  -84,  -84,  529,  245,    0,  231,    0,    0,
    0,  235,    0,    0,    0,    0,    0,    0,  -55,  -91,
  -43,    0,  268,    0,  315,  264,  280,  289,   54,    0,
   54,    6,    6,  181,  705,    0,    0,  273,    0,    0,
    0,    0,    0,    0,  295,    0,  -84,    0,    0,    0,
    0,    0,  293,  294,  306,    2,  -84,    0,    0,  262,
    0,    0,    0,    0,    0,    0,  287,   17,   17,   17,
    0,    0,    6,    0,  181,    0,    0,    0,  -84,  -84,
  -84,  -84,  570,  298,  592,  592,  592,  457,  173,    0,
    0,    0,  303,  331, -245,    0,  -85,  333,  335,  337,
  342,  343, -160,  181,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,  317,    0,    0,  181,  181,  181,  181,  181,
  181,    0,  340,  472,  497,  512,  537,    0,  351,  353,
  354,  355,  359,  360,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,
};
final static short yyrindex[] = {                       654,
    0,   30,    0,    0,    0,  656,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  641, -138,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,   75,
    0,    0,    0,    0,    0,  -71,  165,    0,    0,    0,
  120,    0,    0,    0, -137, -234,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,  300,
    0,    0,  368,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  552,    0,    0,    0,    0,    0, -188,
    0,    0, -121,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  132,    0,    0,  378,
    0,    0,    0,    0,    0,    0,    0,    0,  577,    0,
    0,    0,  210,    0,  255,    0,    0,    0,    0,    0,
  334,  383,    0,    0,    0,    0,    0,    0,  417,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0, -140,    0,    0,    0,    0,    0,  177,    0,
  190,    0,    0,    0,    0,    0,    0,    0,    0,    0,
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
    0,    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
    5,    0,  -21,  387,  -83,    0,    0,  -44,  278,  167,
  -12,    0,  105,    0,   68,    0,  -11,    0,   44,    0,
    0,  583,    0,  581,    0,    0,  462,    0,   -9,  212,
  -67,  149,    0,    0,    0,  240,  257,  285,  302,  367,
  530,  -80,    0, -146,    0,    0,
};
final static int YYTABLESIZE=992;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         29,
   15,  110,   29,   87,    4,   46,  142,  179,   65,  174,
  281,  202,  157,  159,   94,   95,   77,   47,    2,    7,
   29,   19,   80,  166,   48,   85,   85,   89,   19,   19,
  171,   10,   19,   19,   19,   19,  180,  176,   79,  282,
  100,  182,  181,  170,  203,  231,  232,  115,  175,  116,
  114,   78,   19,  103,   29,   29,   29,    2,  192,   24,
   25,  120,   29,  137,   19,   19,  125,   52,  145,  100,
  150,  151,   13,  110,    2,  113,  177,  161,    2,  213,
  214,  216,   24,   25,  117,  118,  256,   90,  104,  162,
   24,   25,   16,   74,    2,  291,   52,   91,   29,   17,
   18,  100,   52,   19,   20,   21,   22,   75,  187,  101,
  183,  102,    8,  233,  189,   53,  191,  123,   26,   30,
   26,    9,   11,   23,  292,   26,   26,  170,   29,   26,
   26,   26,   26,  244,  120,   24,   25,   76,   30,  188,
  227,  120,  120,   14,   53,  120,  120,  120,  120,   26,
   53,   26,   30,   26,  257,  258,  259,  260,  261,  263,
   81,   26,   26,   85,  223,  120,   63,  120,    2,  120,
   49,  105,  107,   50,   51,   52,   53,  120,  120,  124,
  283,  278,   29,  293,  121,  145,  121,  121,  121,  121,
   82,  121,  121,   71,  224,  121,  121,  121,  121,   64,
  220,   72,  116,  284,   69,  299,  300,  301,  302,  303,
  304,   55,  220,  121,  116,  121,  172,  121,  148,  121,
   50,   51,   52,   53,  121,  206,  173,  121,  121,   73,
  221,  207,   29,   29,   29,   29,   29,  117,  118,   50,
   51,   52,   53,   24,   25,  204,  222,  149,  225,  117,
  118,  211,    1,   99,    2,   24,   25,  242,   55,  294,
   83,  196,   94,   95,  295,  296,   98,  297,   94,   95,
  197,  198,   99,  243,  250,  251,  252,   55,    2,  205,
  212,   29,   29,   29,   29,   21,    8,   21,   21,   21,
   21,  126,   21,   21,  199,    9,   21,   21,   21,   21,
   21,   21,   21,   21,   21,   21,   21,   21,   21,   21,
   21,   21,   21,   21,   21,   21,   21,   67,   21,   21,
   21,   21,   24,   25,   68,   21,   94,   95,   21,   21,
   21,  112,   21,   21,   21,   21,  140,  141,   21,   94,
   95,   21,   21,   21,   21,   92,   21,   21,   21,   21,
   21,  144,   93,   21,   21,   21,   21,   21,   21,   21,
   21,   21,  129,   21,   21,   21,   21,   96,   97,  246,
   21,  247,  248,   21,   21,  134,  139,  134,  134,  134,
  134,  143,  134,  134,   94,   95,  134,  134,  134,  134,
  160,  134,  134,  134,  134,  134,  210,  163,  134,  134,
  134,  134,  134,  134,  134,  134,  134,  164,  134,  134,
  134,  266,  266,  266,  266,  134,  108,  108,  134,  134,
  124,  167,  124,  124,  124,  124,  178,  124,  124,  186,
    2,  124,  124,  124,  124,   17,   18,  124,  124,   19,
   20,  185,   22,  124,  124,  124,  124,  124,  124,  124,
  124,  124,  193,  124,  124,  124,  267,  267,  267,  267,
  124,  109,  109,  124,  124,  125,  141,  125,  125,  125,
  125,  201,  125,  125,  107,  107,  125,  125,  125,  125,
  153,  155,  125,  125,  269,  274,  275,  277,  125,  125,
  125,  125,  125,  125,  125,  125,  125,  208,  125,  125,
  125,  270,  270,  270,  270,  125,  254,  255,  125,  125,
  127,  209,  127,  127,  127,  127,  217,  127,  127,  218,
  219,  127,  127,  127,  127,  226,  228,  127,  127,  271,
  271,  271,  271,  127,  127,  127,  127,  127,  127,  127,
  127,  127,  229,  127,  127,  127,  272,  272,  272,  272,
  127,  230,  237,  127,  127,  123,  173,  123,  123,  123,
  123,  236,  123,  123,  239,  240,  123,  123,  123,  123,
   84,  249,   50,   51,   52,   53,   88,  241,   50,   51,
   52,   53,  264,  106,  123,    2,  123,  279,  123,  122,
  123,  122,  122,  122,  122,  123,  122,  122,  123,  123,
  122,  122,  122,  122,  152,  298,   50,   51,   52,   53,
   55,  273,  273,  273,  273,  280,   55,  286,  122,  287,
  122,  288,  122,   28,  122,   28,  289,  290,  305,  122,
   28,   28,  122,  122,   28,   28,   28,   28,   27,  310,
   27,  311,  312,  313,   55,   27,   27,  314,  315,   27,
   27,   27,   27,    1,   28,    2,   28,  154,   28,   50,
   51,   52,   53,   90,  128,  138,   28,   28,  238,   27,
  195,   27,  106,   27,  106,    0,    0,    0,    0,  106,
  106,   27,   27,  106,  106,  106,  106,   16,  156,    2,
   50,   51,   52,   53,   17,   18,    0,   55,   19,   20,
   21,   22,    0,  106,  158,   80,   50,   51,   52,   53,
    0,    0,  265,    0,    2,  106,  106,    0,   45,   17,
   18,    0,    0,   19,   20,   21,   22,   16,   55,    2,
   24,   25,    0,    0,   17,   18,    0,    0,   19,   20,
   21,   22,    0,  276,   55,  190,    0,   50,   51,   52,
   53,    0,   16,    0,    2,   24,   25,    0,  306,   17,
   18,    0,    0,   19,   20,   21,   22,   16,    0,    2,
   24,   25,    0,    0,   17,   18,    0,    0,   19,   20,
   21,   22,    0,  307,  215,   55,   50,   51,   52,   53,
    0,    0,   16,    0,    2,   24,   25,    0,  308,   17,
   18,    0,    0,   19,   20,   21,   22,  119,    0,  119,
   24,   25,    0,    0,  119,  119,    0,    0,  119,  119,
  119,  119,    0,  309,   55,  262,    0,   50,   51,   52,
   53,    0,   62,    0,   62,   24,   25,    0,  119,   62,
   62,    0,    0,   62,   62,   62,   62,  265,    0,    2,
  119,  119,    0,    0,   17,   18,    0,    0,   19,   20,
   21,   22,    0,   62,    0,   55,    0,    0,    0,    0,
    0,   50,   51,   52,   53,   62,   62,   50,   51,   52,
   53,    0,    0,    0,    0,    0,   94,   95,    0,    0,
   24,   25,  130,  131,  132,  133,  134,  135,   29,   29,
   29,   29,   50,   51,   52,   53,    0,   54,  184,   55,
   99,   29,    0,    0,    0,   55,    0,   29,   29,   50,
   51,   52,   53,    0,    0,    0,    0,  108,    0,    2,
    0,   29,  165,    0,   17,   18,   29,    0,   19,   20,
   55,   22,  168,    0,    2,    0,    0,    0,    0,   17,
   18,    0,    0,   19,   20,    0,   22,   55,  109,    0,
  168,    0,    2,    0,    0,    0,    0,   17,   18,    0,
    0,   19,   20,  169,   22,  130,  131,  132,  133,  134,
  135,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  234,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         12,
   10,   69,   15,   48,    0,   17,   87,  256,   21,  256,
  256,  256,   96,   97,  273,  274,  256,  256,  258,  286,
   33,  256,   34,  107,  263,   47,   48,   49,  263,  264,
  289,  286,  267,  268,  269,  270,  285,  256,   34,  285,
   62,  258,  291,  111,  289,  192,  193,  256,  295,  258,
   72,  291,  287,   66,   67,   68,   69,  258,  139,  299,
  300,   73,   75,   85,  299,  300,   78,  256,   90,   91,
   92,   93,  258,  141,  258,   71,  295,   99,  258,  163,
  164,  165,  299,  300,  293,  294,  233,  263,  289,  102,
  299,  300,  256,  277,  258,  256,  285,   54,  111,  263,
  264,  123,  291,  267,  268,  269,  270,  291,  256,  289,
  122,  291,  288,  194,  136,  256,  138,   74,  256,  258,
  258,  297,  256,  287,  285,  263,  264,  195,  141,  267,
  268,  269,  270,  217,  256,  299,  300,   33,  277,  287,
  185,  263,  264,  258,  285,  267,  268,  269,  270,  287,
  291,  289,  291,  291,  235,  239,  240,  241,  242,  243,
  256,  299,  300,  185,  256,  287,  256,  179,  258,  181,
  263,   67,   68,  258,  259,  260,  261,  299,  300,   75,
  266,  249,  195,  264,  256,  207,  258,  259,  260,  261,
  286,  263,  264,  292,  286,  267,  268,  269,  270,  289,
  256,  278,  258,  289,  286,  286,  287,  288,  289,  290,
  291,  296,  256,  285,  258,  287,  285,  289,  256,  291,
  258,  259,  260,  261,  296,  285,  295,  299,  300,  263,
  286,  291,  245,  246,  247,  248,  249,  293,  294,  258,
  259,  260,  261,  299,  300,  256,  179,  285,  181,  293,
  294,  256,  256,  291,  258,  299,  300,  256,  296,  269,
  258,  256,  273,  274,  274,  275,  285,  277,  273,  274,
  265,  266,  291,  272,  258,  259,  260,  296,  258,  290,
  285,  294,  295,  296,  297,  256,  288,  258,  259,  260,
  261,  264,  263,  264,  289,  297,  267,  268,  269,  270,
  271,  272,  273,  274,  275,  276,  277,  278,  279,  280,
  281,  282,  283,  284,  285,  286,  287,  256,  289,  290,
  291,  292,  299,  300,  263,  296,  273,  274,  299,  300,
  256,  289,  258,  259,  260,  261,  285,  286,  264,  273,
  274,  267,  268,  269,  270,  256,  272,  273,  274,  275,
  276,  285,  263,  279,  280,  281,  282,  283,  284,  285,
  286,  287,  285,  289,  290,  291,  292,  275,  276,  221,
  296,  223,  224,  299,  300,  256,  285,  258,  259,  260,
  261,  285,  263,  264,  273,  274,  267,  268,  269,  270,
  289,  272,  273,  274,  275,  276,  285,  271,  279,  280,
  281,  282,  283,  284,  285,  286,  287,  271,  289,  290,
  291,  245,  246,  247,  248,  296,  285,  286,  299,  300,
  256,  287,  258,  259,  260,  261,  258,  263,  264,  287,
  258,  267,  268,  269,  270,  263,  264,  273,  274,  267,
  268,  263,  270,  279,  280,  281,  282,  283,  284,  285,
  286,  287,  256,  289,  290,  291,  245,  246,  247,  248,
  296,  285,  286,  299,  300,  256,  286,  258,  259,  260,
  261,  289,  263,  264,  285,  286,  267,  268,  269,  270,
   94,   95,  273,  274,  245,  246,  247,  248,  279,  280,
  281,  282,  283,  284,  285,  286,  287,  289,  289,  290,
  291,  245,  246,  247,  248,  296,  229,  230,  299,  300,
  256,  289,  258,  259,  260,  261,  272,  263,  264,  289,
  286,  267,  268,  269,  270,  258,  263,  273,  274,  245,
  246,  247,  248,  279,  280,  281,  282,  283,  284,  285,
  286,  287,  263,  289,  290,  291,  245,  246,  247,  248,
  296,  263,  258,  299,  300,  256,  295,  258,  259,  260,
  261,  289,  263,  264,  272,  272,  267,  268,  269,  270,
  256,  285,  258,  259,  260,  261,  256,  272,  258,  259,
  260,  261,  285,  256,  285,  258,  287,  285,  289,  256,
  291,  258,  259,  260,  261,  296,  263,  264,  299,  300,
  267,  268,  269,  270,  256,  289,  258,  259,  260,  261,
  296,  245,  246,  247,  248,  285,  296,  285,  285,  285,
  287,  285,  289,  256,  291,  258,  285,  285,  289,  296,
  263,  264,  299,  300,  267,  268,  269,  270,  256,  289,
  258,  289,  289,  289,  296,  263,  264,  289,  289,  267,
  268,  269,  270,    0,  287,    0,  289,  256,  291,  258,
  259,  260,  261,  286,   82,   85,  299,  300,  207,  287,
  141,  289,  256,  291,  258,   -1,   -1,   -1,   -1,  263,
  264,  299,  300,  267,  268,  269,  270,  256,  256,  258,
  258,  259,  260,  261,  263,  264,   -1,  296,  267,  268,
  269,  270,   -1,  287,  256,  289,  258,  259,  260,  261,
   -1,   -1,  256,   -1,  258,  299,  300,   -1,  287,  263,
  264,   -1,   -1,  267,  268,  269,  270,  256,  296,  258,
  299,  300,   -1,   -1,  263,  264,   -1,   -1,  267,  268,
  269,  270,   -1,  287,  296,  256,   -1,  258,  259,  260,
  261,   -1,  256,   -1,  258,  299,  300,   -1,  287,  263,
  264,   -1,   -1,  267,  268,  269,  270,  256,   -1,  258,
  299,  300,   -1,   -1,  263,  264,   -1,   -1,  267,  268,
  269,  270,   -1,  287,  256,  296,  258,  259,  260,  261,
   -1,   -1,  256,   -1,  258,  299,  300,   -1,  287,  263,
  264,   -1,   -1,  267,  268,  269,  270,  256,   -1,  258,
  299,  300,   -1,   -1,  263,  264,   -1,   -1,  267,  268,
  269,  270,   -1,  287,  296,  256,   -1,  258,  259,  260,
  261,   -1,  256,   -1,  258,  299,  300,   -1,  287,  263,
  264,   -1,   -1,  267,  268,  269,  270,  256,   -1,  258,
  299,  300,   -1,   -1,  263,  264,   -1,   -1,  267,  268,
  269,  270,   -1,  287,   -1,  296,   -1,   -1,   -1,   -1,
   -1,  258,  259,  260,  261,  299,  300,  258,  259,  260,
  261,   -1,   -1,   -1,   -1,   -1,  273,  274,   -1,   -1,
  299,  300,  279,  280,  281,  282,  283,  284,  258,  259,
  260,  261,  258,  259,  260,  261,   -1,  263,  289,  296,
  291,  271,   -1,   -1,   -1,  296,   -1,  277,  278,  258,
  259,  260,  261,   -1,   -1,   -1,   -1,  256,   -1,  258,
   -1,  291,  271,   -1,  263,  264,  296,   -1,  267,  268,
  296,  270,  256,   -1,  258,   -1,   -1,   -1,   -1,  263,
  264,   -1,   -1,  267,  268,   -1,  270,  296,  287,   -1,
  256,   -1,  258,   -1,   -1,   -1,   -1,  263,  264,   -1,
   -1,  267,  268,  287,  270,  279,  280,  281,  282,  283,
  284,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
   -1,  287,
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
"declaracion_variable : VAR error",
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
"parametro_formal : error LE_KW",
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
"bloque_sentencias_ejecutables : LBRACE RBRACE",
"bloque_sentencias_ejecutables : LBRACE error RBRACE",
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
"sentencia_for : encabezado_for bloque_sentencias_ejecutables SEMICOLON",
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

//#line 1906 "gramatic.y"

/* ======= Código Java adicional (opcional) ======= */
//#line 890 "Parser.java"
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
            
            
            
            removeLastGenericError(); 
        }
break;
case 14:
//#line 320 "gramatic.y"
{ 
            addError("Error en cuerpo de función. Recuperando en ';'."); 
        }
break;
case 17:
//#line 332 "gramatic.y"
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
//#line 356 "gramatic.y"
{ 
            addError("Error Sintactico: Falta lista de variables a continuación de var.");
        }
break;
case 19:
//#line 360 "gramatic.y"
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
//#line 384 "gramatic.y"
{ 
            addError("Error Sintactico: Falta lista de variables a continuación de var.");
        }
break;
case 21:
//#line 390 "gramatic.y"
{ 
          yyval.entry = val_peek(0).entry; 
      }
break;
case 22:
//#line 394 "gramatic.y"
{ 
          /* Reportamos el error personalizado */
          yyerror("Error Lexico: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter '_' no está permitido en los identificadores.", true);
          
          /* RECUPERACIÓN: Asumimos que el usuario quería usar el primer ID para seguir compilando */
          yyval.entry = val_peek(2).entry; 
      }
break;
case 23:
//#line 401 "gramatic.y"
{
            yyerror("Error Lexico: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter $2.getLexeme() no está permitido en los identificadores.", true);
    }
break;
case 24:
//#line 407 "gramatic.y"
{    
        yyval.entry = val_peek(0).entry;
    }
break;
case 25:
//#line 410 "gramatic.y"
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
//#line 423 "gramatic.y"
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
//#line 436 "gramatic.y"
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
//#line 450 "gramatic.y"
{
            addError("Error Sintáctico: Falta coma ',' en la declaración de variables.");
            
            /* Recuperación para que siga funcionando*/
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(1).obj;
            if (lista == null) lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).entry); /* $2 es el identificador*/
            yyval.obj = lista;
        }
break;
case 29:
//#line 462 "gramatic.y"
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
case 30:
//#line 504 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)val_peek(0).entry);
            yyval.obj = list;
        }
break;
case 31:
//#line 509 "gramatic.y"
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
//#line 517 "gramatic.y"
{
            addError(" Error sintáctico: Se esperaba una ',' en la asignacion de variables.");
    }
break;
case 33:
//#line 524 "gramatic.y"
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
case 34:
//#line 554 "gramatic.y"
{
        addError("Error Sintactico: Falta nombre de funcion");
        errorfuncion=true;
        yyval.entry = null;
    }
break;
case 35:
//#line 562 "gramatic.y"
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
case 36:
//#line 584 "gramatic.y"
{ 
            addError("Error Sintactico: Se tiene que tener mínimo un parámetro formal.");
        }
break;
case 37:
//#line 588 "gramatic.y"
{ 
            addError("Error Sintactico: Falta '{' de apertura de función.");
        }
break;
case 38:
//#line 592 "gramatic.y"
{ 
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            listaTiposError = false; 
            addError("Error Sintactico: Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
break;
case 39:
//#line 598 "gramatic.y"
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
case 40:
//#line 614 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 41:
//#line 620 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 42:
//#line 627 "gramatic.y"
{
           listaTiposError = true;
           errorEnProduccion = true; 
        }
break;
case 43:
//#line 634 "gramatic.y"
{yyval.obj = new SymbolEntry("int");}
break;
case 44:
//#line 635 "gramatic.y"
{yyval.obj = new SymbolEntry("float");}
break;
case 45:
//#line 640 "gramatic.y"
{yyval.obj=val_peek(0).entry;}
break;
case 46:
//#line 642 "gramatic.y"
{
            addError("Falta ',' entre parametros.");
        }
break;
case 48:
//#line 650 "gramatic.y"
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
case 49:
//#line 675 "gramatic.y"
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
case 50:
//#line 698 "gramatic.y"
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
case 51:
//#line 714 "gramatic.y"
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
case 52:
//#line 727 "gramatic.y"
{
        addError("Error Sintáctico: Falta de nombre de parámetro formal en declaración de función.");
    }
break;
case 53:
//#line 730 "gramatic.y"
{
        addError("Error Sintáctico: Falta de nombre de parámetro formal en declaración de función.");
        }
break;
case 54:
//#line 734 "gramatic.y"
{
        addError("Error Sintactico: Falta la semantica de pasaje(cv o cr) antes de directiva 'le'");
        yyval.semantica = new String[]{"error_pasaje", "le"};
    }
break;
case 55:
//#line 742 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 56:
//#line 746 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 57:
//#line 751 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cv'.");
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 58:
//#line 756 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cr'.");
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 59:
//#line 763 "gramatic.y"
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
case 60:
//#line 808 "gramatic.y"
{
        addError("Error Sintactico: Falta de '(' en return.");
    }
break;
case 61:
//#line 811 "gramatic.y"
{
        addError("Error Sintactico: Falta de ')' en return.");
    }
break;
case 62:
//#line 814 "gramatic.y"
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
case 74:
//#line 853 "gramatic.y"
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
case 75:
//#line 869 "gramatic.y"
{
            addError("Error Sintactico: Falta argumento en print.");
       }
break;
case 76:
//#line 872 "gramatic.y"
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
case 78:
//#line 890 "gramatic.y"
{
    }
break;
case 79:
//#line 893 "gramatic.y"
{ 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
break;
case 81:
//#line 901 "gramatic.y"
{
            addError("Error Sintáctico: El cuerpo de la iteración o bloque no puede estar vacío.");
        }
break;
case 82:
//#line 905 "gramatic.y"
{
            addError("Error Sintáctico: Error en el contenido del bloque o cuerpo vacío.");
            yyerrflag = 0;
        }
break;
case 83:
//#line 913 "gramatic.y"
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
case 84:
//#line 923 "gramatic.y"
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
case 85:
//#line 940 "gramatic.y"
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
case 86:
//#line 950 "gramatic.y"
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
case 87:
//#line 962 "gramatic.y"
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
case 88:
//#line 975 "gramatic.y"
{

          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado.");
      }
break;
case 89:
//#line 985 "gramatic.y"
{ 
          addError("Error Sintactico: Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
break;
case 90:
//#line 992 "gramatic.y"
{
          if (!error_comparacion){
            PolacaElement cond = (PolacaElement)val_peek(1).Polacaelement;
            pilaSaltosBF.push(cond.getFalseList());
          }
          else{
            error_comparacion = false;
          }
      }
break;
case 92:
//#line 1006 "gramatic.y"
{ addError("Error Sintactico: Falta paréntesis de apertura '(' en IF."); }
break;
case 93:
//#line 1008 "gramatic.y"
{ addError("Error Sintactico: Falta paréntesis de cierre ')' en condición."); }
break;
case 94:
//#line 1010 "gramatic.y"
{ addError("Error Sintactico: Error en el cuerpo de la cláusula then."); }
break;
case 95:
//#line 1015 "gramatic.y"
{ 
        if ( !error_comparacion){
            PolacaElement cond = (PolacaElement)val_peek(2).Polacaelement;
            List<Integer> listaBF = cond.getFalseList();

            
            if (!errorEnProduccion) { 
                System.out.println("Línea " + lexer.getContext().getLine() + 
                    ": If simple detectado (sin backpatch aún)");
            }
            
            yyval.obj = listaBF;
        }
        else{
            error_comparacion = false;
        }
    }
break;
case 96:
//#line 1035 "gramatic.y"
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
case 97:
//#line 1104 "gramatic.y"
{
        ForContext ctx = val_peek(2).contextfor; 
       
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
case 98:
//#line 1132 "gramatic.y"
{ 
            addError("Error Sintactico: Falta parentesis de apertura en encabezado del for.");
        }
break;
case 99:
//#line 1136 "gramatic.y"
{
            addError("Error Sintactico:: Falta nombre de variable en for.");
        }
break;
case 100:
//#line 1140 "gramatic.y"
{ 
            addError("Error  Sintactico: Falta palabra clave 'from' en encabezado del for.");
        }
break;
case 101:
//#line 1144 "gramatic.y"
{ 
            addError("Error Sintactico: Falta constante inicial en encabezado del for.");
        }
break;
case 102:
//#line 1148 "gramatic.y"
{ 
            addError("Error Sintactico: Falta palabra clave 'to' en encabezado del for.");
        }
break;
case 103:
//#line 1152 "gramatic.y"
{ 
            addError("Error Sintactico: Falta constante final en encabezado del for.");
        }
break;
case 104:
//#line 1156 "gramatic.y"
{ 
            addError("Error Sintactico: Falta parentesis de cierre en encabezado for.");
        }
break;
case 105:
//#line 1160 "gramatic.y"
{ 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
break;
case 106:
//#line 1164 "gramatic.y"
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
case 107:
//#line 1195 "gramatic.y"
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
case 108:
//#line 1210 "gramatic.y"
{ 
            error_comparacion= true;
            addError("Error Sintactico: Falta de comparador en comparación.");
        }
break;
case 109:
//#line 1215 "gramatic.y"
{ 
            error_comparacion= true;
            addError("Error Sintactico: Falta operando izquierdo en comparación.");
        }
break;
case 110:
//#line 1220 "gramatic.y"
{ 
            error_comparacion= true;
            addError("Error Sintactico: Falta operando derecho en comparación.");
        }
break;
case 111:
//#line 1227 "gramatic.y"
{ yyval.sval = "=="; }
break;
case 112:
//#line 1228 "gramatic.y"
{ yyval.sval = "=!"; }
break;
case 113:
//#line 1229 "gramatic.y"
{ yyval.sval = "<";  }
break;
case 114:
//#line 1230 "gramatic.y"
{ yyval.sval = "<="; }
break;
case 115:
//#line 1231 "gramatic.y"
{ yyval.sval = ">";  }
break;
case 116:
//#line 1232 "gramatic.y"
{ yyval.sval = ">="; }
break;
case 117:
//#line 1238 "gramatic.y"
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
case 118:
//#line 1269 "gramatic.y"
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
case 119:
//#line 1340 "gramatic.y"
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
case 120:
//#line 1374 "gramatic.y"
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
case 121:
//#line 1452 "gramatic.y"
{List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)val_peek(0).Polacaelement);
            yyval.obj = list;
        }
break;
case 122:
//#line 1456 "gramatic.y"
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
case 123:
//#line 1466 "gramatic.y"
{
            /* 1. Reportamos el error*/
            addError("Error Sintáctico: Falta coma ',' entre las expresiones del lado derecho.");
            
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
case 124:
//#line 1485 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 125:
//#line 1487 "gramatic.y"
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
case 126:
//#line 1501 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 127:
//#line 1505 "gramatic.y"
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
case 128:
//#line 1519 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 129:
//#line 1526 "gramatic.y"
{
        yyval.Polacaelement = val_peek(0).Polacaelement;
    }
break;
case 130:
//#line 1529 "gramatic.y"
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
case 131:
//#line 1543 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 132:
//#line 1547 "gramatic.y"
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
case 133:
//#line 1561 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 134:
//#line 1569 "gramatic.y"
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
case 135:
//#line 1603 "gramatic.y"
{
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 136:
//#line 1610 "gramatic.y"
{ 
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 137:
//#line 1617 "gramatic.y"
{ 
          /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
          SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
          symbolTable.add(se_const);
          yyval.Polacaelement = PI().generateOperand(val_peek(0).entry); 
      }
break;
case 138:
//#line 1624 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 139:
//#line 1626 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 140:
//#line 1632 "gramatic.y"
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
case 141:
//#line 1728 "gramatic.y"
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
case 142:
//#line 1742 "gramatic.y"
{ 
            addError("Error Sintactico: Falta el '(' en la conversión explícita.");
        }
break;
case 143:
//#line 1746 "gramatic.y"
{ 
            addError("Error Sintactico: Falta el ')' en la conversión explícita.");
        }
break;
case 144:
//#line 1753 "gramatic.y"
{
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 145:
//#line 1760 "gramatic.y"
{
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)val_peek(2).listParamInv;
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 146:
//#line 1771 "gramatic.y"
{
        PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
        SymbolEntry idParam = (SymbolEntry)val_peek(0).entry;
        
        yyval.paramInv = new ParametroInvocacion(idParam.getLexeme(), expr);
        
        if (!errorEnProduccion) { 
            System.out.println("   -> Parametro nombrado detectado: " + idParam.getLexeme());
        }
    }
break;
case 147:
//#line 1782 "gramatic.y"
{ 
        addError("Error Sintactico: declaracion incorrecta del parámetro real. Se espera 'valor -> nombre'.");
        yyval.paramInv = new ParametroInvocacion("error", (PolacaElement)val_peek(1).Polacaelement); /* Dummy para no romper todo*/
    }
break;
case 148:
//#line 1790 "gramatic.y"
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
case 149:
//#line 1813 "gramatic.y"
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
case 150:
//#line 1865 "gramatic.y"
{ 
        addError("Error Sintactico: Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 151:
//#line 1874 "gramatic.y"
{ 
        addError("Error Sintactico: Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 152:
//#line 1884 "gramatic.y"
{ 
        addError("Error Sintactico: Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 153:
//#line 1893 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 154:
//#line 1897 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 155:
//#line 1901 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
//#line 2813 "Parser.java"
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
