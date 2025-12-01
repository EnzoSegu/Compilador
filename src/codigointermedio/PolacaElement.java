package codigointermedio;

import java.util.ArrayList;
import java.util.List;
import lexer.SymbolEntry; // <<<<<< AÑADIR ESTA IMPORTACIÓN

/**
 * Representa un elemento de una expresión que se transporta a través de la pila
 * del parser. Contiene el tipo de resultado y la referencia a su posición en la Polaca.
 */
public class PolacaElement {

    private String resultType;      // Tipo resultante de la expresión (ej: "int", "float")
    private SymbolEntry resultEntry; // <<<<<< CAMBIAR ESTO: Referencia al SymbolEntry (temporal o ID)
    
    private List<Integer> trueList; 
    private List<Integer> falseList; 
    private int referenceIndex; // <-- Campo para guardar el índice


    public PolacaElement(String resultType, int referenceIndex) {
        // Llama al constructor que acepta el tipo (String)
        this.resultType = resultType;
        this.referenceIndex = referenceIndex;
        this.resultEntry = null; // No hay SymbolEntry asociado
        
        this.trueList = new ArrayList<>();
        this.falseList = new ArrayList<>();
    }

    // <<<<<< AÑADIR ESTE CONSTRUCTOR (REEMPLAZA EL QUE USA 'int referenceIndex')
    public PolacaElement(String resultType, SymbolEntry resultEntry) {
        this.resultType = resultType;
        this.resultEntry = resultEntry; 
        this.trueList = new ArrayList<>();
        this.falseList = new ArrayList<>();
    }

    // Constructor para elementos 'dummy' o de error
    public PolacaElement(String resultType) {
        this(resultType, null); // Ahora llama al nuevo constructor con null
    }
    
    // Constructor para listas vacías (o iniciales) de condiciones
    public PolacaElement() {
        this("void", null);
    }
    
    // --- Getters y Setters ---

    public String getResultType() {
        return resultType;
    }
    
    public SymbolEntry getResultEntry() { // <<<<<< NUEVO GETTER
        return resultEntry;
    }

    public List<Integer> getTrueList() {
        return trueList;
    }

    public List<Integer> getFalseList() {
        return falseList;
    }
    
    // Los métodos set/get 'referenceIndex' que usan 'int' deben ser eliminados o adaptados.
}