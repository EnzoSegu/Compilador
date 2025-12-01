package lexer;

import java.io.IOException;
import java.io.Reader;

import lexer.semanticActions.*;

public class Scanner {

    private Reader reader;
    private ScannerContext context=new ScannerContext();
    private int currentChar;
    private static Automaton automaton = new Automaton();
    private static SemanticMatrix semanticMatrix = new SemanticMatrix();

    public Scanner(Reader reader) throws IOException {
        this.reader = reader;
        this.currentChar = reader.read();
    }

    

    public ScannerContext getContext() {
        return context;
    }



    private int charToCol(int c) {
        if (c == -1)
            return 14; // EOF
        if (c == 'I')
            return 17;
        if (c == 'F')
            return 18;
        if (Character.isLetter(c))
            return 0; // LETTER
        if (Character.isDigit(c))
            return 1; // DIGIT
        if (c == '_')
            return 2;
        if (c == '"')
            return 3;
        if (c == '.')
            return 4;
        if (Character.isWhitespace(c))
            return 5;
        if (c == '-')
            return 7;
        if (c == '+')
            return 16;
        if ("+-*/".indexOf(c) >= 0)
            return 6;
        if ("(){},;".indexOf(c) >= 0)
            return 8;
        if (c == ':')
            return 9;
        if (c == '=')
            return 10;
        if ("<>".indexOf(c) >= 0)
            return 11;
        if (c == '%')
            return 12;
        if (c == '#')
            return 13;
        if (c == '!')
            return 15;
        return 14; // OTHER
    }

    public Token nextToken() throws IOException {
        int state = 0;

        while (currentChar != -1) {
            int col = charToCol(currentChar);
            int nextState = Scanner.automaton.TRANSITIONS[state][col];
            SemanticAction action = Scanner.semanticMatrix.TRANSITIONS[state][col];
            if((char)currentChar == '\n'){
                context.incrementLine();
            }

            action.execute(context, currentChar);

            if (context.getToken() != null && context.isDebug() == true ) {
            Token tok = context.getToken();
            context.clearToken();// limpiar token guardado
            currentChar = reader.read(); // leer siguiente caracter
            return tok;
            }
            else if (context.getToken() != null && context.isDebug()!= true) {
                Token tok = context.getToken();
                context.clearToken();// limpiar token guardado
                context.setDebug(true);
                return tok;
                }

            state = nextState;
            currentChar = reader.read();   
        }
        return new Token(TokenType.EOF, "EOF");
    }

}
