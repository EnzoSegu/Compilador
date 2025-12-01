package lexer;

import java.util.Map;

public enum TokenType {
    // Palabras reservadas (minúsculas obligatorias)
    IF, ELSE, ENDIF, PRINT, RETURN,
    VAR, INT_KW, FLOAT_KW, FOR, FROM, TO,
    CV,CR,LE_KW,TOF,


    // Identificadores
    ID,

    // Constantes
    INT16,        // Entero de 16 bits con sufijo I
    FLOAT32,      // Flotante de 32 bits con posible exponente F
    STRING,       // Cadena entre comillas

    // Operadores aritméticos
    PLUS, MINUS, STAR, SLASH,

    // Asignación
    ASSIGN, ASSIGN_COLON,

    // Comparadores
    EQ, NEQ, LT, LE_OP, GT, GE,

    // Otros símbolos
    LPAREN, RPAREN, LBRACE, RBRACE,
    UNDERSCORE, SEMICOLON, ARROW, COMMA, POINT ,

    // Especiales
    EOF, ERROR;

    public static final Map<String, TokenType> RESERVED_WORDS = Map.ofEntries(
        Map.entry("if", IF),
        Map.entry("else", ELSE),
        Map.entry("endif", ENDIF),
        Map.entry("print", PRINT),
        Map.entry("return", RETURN),
        Map.entry("var", VAR),
        Map.entry("for", FOR),
        Map.entry("from", FROM),
        Map.entry("to", TO),
        Map.entry("cv", CV),
        Map.entry("cr", CR),
        Map.entry("le", LE_KW),
        Map.entry("tof", TOF),
        Map.entry("int", INT_KW),
        Map.entry("float", FLOAT_KW)
    );

    public static TokenType fromLexeme(String lexeme) {
        switch (lexeme) {
            case "+": return PLUS;
            case "-": return MINUS;
            case "*": return STAR;
            case "/": return SLASH;
            case "(": return LPAREN;
            case ")": return RPAREN;
            case "{": return LBRACE;
            case "}": return RBRACE;
            case "=": return ASSIGN;
            case ":=": return ASSIGN_COLON;
            case "==": return EQ;
            case "=!": return NEQ;
            case "<": return LT;
            case "<=": return LE_OP;
            case ">": return GT;
            case ">=": return GE;
            case ";": return SEMICOLON;
            case "->": return ARROW;
            case "_": return UNDERSCORE;
            case ",": return COMMA;
            case ".": return POINT;
            default: return null;
        }
    }

}
