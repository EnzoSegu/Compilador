package codigointermedio;

/**
 * Clase para chequear la compatibilidad de tipos en operaciones y asignaciones.
 */
public class TypeChecker {

    /**
     * Chequea la compatibilidad en operaciones aritméticas o comparaciones.
     * Retorna el tipo resultante o "error".
     */
    public static String checkArithmetic(String type1, String type2) {
        if (type1.equals("float") && type2.equals("float")) {
            return "float";
        }
        if (type1.equals("int") && type2.equals("int")) {
            return "int";
        }
        
        // Tema 29: Conversiones Explícitas.
        // Si no hay conversión explícita, se debería informar un error.
        // Nota: Si se requiere Conversión IMPLÍCITA, esta lógica cambiaría.
        
        return "error"; // Tipos incompatibles
    }

    /**
     * Chequea la compatibilidad en una asignación (destino = fuente).
     * Retorna true si es compatible, false si no lo es.
     */
    public static boolean checkAssignment(String typeDestino, String typeFuente) {
        if (typeDestino == null || typeFuente == null) {
            // Un tipo nulo (reserva de nombre sin asignar) puede ser un caso válido.
            return true; 
        }
        
        if (typeDestino.equals(typeFuente)) {
            return true;
        }
        
        // Tema 29: La asignación de 'int' a 'float' SOLO es válida con conversión explícita.
        if (typeDestino.equals("float") && typeFuente.equals("int")) {
            // Esto es un error, a menos que la fuente sea un TOF/TOD.
            // Para simplificar, asumimos que si no son iguales, se requiere chequeo extra.
            return false;
        }

        return false;
    }
}