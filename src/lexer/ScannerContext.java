package lexer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ScannerContext {
    private StringBuilder lexeme = new StringBuilder();
    private int line = 1;
    private Token token;
    private final List<String> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private boolean debug = true;
    
    public String getLexeme() {
        return lexeme.toString();
    }

    public void append(char c) {
        lexeme.append(c);
    }

    public void clear() {
        lexeme.setLength(0);
    }

    public int getLine() {
        return line;
    }

    public void incrementLine() {
        line++;
    }
    
    public void setToken(TokenType type) {
        this.token = new Token(type, getLexeme().isEmpty() ? "" : getLexeme());
        tokens.add("Linea "+ line + ": " + type + "-> "+lexeme);
    }

    public void setToken(TokenType type, String lexeme) {
        this.token = new Token(type, lexeme);
    }
    public void setToken(TokenType type, String lexeme, int line) {
        this.token = new Token(type, lexeme, line);
    }
    // AÑADIR ESTE
    public void setToken(TokenType type, SymbolEntry entry) {
        this.token = new Token(type, entry);
        tokens.add("Linea "+ line + ": " + type + "-> "+ entry.getLexeme());
}

    public Token getToken() {
        return token;
    }
    public void clearToken() {
        this.token = null;
    }

    public void addError(String message) {
        errors.add("Linea "+ line + ": " + message);
    }

    public void dumpTokens(File out) throws IOException {
        try (PrintWriter pw = new PrintWriter(out)) {
            for (String s : tokens) pw.println(s);
        }
    }

    public void dumpErrors(File out) throws IOException {
        try (PrintWriter pw = new PrintWriter(out)) {
            for (String s : errors) pw.println(s);
        }
    }

    public List<String> getTokensDetectados() { return tokens; }
    public List<String> getErrores() { return errors; }
    public void setDebug(boolean debug) { this.debug = debug; }
    public boolean isDebug() { return debug; }

}

