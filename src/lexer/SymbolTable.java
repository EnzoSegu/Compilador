package lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    private Stack<Map<String, SymbolEntry>> scopes;
    private Stack<String> scopeNames; // Pila para el name mangling (ej: "MAIN", "AA")
    private List<SymbolEntry> allEntries;

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.scopeNames = new Stack<>();
        this.allEntries = new ArrayList<>();
    }

    /**
     * Inicia un nuevo ámbito (scope).
     * @param name El nombre del ámbito (ej: "MAIN" o el nombre de una función "AA")
     */
    public void pushScope(String name) {
        scopes.push(new HashMap<>());
        scopeNames.push(name);
    }

    /**
     * Cierra el ámbito actual.
     */
    public void popScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
            scopeNames.pop();
        }
    }

    /**
     * Devuelve el nombre del ámbito actual para el name mangling (ej: ":MAIN:AA")
     */
    public String getCurrentScopeMangledName() {
        StringBuilder sb = new StringBuilder();
        for (String s : scopeNames) {
            sb.append(":");
            sb.append(s);
        }
        return sb.toString();
    }
    
    /**
     * Devuelve el nombre simple del ámbito actual (ej: "AA")
     */
    public String getCurrentScopeName() {
        if (scopeNames.isEmpty()) {
            return null;
        }
        return scopeNames.peek();
    }

    /**
     * Añade un símbolo al ámbito actual (el de arriba de la pila).
     * Chequea redeclaraciones SÓLO en este ámbito.
     * @param entry El SymbolEntry a añadir.
     * @return true si se añadió, false si ya existía (redeclaración).
     */
    public boolean add(SymbolEntry entry) {
        if (scopes.isEmpty()) {
            System.out.println("Error: No hay ámbito activo para añadir símbolos.");
            return false; // No hay ámbito
        }

        Map<String, SymbolEntry> currentScope = scopes.peek();
        String lexeme = entry.getLexeme();
        
        // Regla de Alcance: "No se permiten variables y funciones con el mismo nombre dentro de un mismo ámbito"
        if (currentScope.containsKey(lexeme)) {
            return false; // Error: Redeclaración en el mismo ámbito
        }
        if (!"constante".equals(entry.getUso())) {
            // Aplicar Name Mangling de ámbito para variables y funciones.
            String mangledName = lexeme + getCurrentScopeMangledName();
            entry.setMangledName(mangledName);
        }

        currentScope.put(lexeme, entry);
        allEntries.add(entry);
        return true;
    }

    /**
     * Busca un símbolo desde el ámbito actual hacia arriba (global).
     * (Implementa Tema 23: búsqueda automática hacia arriba)
     * @param lexeme El nombre simple (ej: "A")
     * @return El SymbolEntry si se encuentra, o null.
     */
    public  SymbolEntry lookup(String lexeme) {
        // Iterar desde el tope de la pila (ámbito local) hacia abajo (global)
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, SymbolEntry> scope = scopes.get(i);
            if (scope.containsKey(lexeme)) {
                return scope.get(lexeme);
            }
        }
        return null; // No se encontró en ningún ámbito visible
    }

    /**
     * Busca un símbolo en un ámbito específico (para Tema 23: "MAIN.A")
     * @param lexeme El nombre simple (ej: "A")
     * @param scopePrefix El prefijo de ámbito (ej: "MAIN" o "MAIN.AA")
     * @return El SymbolEntry si se encuentra, o null.
     */
public SymbolEntry lookup(String lexeme, String scopePrefix) {
        
        // CASO 1: Acceso al ROOT (MAIN)
        // Las variables del main son siempre "NOMBRE:MAIN"
        if (scopePrefix.equals("MAIN")) {
            String target = lexeme + ":MAIN";
             for (SymbolEntry e : allEntries) {
                if (e.getMangledName().equals(target)) {
                    return e;
                }
            }
            return null;
        }

        // CASO 2: Acceso a un ámbito anidado (Funciones AA, BB, etc.)
        // 1. Primero buscamos "quién es" ese prefijo (ej: buscamos la función "AA")
        SymbolEntry scopeSymbol = lookup(scopePrefix); // Usamos el lookup normal recursivo

        if (scopeSymbol != null) {
            // Encontramos la función AA. Su mangledName será algo como "AA:MAIN"
            String scopeMangled = scopeSymbol.getMangledName();
            
            // 2. Extraemos la "ruta padre" quitando el nombre de la función del principio.
            // Si scopeMangled es "AA:MAIN" y el prefijo es "AA", 
            // el substring nos deja ":MAIN"
            String parentPath = scopeMangled.substring(scopePrefix.length());
            
            // 3. Construimos el nombre real de la variable que buscamos.
            // Estructura: NOMBRE_VAR + RUTA_PADRE + ":" + NOMBRE_SCOPE
            // Ejemplo: "P" + ":MAIN" + ":" + "AA" = "P:MAIN:AA"
            String targetMangledName = lexeme + parentPath + ":" + scopePrefix;

            // 4. Buscamos la coincidencia exacta
            for (SymbolEntry entry : allEntries) {
                if (entry.getMangledName().equals(targetMangledName)) {
                    return entry;
                }
            }
        }

        return null; // No se encontró
    }
    /**
     * Actualiza el valor de un símbolo existente en la tabla de símbolos.
     * Busca desde el ámbito actual hacia arriba (global) y actualiza el primer
     * símbolo que encuentre con ese lexema.
     * @param entry El SymbolEntry con el valor actualizado.
     * @return true si se actualizó, false si no se encontró el símbolo.
     */
    public boolean updateValue(SymbolEntry entry) {
        String lexeme = entry.getLexeme();
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, SymbolEntry> scope = scopes.get(i);
            if (scope.containsKey(lexeme)) {
                SymbolEntry existing = scope.get(lexeme);
                existing.setValue(entry.getValue());
                return true;
            }
        }
        return false;
    }
    /**
     * Busca si un lexema existe SÓLO en el ámbito actual.
     */
    public boolean containsInCurrentScope(String lexeme) {
        if (scopes.isEmpty()) {
            return false;
        }
        return scopes.peek().containsKey(lexeme);
    }

    public List<SymbolEntry> getAllEntries() {
    return this.allEntries;
}

public SymbolEntry lookupByLexeme(String lexeme) {
    for (SymbolEntry e : allEntries) {
        // Utilizamos equalsIgnoreCase si la búsqueda debe ignorar mayúsculas/minúsculas
        if (lexeme.equals(e.getLexeme())) {
            return e;
        }
    }
    return null;
}

    // En SymbolTable.java


@Override
public String toString() {
    if (allEntries.isEmpty()) return "Tabla de Símbolos vacía.";

    // 1. Calcular anchos máximos iniciales (basados en las cabeceras)
    int wName = "NOMBRE MANGLED".length();
    int wType = "TIPO".length();
    int wUso  = "USO".length();
    int wVal  = "VALOR".length();

    // 2. Recorrer entradas para ajustar el ancho si hay contenidos más largos
    for (SymbolEntry e : allEntries) {
        String name = e.getMangledName() != null ? e.getMangledName() : "null";
        String tipo = e.getTipo() != null ? e.getTipo() : "-";
        String uso  = e.getUso() != null ? e.getUso() : "-";
        String val  = e.getValue() != null ? e.getValue().toString() : "-";
        
        wName = Math.max(wName, name.length());
        wType = Math.max(wType, tipo.length());
        wUso  = Math.max(wUso, uso.length());
        wVal  = Math.max(wVal, val.length());
    }

    // Agregar un pequeño margen de padding (espacio extra)
    wName += 2; 
    wType += 2; 
    wUso += 2; 
    wVal += 2;

    // 3. Generar formatos y líneas separadoras dinámicas
    String format = "| %-" + wName + "s | %-" + wType + "s | %-" + wUso + "s | %-" + wVal + "s |\n";
    String line = "+" + "-".repeat(wName + 2) + "+" + "-".repeat(wType + 2) + "+" + "-".repeat(wUso + 2) + "+" + "-".repeat(wVal + 2) + "+\n";
    
    // Ancho total para las líneas de detalles (Funciones)
    int totalWidth = (wName + 2) + (wType + 2) + (wUso + 2) + (wVal + 2) + 5; 

    StringBuilder sb = new StringBuilder();
    sb.append("\n");

    
    // Cabecera
    sb.append(line);
    sb.append(String.format(format, "NOMBRE MANGLED", "TIPO", "USO", "VALOR"));
    sb.append(line);

    // 4. Imprimir filas
    for (SymbolEntry e : allEntries) {
        String name = e.getMangledName() != null ? e.getMangledName() : "null";
        String tipo = (e.getTipo() != null) ? e.getTipo() : "-";
        String uso = (e.getUso() != null) ? e.getUso() : "-";
        String val = (e.getValue() != null) ? e.getValue().toString() : "-"; 
        
        sb.append(String.format(format, name, tipo, uso, val));
        
        // --- LÓGICA DE DETALLES PARA FUNCIONES ---
        if ("funcion".equals(uso)) {
            // Imprimir una línea separadora interna sutil
            String innerLine = "|" + "-".repeat(totalWidth - 2) + "|\n";
            sb.append(innerLine);
            
            // Retornos
            String retornos = String.join(", ", e.getTiposRetorno());
            String textRet = " -> Retornos: (" + (retornos.isEmpty() ? "void" : retornos) + ")";
            sb.append(String.format("| %-" + (totalWidth - 4) + "s |\n", textRet));
            
            // Parámetros
            List<SymbolEntry> params = e.getParametros();
            if (!params.isEmpty()) {
                sb.append(String.format("| %-" + (totalWidth - 4) + "s |\n", " -> PARÁMETROS:"));
                for (SymbolEntry p : params) {
                    String paramInfo = String.format("      * %s %s (Pasaje: %s, Modo: %s)", 
                        p.getTipo(), p.getLexeme(), p.getMecanismoPasaje(), p.getModoParametro());
                    sb.append(String.format("| %-" + (totalWidth - 4) + "s |\n", paramInfo));
                }
            }
            // Línea de cierre de la sección de función
            sb.append(line); 
        }
    }
    // Cierre final si la última no fue función (para que no quede doble línea)
    if (!allEntries.isEmpty() && !"funcion".equals(allEntries.get(allEntries.size()-1).getUso())) {
        sb.append(line);
    }
    
    return sb.toString();
}
}