package lexer;

import lexer.semanticActions.*;
public class SemanticMatrix {

     AS1 as1 = new AS1();
     AS2 as2 = new AS2();
     AS3 as3 = new AS3();
     AS4 as4 = new AS4();
     AS5 as5 = new AS5();
     AS6 as6 = new AS6();
     AS7 as7 = new AS7();
     AS8 as8 = new AS8();
     AS9 as9 = new AS9();
     AS10 as10 = new AS10();


public final SemanticAction[][] TRANSITIONS = {
//      L   D   _   "   .   WS  OP  -  SEP  :   =  COMP  %   #  OTH   !   +   I   F
/*0*/ {as1,as1,as2,as1,as1,as9,as2,as1,as2,as1,as1,as1, as7,as1,as7,as7,as2,as1,as1},
/*1*/ {as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as2,as7,as7,as7,as7,as7,as7,as7,as7},
/*2*/ {as10,as10,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as10,as10},
/*3*/ {as10,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as2,as10,as10},
/*4*/ {as10,as1,as2,as2,as2,as2,as2,as1,as2,as2,as2,as2,as2,as2,as2,as2,as2,as10,as10},
/*5*/ {as7,as1,as7,as7,as1,as7,as7,as10,as7,as7,as7,as7,as7,as7,as7,as7,as7,as3,as7},
/*6*/ {as1,as1,as5,as5,as5,as5,as5,as5,as5,as5,as5,as5,as1,as5,as7,as5,as5,as1,as1},
/*7*/ {as10,as1,as10,as10,as10,as10,as10,as10,as10,as10,as10,as10,as10,as10,as10,as10,as2,as10,as1},
/*8*/ {as7,as7,as7,as7,as7,as7,as7,as1,as7,as7,as7,as7,as7,as7,as7,as7,as1,as7,as7},
/*9*/ {as7,as1,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7},
/*10*/{as1,as1,as1,as8,as1,as6,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1},
/*11*/{as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as7,as1,as7,as7,as7,as7,as7},
/*12*/{as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1},
/*13*/{as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as1,as9,as1,as1,as1,as1,as1},
/*14*/{as4,as1,as4,as4,as7,as4,as4,as4,as4,as4,as4,as4,as1,as4,as1,as4,as4,as4,as1},
};
}