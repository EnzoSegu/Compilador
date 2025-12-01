package lexer;

import java.util.ArrayList;
import java.util.List;

public class SymbolEntry {
    
    private final String lexeme;   
    private String tipo; // "int", "float", "undefined", "string", etc.
    private String uso;  // "variable", "funcion", "parametro", "programa", "constante"
    private Object value;  // Para constantes

    // --- Atributos para Name Mangling y Ámbitos ---
    private String mangledName; // Para el "name mangling" (ej: A:MAIN:AA)
    private SymbolEntry funcionPadre; // Para saber en qué función estoy (lo usaremos luego)

    // --- Atributos para Funciones (Tema Funciones) ---
    private String tipoRetorno; // "int", "float", "void"
    private List<SymbolEntry> parametros; // Lista de parámetros formales
    private List<String> tiposRetorno;
    private SymbolEntry funcionOrigen = null;

    private String mecanismoPasaje; // "cv" (Copia-Valor), "cr" (Copia-Resultado), "cvr" (Defecto)
    private String modoParametro;   // "le" (Lectura-Escritura)
    
    private String scopePrefix = null;
    /**
     * Constructor para IDs (el tipo se sabrá después)
     */
    public SymbolEntry(String lexeme) {
        this.lexeme = lexeme;
        this.tipo = null; 
        this.uso = null;
        this.parametros = new ArrayList<>();
        this.tiposRetorno = new ArrayList<>();
    }

    /**
     * Constructor para Constantes (usado por el Léxico)
     */
    public SymbolEntry(String lexeme, String tipo, Object value) {
        this.lexeme = lexeme;
        this.tipo = tipo;
        this.value = value;
        this.uso = "constante";
        this.parametros = new ArrayList<>();
        this.tiposRetorno = new ArrayList<>();
        this.mecanismoPasaje = "cvr"; // "semántica por defecto: copia-valor-resultado"
        this.modoParametro = "le";    // Por defecto asumimos LE
        this.mangledName = "CTE_" + lexeme.replace(".", "_").replace(":", "_").replace("\"", "");
    }

    // --- Getters y Setters ---

    public String getLexeme() { return lexeme; }
    public void setLexeme(String lexeme) { /* No se usa, el lexema es final */ }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public String getUso() { return uso; }
    public void setUso(String uso) { this.uso = uso; }
    
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public String getMangledName() { return mangledName; }
    public void setMangledName(String mangledName) { this.mangledName = mangledName; }

    public SymbolEntry getFuncionPadre() { return funcionPadre; }
    public void setFuncionPadre(SymbolEntry funcionPadre) { this.funcionPadre = funcionPadre; }

    public String getTipoRetorno() { return tipoRetorno; }
    public void setTipoRetorno(String tipoRetorno) { this.tipoRetorno = tipoRetorno; }

    public List<SymbolEntry> getParametros() { return parametros; }
    public void addParametro(SymbolEntry param) { this.parametros.add(param); }

    public List<String> getTiposRetorno() {
        return tiposRetorno;
    }

    public void setTiposRetorno(List<String> tiposRetorno) {
        this.tiposRetorno = tiposRetorno;
    }
    
    // Helper para agregar uno a uno (opcional)
    public void addTipoRetorno(String tipo) {
        this.tiposRetorno.add(tipo);
    }
    public String getMecanismoPasaje() { return mecanismoPasaje; }
    public void setMecanismoPasaje(String m) { this.mecanismoPasaje = m; }

    public String getModoParametro() { return modoParametro; }
    public void setModoParametro(String m) { this.modoParametro = m; }

    public String getScopePrefix() { return scopePrefix; }
    public void setScopePrefix(String scopePrefix) { this.scopePrefix = scopePrefix; }

    public void setFuncionOrigen(SymbolEntry f) { this.funcionOrigen = f; }
    public SymbolEntry getFuncionOrigen() { return this.funcionOrigen; }



    @Override
    public String toString() {
        // Una versión más útil para debuggear
        return "SymbolEntry{" +
                "mangledName='" + mangledName + '\'' +
                ", tipo='" + tipo + '\'' +
                ", uso='" + uso + '\'' + ", lexeme="+ lexeme +
                (parametros.isEmpty() ? "" : ", #params=" + parametros.size()) +
                '}';
    }
}