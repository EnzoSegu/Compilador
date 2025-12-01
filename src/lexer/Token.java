package lexer;

public class Token {
    private TokenType type;
    private String lexeme;
    private SymbolEntry entry;

    public Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
        this.entry = null;
    }

    public Token(TokenType type) {
        this.type = type;
        this.lexeme = "";
        this.entry = null;
    }

    public Token(TokenType type, SymbolEntry symbolEntry) {
        this.type = type;
        this.entry = symbolEntry;
        this.lexeme = symbolEntry.getLexeme();
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public SymbolEntry getEntry() {
        return entry;
    }

    @Override
    public String toString() {
        if (TokenType.RESERVED_WORDS.containsValue(type)) {
            return "Palabra reservada " + lexeme;
        }

        switch (type) {
            case ID:
                return "Identificador " + lexeme;
            case STRING:
                return "Cadena " + lexeme;
            case INT16:
                return "Constante entera " + lexeme;
            case FLOAT32:
                return "Constante flotante " + lexeme;
            case ERROR:
                return "Error: " + lexeme;
            case EOF:
                return "EOF";
        }

        if (TokenType.fromLexeme(lexeme) != null) {
            return lexeme;
        }

        return type + " " + lexeme;
    }

    public static TokenType fromLexeme(String lexeme) {
        TokenType type = TokenType.fromLexeme(lexeme);
        return type != null ? type : TokenType.ERROR;
    }
}
