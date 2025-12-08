package lexer;

public class Automaton {

    public static final int LETTER      = 0; // a-z A-Z
    public static final int DIGIT       = 1; // 0-9
    public static final int UNDERSCORE  = 2; // _
    public static final int QUOTE       = 3; // ""
    public static final int DOT         = 4; // .
    public static final int WHITESPACE  = 5; // space, tab, newline
    public static final int OPERATOR    = 6; // + * / -
    public static final int MINUN       = 7; // -
    public static final int SEPARATOR   = 8; // ( ) { } ; , 
    public static final int DOUBLEPOINT = 9; // :
    public static final int EQUALS      = 10; // =
    public static final int COMPARATOR  = 11; // < > 
    public static final int PORCENTAGE  = 12; // %
    public static final int NUMERAL     = 13;// #
    public static final int OTHER       = 14;
    public static final int EXCLAMATION = 15; // ! 
    public static final int PLUS        = 16; // +
    public static final int I           = 17; // integer
    public static final int F           = 18; // float



public final int[][] TRANSITIONS = {
//      L   D   _   "   .  WS  OP  -  SEP  :   =  COMP  %   #  OTH   !   +   I    F
/*0*/ { 6,  5, -1, 10,  7,  0, -1, 4, -1,  1,  3,  2,  0,  11,   0 ,  0, -1,  6,  6},
/*1*/ {0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  -1,  0,  0,  0,   0,   0,  0,  0,  0},
/*2*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  -1, -1,  -1, -1, -1, -1, -1},
/*3*/ {-1, -1,  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  -1, -1,  -1, -1, -1, -1, -1},
/*4*/ {-1,  5,  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  1, -1,  -1, -1, -1, -1, -1},
/*5*/ { 0,  5,  0,  0,  7,  0,  0,  -1,  0,  0,  0,  0,  0,  0,   0,  0,  0, -1,  0},
/*6*/ { 6,  6,  -1, -1 , -1, -1, -1, -1, -1,  -1,  -1, -1, 6, -1,  -1,  -1, -1, 6, 6},
/*7*/ { -1, 14, -1, -1, -1, -1, -1,-1, -1, -1, -1,  -1, -1, -1,  -1,  -1,  9,  -1,  8},
/*8*/ { 0, 0 , 0 ,  0, 0 , 0 , 0 ,  9, 0 , 0 , 0 ,  0, 0 ,  0 ,  0,  0,  9,  0,  0},
/*9*/ { 0, 14,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,   0,  0,  0,  0,  0},
/*10*/ {10, 10, 10, -1, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,  10, 10, 10, 10, 10},
/*11*/{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  12,   0,  0,  0,  0,  0},
/*12*/ {12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 13,  12, 12, 12, 12, 12},
/*13*/{ 12,  12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 0,   12, 12, 12, 12, 12},
/*14*/ {-1, 14, -1,  -1, 0, -1, -1, -1, -1,  -1,  -1,  -1,  0, -1, 0,  -1,  14,  -1, 8},
};
}