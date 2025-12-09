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

    /* --- Gestores Principales ---*/
//#line 190 "gramatic.y"

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

    private void removeLastGenericError() {
        if (listaErrores.isEmpty()) return;
    
    /* El mensaje genérico de BYACC/J siempre contiene el prefijo "Error Sintactico" y "syntax error"*/
        String lastError = listaErrores.get(listaErrores.size() - 1);
    
        if  (lastError.contains("syntax error")) {
            listaErrores.remove(listaErrores.size() - 1);
        /* NO se toca errorEnProduccion aquí, ya que el error existe.*/
        }
}

    /* 1. Método para tus errores manuales en la gramática (Sintácticos)*/
    private void addError(String mensaje) {
        removeLastGenericError();
        int linea=lexer.getContext().getLine();
        String error = "Línea " + linea + ": " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }
    /* 1. Método para tus errores manuales en la gramática (Sintácticos)*/
    private void addErrorSemicolon(String mensaje) {
        removeLastGenericError();
        int linea = lexer.getContext().getLine()-1;
        String error = "Línea " + linea + ": " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }
    private void addErrorLex(String mensaje, int linea) {
        removeLastGenericError();
        String error = "Línea " + linea + ": " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }


    /* 2. Método para errores SEMÁNTICOS (Tipos, declaraciones)*/
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
   35,    1,    1,    1,   11,   11,   16,   16,   16,   13,
   20,   20,   14,   34,   34,   34,   34,   34,   34,   34,
   21,   21,   21,   17,   17,   18,   18,   18,   15,   15,
   15,   15,   25,   25,   25,   25,   36,   36,   36,   31,
   31,   31,   31,   31,   31,   33,   33,   33,   33,   33,
   38,   38,   38,   41,   41,   41,   42,   43,   44,   43,
   43,   45,   39,   39,   39,   39,   22,    2,   40,   40,
   40,   40,   40,   40,   40,   40,   40,    8,    8,    8,
    8,   24,   24,   24,   24,   24,   24,   37,   37,   37,
   37,   19,   19,   19,    3,    3,    3,    3,    3,    4,
    4,    4,    4,    4,    5,    5,    5,    5,    5,    5,
    7,    6,    6,    6,   26,   26,   27,   27,   23,   10,
   10,   10,   10,    9,    9,    9,
};
final static short yylen[] = {                            2,
    0,    1,    2,    1,    4,    4,    4,    3,    0,    2,
    2,    3,    0,    2,    2,    3,    1,    1,    3,    2,
    3,    1,    3,    3,    1,    3,    1,    3,    2,    1,
    1,    3,    2,    9,   10,    9,    9,    9,    9,    7,
    1,    3,    2,    1,    1,    1,    3,    3,    2,    3,
    2,    1,    2,    2,    2,    2,    5,    4,    5,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    5,    5,    5,    0,    2,    3,    3,    2,    0,    5,
    1,    0,    7,    6,    6,    6,    5,    8,    5,   10,
   10,    9,   10,   10,   10,   10,    3,    3,    2,    3,
    3,    1,    1,    1,    1,    1,    1,    4,    4,    4,
    4,    1,    3,    2,    1,    3,    3,    3,    3,    1,
    3,    3,    3,    3,    1,    1,    1,    1,    1,    1,
    4,    4,    4,    4,    1,    3,    3,    2,    4,    7,
    7,    7,    7,    1,    1,    1,
};
final static short yydefred[] = {                         0,
    0,    0,    0,    4,    0,    0,    9,    0,    0,    9,
    0,    3,    0,   23,   24,    0,    0,    0,    0,    0,
    0,    0,    0,   44,   45,   25,    0,   65,    0,    0,
    0,   41,    0,    0,    0,   10,   11,   17,   18,   61,
   60,   62,   63,   64,    0,    6,    0,    5,   12,    0,
    0,    0,    0,  128,  126,  127,    0,    0,    0,    0,
  120,  130,  129,    0,    0,   20,    0,    0,    0,    0,
    0,   74,    0,    0,    0,    0,    0,    0,    0,    0,
   33,   43,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,   21,   19,    0,    0,   97,    0,    0,    0,    0,
    0,   26,    0,    0,    0,   52,    0,    0,   46,    0,
    0,    0,    0,   32,    0,   42,    0,    0,    0,  102,
  103,  104,  105,  106,  107,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,  135,    0,    0,    0,    0,
  117,    0,  119,    0,  122,  121,  124,  123,   58,    0,
    0,    0,    0,    0,    0,  139,    0,    0,   75,  110,
  108,    0,    0,   55,   53,   56,   54,   49,    0,    0,
    0,   51,    0,  111,  109,    0,    0,    0,    0,    0,
    0,    0,  101,    0,   74,    0,    0,    0,    0,   72,
   73,   71,  138,    0,  131,    0,   59,   57,  133,  134,
  132,    0,    0,    0,    0,    0,   76,   89,    0,   13,
   13,   47,   13,   13,   48,   50,   13,    0,    0,    0,
    0,    0,   81,   79,    0,   84,   85,   86,    0,  137,
  136,    0,    0,    0,    0,    0,    0,   13,    0,    0,
    0,    0,    0,    0,  144,  145,  146,    0,    0,    0,
   77,    0,   78,   83,    0,    0,    0,    0,    0,    0,
    0,    0,    0,   70,   14,   15,    9,   66,   67,   68,
   69,    9,    9,   40,    9,    9,   87,  141,  142,  143,
  140,    0,    0,    0,    0,    0,    0,    0,   88,    0,
    9,   16,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,   92,    0,   36,   39,   37,   34,
    0,   80,   90,   91,   93,   94,   95,   96,   35,
};
final static short yydgoto[] = {                          3,
   26,   27,   59,   60,   61,   62,   63,   87,  258,   28,
   64,    5,   30,   31,  119,   68,   32,  121,   65,   33,
   34,  128,   35,  136,  122,  145,  146,    6,   11,   36,
   37,  249,  276,   38,   39,   40,   41,   42,   43,   44,
  111,  196,  236,  262,  199,
};
final static short yysindex[] = {                       -49,
 -260, -254,    0,    0, -250, -220,    0, -217, -214,    0,
  165,    0, -154,    0,    0,  -34, -193, -111, -177,  -23,
 -161,  -28,   25,    0,    0,    0, -142,    0,  -66,  -46,
   44,    0, -251, -212, -118,    0,    0,    0,    0,    0,
    0,    0,    0,    0, -193,    0, -193,    0,    0,   54,
   54,   76, -249,    0,    0,    0, -125,   63,   67,   87,
    0,    0,    0,  -66,  505,    0,  -66, -127, -148,  132,
   -5,    0,   20, -125,   59, -226, -125,   20, -174,   25,
    0,    0,   29,   29,  605,  497,   61, -131,   80, -179,
 -125,  197, -125, -125,   98,  120,  144,  237,   15, -125,
   67,    0,    0,   20,  -66,    0,  117,  131,  370,   83,
  528,    0,  111, -174,  124,    0, -231, -205,    0,  163,
 -207, -218,  464,    0, -188,    0,  141,  154, -166,    0,
    0,    0,    0,    0,    0, -125,   67,  253,  164,  164,
  195,  171, -239,   -1,  -17,    0,  192,  200, -124,  219,
    0,   87,    0,   87,    0,    0,    0,    0,    0,   67,
  -66, -125, -125,  292,  183,    0,  201,  205,    0,    0,
    0, -169,  182,    0,    0,    0,    0,    0, -176, -116,
 -174,    0,  241,    0,    0, -174,  224,   54,  239,  240,
  250,   67,    0,   67,    0,  209,  209,  209,  164,    0,
    0,    0,    0,  272,    0, -125,    0,    0,    0,    0,
    0,  264,  269,  270, -108, -125,    0,    0,  256,    0,
    0,    0,    0,    0,    0,    0,    0,  271,    8,    8,
    8,  547,    0,    0,  266,    0,    0,    0,  209,    0,
    0, -125, -125, -125, -125,  331,  277,    0,  179,  179,
  179,   19,  179,  558,    0,    0,    0,  278,  285, -119,
    0,  164,    0,    0,  288,  293,  295,  296,  309,  -38,
  164,  179,  306,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  334,  164,  164,  164,  164,  164,  164,    0,  307,
    0,    0,   42,   64,   86,  108,  203,  311,  312,  313,
  320,  322,  323,  324,    0,  130,    0,    0,    0,    0,
 -193,    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yyrindex[] = {                       615,
    0,  -78,    0,    0,  217,  616,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    1,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  512, -245,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    4,    0,    0,    0,
    0,    0,  247,    0,    0,    0,    0,    0,  411,  325,
    0,    0,    0,  286,    0,    0,  -72,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  450,    0,    0,    0,  -43,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    6,    0,    0,    0,
  333,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  364,    0,  403,    0,    0,    0,    0,    0,  458,
  -31,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,   32,    0,   35,    0,    0,    0,    0,    0,    0,
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
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  152,    0,    0,    0,    0,    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
   16,    0,  -39,  315,  -89,    0,    0,  -37,  211,  476,
  -11,    0,  355,    0,  391,    0,  -16,  -58,  -35,    0,
    0,  534,    0,  540,    0,    0,  425,    0,   -4,  584,
  -96,   26,    0,    0,    0,  592,  597,  602,  608,  626,
  437, -121, -170,    0,    0,
};
final static int YYTABLESIZE=898;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         29,
    8,   29,   13,    7,   29,   16,   71,  156,  158,   67,
   86,   86,   90,   88,  169,    4,  201,   82,  197,  165,
  125,   92,   77,    7,  174,  101,  237,  238,   31,  115,
  116,    8,   91,   10,  113,   12,    8,   78,  182,   14,
    9,  123,   15,   31,    2,    9,  137,  202,  179,   81,
  176,  144,  101,  149,  150,  172,  105,   29,   29,  120,
  160,  175,  120,  126,  117,  118,   29,  186,  264,   79,
   24,   25,  212,  213,  215,  180,   80,  239,   24,   25,
  116,  181,  116,  101,   24,   25,  186,  177,  112,  190,
   95,   96,  161,   49,  187,    2,  192,  120,  194,   29,
  181,   45,    2,  143,   52,  183,   18,  221,    2,   19,
   20,   21,   22,  219,  117,  118,  117,  118,  191,  181,
   24,   25,   24,   25,  140,   66,  247,   23,  102,    2,
   46,   53,   54,   55,   56,  169,  290,   83,  106,  223,
  292,   72,   24,   25,   50,   95,   96,  245,   86,  300,
  228,  141,  265,  266,  267,  268,  270,  287,  209,  103,
  246,  104,  120,  291,  120,   84,  144,  224,   58,  120,
   51,  309,  310,  311,  312,  313,  314,   22,   22,   22,
   22,   22,   22,   27,   27,   22,   22,   22,   22,   22,
   22,   22,   22,   22,   22,   22,   22,   22,   22,   22,
   22,   22,   22,   22,   22,   22,    1,    2,   22,   22,
   22,   22,   29,   29,   27,   22,   27,  298,   22,   22,
   29,   47,    2,   73,   28,   28,   18,   69,   74,   19,
   20,   21,   22,   53,   54,   55,   56,   29,   29,   29,
   29,   29,   29,   29,  299,   29,  250,   23,  251,  252,
   48,  110,  253,   70,  203,   28,    8,   28,   57,    7,
   29,   99,   24,   25,  255,  205,  256,  257,   95,   96,
   58,  206,  303,  272,  273,    2,    2,  304,  305,   18,
  306,  307,   19,   20,   21,   22,  204,  100,   99,  127,
   98,   29,   29,   29,   29,   29,  316,   45,    2,   75,
   23,  159,   18,  284,   29,   19,   20,   21,   22,   85,
   53,   54,   55,   56,  100,   24,   25,   98,   93,   45,
    2,   24,   25,   23,   18,   76,  317,   19,   20,   21,
   22,   89,   53,   54,   55,   56,   95,   96,   24,   25,
  114,   45,    2,  139,   94,   23,   18,   58,  318,   19,
   20,   21,   22,  151,   53,   54,   55,   56,   97,   98,
   24,   25,  142,   45,    2,  166,  170,   23,   18,   58,
  319,   19,   20,   21,   22,  153,   53,   54,   55,   56,
   95,   96,   24,   25,  162,   45,    2,  108,    2,   23,
   18,   58,  320,   19,   20,   21,   22,  171,  163,  155,
   53,   54,   55,   56,   24,   25,  173,   38,   38,  152,
  154,   23,   38,   58,  329,   38,   38,   38,   38,  178,
   17,    2,  188,  107,  109,   18,   24,   25,   19,   20,
   21,   22,  124,   38,  273,    2,   38,   58,  189,   18,
  259,  260,   19,   20,   21,   22,   23,  195,   38,   38,
  198,  216,  147,   53,   54,   55,   56,  200,  321,    2,
   23,   24,   25,   18,  233,  220,   19,   20,   21,   22,
  234,  235,    9,    9,  210,   24,   25,    9,  207,  148,
    9,    9,    9,    9,   23,  100,  208,  217,   95,   96,
   58,  218,  157,   53,   54,   55,   56,  226,    9,   24,
   25,  211,   22,   22,   22,   22,   22,  227,  193,   53,
   54,   55,   56,    9,    9,   22,   22,   22,   22,   22,
  229,  230,   22,   22,   22,   22,   22,   22,  240,   22,
   58,  231,  242,   22,   22,   22,   22,  243,  244,  248,
   22,  125,  125,  125,  125,  125,   58,  214,   53,   54,
   55,   56,  263,  254,  125,  125,  125,  125,  125,  271,
  288,  125,  125,  125,  125,  125,  125,  289,  125,  222,
  293,  225,  125,  125,  125,  294,  222,  295,  296,  125,
  115,  115,  115,  115,  115,   58,  269,   53,   54,   55,
   56,  297,  302,  315,  115,  115,  308,  322,  323,  324,
  115,  115,  115,  115,  115,  115,  325,  115,  326,  327,
  328,  115,  115,  115,    1,    2,   82,  129,  115,  116,
  116,  116,  116,  116,   58,  138,   53,   54,   55,   56,
  241,  232,    0,  116,  116,    0,    0,  164,    0,  116,
  116,  116,  116,  116,  116,    0,  116,    0,    0,    0,
  116,  116,  116,    0,    0,    0,    0,  116,  118,  118,
  118,  118,  118,   58,    0,    0,  112,  112,  112,  112,
  112,    0,  118,  118,    0,    0,    0,    0,  118,  118,
  118,  118,  118,  118,    0,  118,    0,    0,    0,  118,
  118,  118,    0,  112,    0,    0,  118,  112,    0,  112,
    0,    0,    0,    0,  112,  114,  114,  114,  114,  114,
    0,    0,    0,  113,  113,  113,  113,  113,    0,  184,
   53,   54,   55,   56,  274,  274,  274,  274,  274,    0,
    0,    0,  114,    0,    0,    0,  114,    0,  114,    0,
  113,    0,    0,  114,  113,    0,  113,  274,    0,    0,
  185,  113,  100,   53,   54,   55,   56,   58,    0,    0,
    0,   53,   54,   55,   56,    0,   95,   96,   30,   30,
   30,   30,  130,  131,  132,  133,  134,  135,    0,   30,
    0,    0,    0,  167,    2,   30,   30,   99,   18,    0,
   58,   19,   20,  100,   22,    0,    0,    0,   58,    0,
   30,    0,  167,    2,    0,   30,    0,   18,    0,   23,
   19,   20,  168,   22,    2,    0,    0,    0,   18,    0,
    0,   19,   20,    0,   22,    0,    0,    0,   23,    0,
    0,  261,  275,  275,  275,  275,  275,    0,    0,   23,
  277,  282,  283,  285,  286,  278,  278,  278,  278,  278,
  279,  279,  279,  279,  279,  275,  280,  280,  280,  280,
  280,    0,    0,  301,    0,    0,    0,    0,  278,    0,
    0,    0,    0,  279,  281,  281,  281,  281,  281,  280,
  130,  131,  132,  133,  134,  135,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,  281,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         11,
    0,   13,    7,    0,   16,   10,   23,   97,   98,   21,
   50,   51,   52,   51,  111,    0,  256,   34,  140,  109,
   79,   57,  274,  284,  256,   65,  197,  198,  274,  256,
  257,  286,  282,  284,   74,  256,  286,  289,  257,  257,
  295,   77,  257,  289,  257,  295,   86,  287,  256,   34,
  256,   91,   92,   93,   94,  114,   68,   69,   70,   76,
  100,  293,   79,   80,  291,  292,   78,  256,  239,  282,
  297,  298,  162,  163,  164,  283,  289,  199,  297,  298,
  257,  289,  257,  123,  297,  298,  256,  293,   73,  256,
  270,  271,  104,  287,  283,  257,  136,  114,  138,  111,
  289,  256,  257,  283,  282,  122,  261,  284,  257,  264,
  265,  266,  267,  283,  291,  292,  291,  292,  285,  289,
  297,  298,  297,  298,  256,  287,  216,  282,  256,  257,
  285,  257,  258,  259,  260,  232,  256,  256,  287,  256,
  262,  284,  297,  298,  256,  270,  271,  256,  188,  271,
  188,  283,  242,  243,  244,  245,  246,  254,  283,  287,
  269,  289,  179,  283,  181,  284,  206,  284,  294,  186,
  282,  293,  294,  295,  296,  297,  298,  256,  257,  258,
  259,  260,  261,  256,  257,  264,  265,  266,  267,  268,
  269,  270,  271,  272,  273,  274,  275,  276,  277,  278,
  279,  280,  281,  282,  283,  284,  256,  257,  287,  288,
  289,  290,  256,  257,  287,  294,  289,  256,  297,  298,
  232,  256,  257,  290,  256,  257,  261,  256,  275,  264,
  265,  266,  267,  257,  258,  259,  260,  249,  250,  251,
  252,  253,  254,  287,  283,  289,  221,  282,  223,  224,
  285,  257,  227,  282,  256,  287,  256,  289,  282,  256,
  272,  256,  297,  298,  257,  283,  259,  260,  270,  271,
  294,  289,  277,  248,  256,  257,  257,  282,  283,  261,
  285,  286,  264,  265,  266,  267,  288,  256,  283,  261,
  256,  303,  304,  305,  306,  307,  301,  256,  257,  256,
  282,  287,  261,  285,  316,  264,  265,  266,  267,  256,
  257,  258,  259,  260,  283,  297,  298,  283,  256,  256,
  257,  297,  298,  282,  261,  282,  285,  264,  265,  266,
  267,  256,  257,  258,  259,  260,  270,  271,  297,  298,
  282,  256,  257,  283,  282,  282,  261,  294,  285,  264,
  265,  266,  267,  256,  257,  258,  259,  260,  272,  273,
  297,  298,  283,  256,  257,  283,  256,  282,  261,  294,
  285,  264,  265,  266,  267,  256,  257,  258,  259,  260,
  270,  271,  297,  298,  268,  256,  257,  256,  257,  282,
  261,  294,  285,  264,  265,  266,  267,  287,  268,  256,
  257,  258,  259,  260,  297,  298,  283,  256,  257,   95,
   96,  282,  261,  294,  285,  264,  265,  266,  267,  257,
  256,  257,  282,   69,   70,  261,  297,  298,  264,  265,
  266,  267,   78,  282,  256,  257,  285,  294,  285,  261,
  230,  231,  264,  265,  266,  267,  282,  284,  297,  298,
  256,  269,  256,  257,  258,  259,  260,  287,  256,  257,
  282,  297,  298,  261,  256,  284,  264,  265,  266,  267,
  262,  263,  256,  257,  256,  297,  298,  261,  287,  283,
  264,  265,  266,  267,  282,  289,  287,  287,  270,  271,
  294,  287,  256,  257,  258,  259,  260,  257,  282,  297,
  298,  283,  256,  257,  258,  259,  260,  284,  256,  257,
  258,  259,  260,  297,  298,  269,  270,  271,  272,  273,
  282,  282,  276,  277,  278,  279,  280,  281,  257,  283,
  294,  282,  269,  287,  288,  289,  290,  269,  269,  284,
  294,  256,  257,  258,  259,  260,  294,  256,  257,  258,
  259,  260,  287,  283,  269,  270,  271,  272,  273,  283,
  283,  276,  277,  278,  279,  280,  281,  283,  283,  179,
  283,  181,  287,  288,  289,  283,  186,  283,  283,  294,
  256,  257,  258,  259,  260,  294,  256,  257,  258,  259,
  260,  283,  287,  287,  270,  271,  263,  287,  287,  287,
  276,  277,  278,  279,  280,  281,  287,  283,  287,  287,
  287,  287,  288,  289,    0,    0,  284,   84,  294,  256,
  257,  258,  259,  260,  294,   86,  257,  258,  259,  260,
  206,  195,   -1,  270,  271,   -1,   -1,  268,   -1,  276,
  277,  278,  279,  280,  281,   -1,  283,   -1,   -1,   -1,
  287,  288,  289,   -1,   -1,   -1,   -1,  294,  256,  257,
  258,  259,  260,  294,   -1,   -1,  256,  257,  258,  259,
  260,   -1,  270,  271,   -1,   -1,   -1,   -1,  276,  277,
  278,  279,  280,  281,   -1,  283,   -1,   -1,   -1,  287,
  288,  289,   -1,  283,   -1,   -1,  294,  287,   -1,  289,
   -1,   -1,   -1,   -1,  294,  256,  257,  258,  259,  260,
   -1,   -1,   -1,  256,  257,  258,  259,  260,   -1,  256,
  257,  258,  259,  260,  249,  250,  251,  252,  253,   -1,
   -1,   -1,  283,   -1,   -1,   -1,  287,   -1,  289,   -1,
  283,   -1,   -1,  294,  287,   -1,  289,  272,   -1,   -1,
  287,  294,  289,  257,  258,  259,  260,  294,   -1,   -1,
   -1,  257,  258,  259,  260,   -1,  270,  271,  257,  258,
  259,  260,  276,  277,  278,  279,  280,  281,   -1,  268,
   -1,   -1,   -1,  256,  257,  274,  275,  283,  261,   -1,
  294,  264,  265,  289,  267,   -1,   -1,   -1,  294,   -1,
  289,   -1,  256,  257,   -1,  294,   -1,  261,   -1,  282,
  264,  265,  285,  267,  257,   -1,   -1,   -1,  261,   -1,
   -1,  264,  265,   -1,  267,   -1,   -1,   -1,  282,   -1,
   -1,  285,  249,  250,  251,  252,  253,   -1,   -1,  282,
  249,  250,  251,  252,  253,  249,  250,  251,  252,  253,
  249,  250,  251,  252,  253,  272,  249,  250,  251,  252,
  253,   -1,   -1,  272,   -1,   -1,   -1,   -1,  272,   -1,
   -1,   -1,   -1,  272,  249,  250,  251,  252,  253,  272,
  276,  277,  278,  279,  280,  281,   -1,   -1,   -1,   -1,
   -1,   -1,   -1,   -1,   -1,   -1,   -1,  272,
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
"identificador : ID ERROR ID",
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
"sem_pasaje : CV LE_KW",
"sem_pasaje : CR LE_KW",
"sem_pasaje : CV error",
"sem_pasaje : CR error",
"sentencia_return : RETURN LPAREN lista_expresiones RPAREN SEMICOLON",
"sentencia_return : RETURN lista_expresiones RPAREN SEMICOLON",
"sentencia_return : RETURN LPAREN lista_expresiones error SEMICOLON",
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
"sentencia_for : FOR LPAREN identificador_destino factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON",
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

//#line 1652 "gramatic.y"

/* ======= Código Java adicional (opcional) ======= */
//#line 850 "Parser.java"
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
//#line 237 "gramatic.y"
{ 
            addError("Contenido inesperado después del final del programa. ¿Una '}' extra?");
        }
break;
case 4:
//#line 245 "gramatic.y"
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
//#line 261 "gramatic.y"
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
//#line 275 "gramatic.y"
{
            addError("Error Sintactico: Falta nombre de programa.");
        }
break;
case 7:
//#line 279 "gramatic.y"
{
            addError("Error Sintactico: Falta delimitador '}' de cierre del programa.");
        }
break;
case 8:
//#line 283 "gramatic.y"
{
            addError("Error Sintactico: Falta delimitador '{' de apertura del programa.");
        }
break;
case 10:
//#line 293 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 11:
//#line 295 "gramatic.y"
{ errorEnProduccion = false; }
break;
case 12:
//#line 297 "gramatic.y"
{ 
            yyerror("Error sintáctico en sentencia. Recuperando en ';'."); 
            errorEnProduccion = false; /* REINICIAR*/
        }
break;
case 16:
//#line 308 "gramatic.y"
{ 
            addError("Error en cuerpo de función. Recuperando en ';'."); 
        }
break;
case 19:
//#line 320 "gramatic.y"
{   ArrayList<SymbolEntry> entries = (ArrayList<SymbolEntry>)val_peek(1).obj;
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
break;
case 20:
//#line 342 "gramatic.y"
{ 
            addError("Error Semantico: Falta lista de variables a continuación de var.");
        }
break;
case 21:
//#line 346 "gramatic.y"
{ 
            addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la declaración de variables.");
            listaVariablesError = false; 
        }
break;
case 22:
//#line 353 "gramatic.y"
{ 
          yyval.entry = val_peek(0).entry; 
      }
break;
case 23:
//#line 357 "gramatic.y"
{ 
          /* Reportamos el error personalizado */
          yyerror("Error Lexico: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter '_' no está permitido en los identificadores.", true);
          
          /* RECUPERACIÓN: Asumimos que el usuario quería usar el primer ID para seguir compilando */
          yyval.entry = val_peek(2).entry; 
      }
break;
case 24:
//#line 364 "gramatic.y"
{
            yyerror("Error Lexico: Identificador inválido '" + val_peek(2).entry.getLexeme() + "_" + val_peek(0).entry.getLexeme() + "'. El caracter $2.getLexeme() no está permitido en los identificadores.", true);
    }
break;
case 25:
//#line 370 "gramatic.y"
{    
        yyval.entry = val_peek(0).entry;
    }
break;
case 26:
//#line 373 "gramatic.y"
{
        /* Caso prefijado: MAIN.A*/
        SymbolEntry scopeID = (SymbolEntry)val_peek(2).entry;
        SymbolEntry varID = (SymbolEntry)val_peek(0).entry;
        
        /* Guardamos "MAIN" dentro de "A" para usarlo después en el lookup*/
        varID.setScopePrefix(scopeID.getLexeme());
        
        yyval.entry = varID;
    }
break;
case 27:
//#line 386 "gramatic.y"
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
case 28:
//#line 399 "gramatic.y"
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
case 29:
//#line 413 "gramatic.y"
{ 
            addError("Error Sintáctico: Falta ',' en la declaración de variables.");
            listaVariablesError = true; 
            errorEnProduccion = true; /* Activar*/
        }
break;
case 30:
//#line 420 "gramatic.y"
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
case 31:
//#line 462 "gramatic.y"
{
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)val_peek(0).entry);
            yyval.obj = list;
        }
break;
case 32:
//#line 467 "gramatic.y"
{            
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add(val_peek(0).entry);
            yyval.obj = lista; }
break;
case 33:
//#line 479 "gramatic.y"
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
//#line 512 "gramatic.y"
{
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
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
break;
case 35:
//#line 530 "gramatic.y"
{ 
            addError("Error Sintactico: Falta nombre de función.");
        }
break;
case 36:
//#line 534 "gramatic.y"
{ 
            addError("Error Semantico: Se tiene que tener mínimo un parámetro formal.");
        }
break;
case 37:
//#line 538 "gramatic.y"
{ 
            addError("Error Sintactico: Falta '{' de apertura de función.");
        }
break;
case 38:
//#line 542 "gramatic.y"
{ 
            addError("Error Sintactico: Falta '}' de cierre de función.");
        }
break;
case 39:
//#line 546 "gramatic.y"
{ 
            SymbolEntry se = (SymbolEntry)val_peek(8).entry;
            listaTiposError = false; 
            addError("Error Sintactico: Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
break;
case 40:
//#line 552 "gramatic.y"
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
case 41:
//#line 568 "gramatic.y"
{
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 42:
//#line 574 "gramatic.y"
{
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)val_peek(2).obj;
            lista.add((SymbolEntry)val_peek(0).obj);
            yyval.obj = lista;
        }
break;
case 43:
//#line 581 "gramatic.y"
{
           listaTiposError = true;
           errorEnProduccion = true; 
        }
break;
case 44:
//#line 588 "gramatic.y"
{yyval.obj = new SymbolEntry("int");}
break;
case 45:
//#line 589 "gramatic.y"
{yyval.obj = new SymbolEntry("float");}
break;
case 46:
//#line 594 "gramatic.y"
{yyval.obj=val_peek(0).entry;}
break;
case 47:
//#line 596 "gramatic.y"
{
            addError("Falta ',' entre parametros.");
        }
break;
case 49:
//#line 604 "gramatic.y"
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
case 50:
//#line 629 "gramatic.y"
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
case 51:
//#line 652 "gramatic.y"
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
case 52:
//#line 668 "gramatic.y"
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
case 53:
//#line 685 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 54:
//#line 689 "gramatic.y"
{ 
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 55:
//#line 694 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cv'.");
        yyval.semantica = new String[]{"cv", "le"}; 
    }
break;
case 56:
//#line 699 "gramatic.y"
{ 
        addError("Falta la directiva 'le' después de 'cr'.");
        yyval.semantica = new String[]{"cr", "le"}; 
    }
break;
case 57:
//#line 706 "gramatic.y"
{
            ArrayList<PolacaElement> retornosReales = (ArrayList<PolacaElement>)val_peek(2).obj;
            
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
break;
case 58:
//#line 750 "gramatic.y"
{
        addError("Error Sintactico: Falta de '(' en return.");
    }
break;
case 59:
//#line 753 "gramatic.y"
{
        addError("Error Sintactico: Falta de ')' en return.");
    }
break;
case 71:
//#line 777 "gramatic.y"
{ 
            PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
            PI().generatePrint(expr);
            
            if (!errorEnProduccion) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Print detectado");
            }
       }
break;
case 72:
//#line 786 "gramatic.y"
{
            addError("Error Semantico: Falta argumento en print.");
       }
break;
case 73:
//#line 789 "gramatic.y"
{ 
        addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final del PRINT (en función)."); 
    }
break;
case 76:
//#line 799 "gramatic.y"
{ 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
break;
case 78:
//#line 812 "gramatic.y"
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
case 79:
//#line 824 "gramatic.y"
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
case 80:
//#line 837 "gramatic.y"
{

          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado.");
      }
break;
case 81:
//#line 848 "gramatic.y"
{ 
          addError("Error Semantico: Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
break;
case 82:
//#line 855 "gramatic.y"
{
          
          PolacaElement cond = (PolacaElement)val_peek(1).Polacaelement;
          pilaSaltosBF.push(cond.getFalseList());
      }
break;
case 84:
//#line 865 "gramatic.y"
{ addError("Error Sintactico: Falta paréntesis de apertura '(' en IF."); }
break;
case 85:
//#line 867 "gramatic.y"
{ addError("Error Sintactico: Falta paréntesis de cierre ')' en condición."); }
break;
case 86:
//#line 869 "gramatic.y"
{ addError("Error Semantico: Error en el cuerpo de la cláusula then."); }
break;
case 87:
//#line 874 "gramatic.y"
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
case 88:
//#line 889 "gramatic.y"
{
        SymbolEntry id = (SymbolEntry)val_peek(5).entry;
        PolacaElement cte1 = val_peek(3).Polacaelement; 
        PolacaElement cte2 = val_peek(1).Polacaelement;
        
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
break;
case 89:
//#line 952 "gramatic.y"
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
case 90:
//#line 979 "gramatic.y"
{ 
            addError("Error Sintactico: Falta parentesis de apertura en encabezado del for.");
        }
break;
case 91:
//#line 983 "gramatic.y"
{
            addError("Error Sintactico:: Falta nombre de variable en for.");
        }
break;
case 92:
//#line 987 "gramatic.y"
{ 
            addError("Error  Sintactico: Falta palabra clave 'from' en encabezado del for.");
        }
break;
case 93:
//#line 991 "gramatic.y"
{ 
            addError("Error Sintactico: Falta constante inicial en encabezado del for.");
        }
break;
case 94:
//#line 995 "gramatic.y"
{ 
            addError("Error Sintactico: Falta palabra clave 'to' en encabezado del for.");
        }
break;
case 95:
//#line 999 "gramatic.y"
{ 
            addError("Error Sintactico: Falta constante final en encabezado del for.");
        }
break;
case 96:
//#line 1003 "gramatic.y"
{ 
            addError("Error Sintactico: Falta parentesis de cierre en encabezado for.");
        }
break;
case 97:
//#line 1007 "gramatic.y"
{ 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
break;
case 98:
//#line 1014 "gramatic.y"
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
case 99:
//#line 1029 "gramatic.y"
{ 
            addError("Error Sintactico: Falta de comparador en comparación.");
        }
break;
case 100:
//#line 1033 "gramatic.y"
{ 
            addError("Error Sintactico: Falta operando izquierdo en comparación.");
        }
break;
case 101:
//#line 1037 "gramatic.y"
{ 
            addError("Error Sintactico: Falta operando derecho en comparación.");
        }
break;
case 102:
//#line 1043 "gramatic.y"
{ yyval.sval = "=="; }
break;
case 103:
//#line 1044 "gramatic.y"
{ yyval.sval = "=!"; }
break;
case 104:
//#line 1045 "gramatic.y"
{ yyval.sval = "<";  }
break;
case 105:
//#line 1046 "gramatic.y"
{ yyval.sval = "<="; }
break;
case 106:
//#line 1047 "gramatic.y"
{ yyval.sval = ">";  }
break;
case 107:
//#line 1048 "gramatic.y"
{ yyval.sval = ">="; }
break;
case 108:
//#line 1053 "gramatic.y"
{
            SymbolEntry destino = (SymbolEntry)val_peek(3).entry;
            PolacaElement fuente = (PolacaElement)val_peek(1).Polacaelement;
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
break;
case 109:
//#line 1082 "gramatic.y"
{    
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)val_peek(1).obj;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)val_peek(3).obj;

        if(listaVariablesError) {
            addError("Error Sintactico: Falta ',' en la lista de variables de la asignación.");
        } 
        if(listaExpresionesError) {
            addError("Error Sintactico: Falta ',' en la lista de expresiones de la asignación.");
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
                                yyerror("Error Semantico: Error de Tipos en variable " + (i+1) + " ('" + destino.getLexeme() + 
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
                    yyerror("Error Semantico: número de variables (" + listaDestinos.size() + 
                            ") y expresiones (" + listaFuentes.size() + ") no coinciden en la asignacion multiple.", true);
                } else {
                    for (int i = 0; i < listaDestinos.size(); i++) {
                        SymbolEntry destino = listaDestinos.get(i);
                        PolacaElement fuente = listaFuentes.get(i);
                        
                        if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                            yyerror("Error Semantico: El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                        } else if(destino.getUso().equals("variable")){
                            if (destino.getTipo().equals("untype")){
                                destino.setTipo("int"); 
                            }
                        }
                        if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                            yyerror("Error Semantico: Tipos incompatibles, no se puede asignar '" + fuente.getResultType() 
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
case 110:
//#line 1199 "gramatic.y"
{ 
            addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la asignación.");
        }
break;
case 111:
//#line 1203 "gramatic.y"
{ 
                addError("Error Sintáctico: Falta punto y coma ';' al final de la asignación múltiple.");
            }
break;
case 112:
//#line 1210 "gramatic.y"
{List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)val_peek(0).Polacaelement);
            yyval.obj = list;
        }
break;
case 113:
//#line 1214 "gramatic.y"
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
case 114:
//#line 1224 "gramatic.y"
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
case 115:
//#line 1241 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 116:
//#line 1243 "gramatic.y"
{
            PolacaElement elem1 = val_peek(2).Polacaelement;
            PolacaElement elem2 = val_peek(0).Polacaelement;
            
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Error Semantico: Tipos incompatibles para la suma " + "elem1" + ": " + elem1.getResultType() + "elem2" + ": " + elem2.getResultType(), true);
                yyval.Polacaelement = PI().generateErrorElement("error"); 
            } else {
                yyval.Polacaelement = PI().generateOperation(elem1, elem2, "+", tipoResultante);
            }
        }
break;
case 117:
//#line 1256 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 118:
//#line 1260 "gramatic.y"
{
            PolacaElement elem1 = val_peek(2).Polacaelement;
            PolacaElement elem2 = val_peek(0).Polacaelement;
            
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Error Semantico: Tipos incompatibles para la resta " + "elem1" + ": " + elem1.getResultType() + "elem2" + ": " + elem2.getResultType(), true);
                yyval.Polacaelement = PI().generateErrorElement("error"); 
            } else {
                yyval.Polacaelement = PI().generateOperation(elem1, elem2, "-", tipoResultante);
            }
        }
break;
case 119:
//#line 1273 "gramatic.y"
{ 
            addError("Falta de operando en expresión.");
        }
break;
case 120:
//#line 1280 "gramatic.y"
{
        yyval.Polacaelement = val_peek(0).Polacaelement;
    }
break;
case 121:
//#line 1283 "gramatic.y"
{
        PolacaElement term = val_peek(2).Polacaelement;
        PolacaElement fact = val_peek(0).Polacaelement;
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Error Semantico: Tipos incompatibles para la multiplicación " + "elem1" + ": " + term.getResultType() + "elem2" + ": " + fact.getResultType(), true);
            yyval.Polacaelement = PI().generateErrorElement("error"); 
        } else {
            yyval.Polacaelement = PI().generateOperation(term, fact, "*", tipoResultante);
        }
    }
break;
case 122:
//#line 1295 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 123:
//#line 1299 "gramatic.y"
{
        PolacaElement term = val_peek(2).Polacaelement;
        PolacaElement fact = val_peek(0).Polacaelement;
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Error Semantico: Tipos incompatibles para la división  " + "elem1" + ": " + term.getResultType() + "elem2" + ": " + fact.getResultType(), true);
            yyval.Polacaelement = PI().generateErrorElement("error"); 
        } else {
            yyval.Polacaelement = PI().generateOperation(term, fact, "/", tipoResultante);
        }
    }
break;
case 124:
//#line 1311 "gramatic.y"
{ 
            addError("Falta de operando en término.");
        }
break;
case 125:
//#line 1318 "gramatic.y"
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
break;
case 126:
//#line 1349 "gramatic.y"
{
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 127:
//#line 1356 "gramatic.y"
{ 
            /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
            SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
            symbolTable.add(se_const);
            yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
        }
break;
case 128:
//#line 1363 "gramatic.y"
{ 
          /* CORRECCIÓN: Capturar y añadir la constante a la TS*/
          SymbolEntry se_const = (SymbolEntry)val_peek(0).entry;
          symbolTable.add(se_const);
          yyval.Polacaelement = PI().generateOperand(val_peek(0).entry); 
      }
break;
case 129:
//#line 1370 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 130:
//#line 1372 "gramatic.y"
{ yyval.Polacaelement = val_peek(0).Polacaelement; }
break;
case 131:
//#line 1378 "gramatic.y"
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
case 132:
//#line 1474 "gramatic.y"
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
case 133:
//#line 1488 "gramatic.y"
{ 
            addError("Error Sintactico: Falta el '(' en la conversión explícita.");
        }
break;
case 134:
//#line 1492 "gramatic.y"
{ 
            addError("Error Sintactico: Falta el ')' en la conversión explícita.");
        }
break;
case 135:
//#line 1499 "gramatic.y"
{
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 136:
//#line 1506 "gramatic.y"
{
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)val_peek(2).listParamInv;
        lista.add(val_peek(0).paramInv);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
break;
case 137:
//#line 1517 "gramatic.y"
{
        PolacaElement expr = (PolacaElement)val_peek(2).Polacaelement;
        SymbolEntry idParam = (SymbolEntry)val_peek(0).entry;
        
        yyval.paramInv = new ParametroInvocacion(idParam.getLexeme(), expr);
        
        if (!errorEnProduccion) { 
            System.out.println("   -> Parametro nombrado detectado: " + idParam.getLexeme());
        }
    }
break;
case 138:
//#line 1528 "gramatic.y"
{ 
        addError("Error Sintactico: declaracion incorrecta del parámetro real. Se espera 'valor -> nombre'.");
        yyval.paramInv = new ParametroInvocacion("error", (PolacaElement)val_peek(1).Polacaelement); /* Dummy para no romper todo*/
    }
break;
case 139:
//#line 1536 "gramatic.y"
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
case 140:
//#line 1559 "gramatic.y"
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
case 141:
//#line 1611 "gramatic.y"
{ 
        addError("Error Sintactico: Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 142:
//#line 1620 "gramatic.y"
{ 
        addError("Error Sintactico: Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 143:
//#line 1630 "gramatic.y"
{ 
        addError("Error Sintactico: Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        yyval.Polacaelement = new PolacaElement("error");
    }
break;
case 144:
//#line 1639 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 145:
//#line 1643 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
case 146:
//#line 1647 "gramatic.y"
{
        yyval.Polacaelement = PI().generateOperand(val_peek(0).entry);
    }
break;
//#line 2512 "Parser.java"
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
