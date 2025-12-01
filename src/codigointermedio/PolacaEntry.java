package codigointermedio;

import lexer.SymbolEntry;

public class PolacaEntry {

    private final int address;    // Dirección/índice de esta entrada
    private Object value;         // El valor real: String (operador) o SymbolEntry (operando)
    private boolean isJump;       // Indica si esta entrada es un salto (BF, BI)
    private String type; 

    public PolacaEntry(int address, Object value, String type, boolean isJump) {
        this.address = address;
        this.value = value;
        this.type = type;
        this.isJump = isJump;
    }

    // --- Getters y Setters ---

    public int getAddress() {
        return address;
    }

    public Object getValue() {
        return value;
    }

    // Usado por Backpatching
    public void setValue(Object targetAddress) {
        this.value = targetAddress; 
    }

    public boolean isJump() {
        return isJump;
    }
    
    public String getType() {
        return type;
    }
    
    // Representación para la salida
    // En codigointermedio/PolacaEntry.java

    @Override
    public String toString() {
        String displayValue = "ERROR_VISUALIZACION";

        if (value instanceof SymbolEntry) {
            // CASO 1: Es una Variable o Constante (SymbolEntry)
            SymbolEntry se = (SymbolEntry) value;
            
            // Prioridad 1: Nombre Mangled (ej: "A:MAIN")
            if (se.getMangledName() != null && !se.getMangledName().equals("null") && !se.getMangledName().isEmpty()) {
                displayValue = se.getMangledName();
            } 
            // Prioridad 2: Lexema original (ej: "4", "A") <-- ESTO ES LO QUE TE FALTA
            else if (se.getLexeme() != null) {
                displayValue = se.getLexeme();
            } 
            // Fallback de emergencia
            else {
                displayValue = "SIN_NOMBRE";
            }
            
        } else if (value == null) {
            // CASO 2: Salto pendiente (Backpatching no hecho aún)
            displayValue = isJump ? "JUMP_?" : "null";
            
        } else {
            // CASO 3: Es un Operador (+, -, <=) o una Dirección de Salto (Integer)
            String rawValue = value.toString();
            
            if (isJump) {
                // Decoración para entender si es un salto (BI/BF)
                // Si el valor es un número, podemos deducir el tipo
                try {
                    int target = Integer.parseInt(rawValue);
                    // Si salta hacia atrás (destino < origen) es un bucle (BI)
                    // Si salta hacia adelante es un condicional (BF)
                    String tipoSalto = (target < address) ? "BI" : "BF";
                    displayValue = tipoSalto + " " + rawValue;
                } catch (NumberFormatException e) {
                    displayValue = "JUMP " + rawValue;
                }
            } else {
                displayValue = rawValue;
            }
        }
        
        // Formato de tabla alineada
        return String.format("%-5d | %s", address, displayValue);
    }


}
