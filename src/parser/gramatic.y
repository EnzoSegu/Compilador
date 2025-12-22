/* ============================================================
   parser.y - Analizador sintáctico del compilador (BYACC/J)
   ============================================================ */

%{
package parser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import lexer.*;
import codigointermedio.*; 

    // --- Gestores Principales ---
    private SymbolTable symbolTable;
    private Map<String, PolacaInversa> polacaGenerada;
    private Stack<PolacaInversa> pilaGestoresPolaca;
    private Stack<List<Integer>> pilaSaltosBF = new Stack<>();
    private Stack<List<Integer>> pilaSaltosElse = new Stack<>();

    // --- Variables de control ---
    private Scanner lexer;
    private boolean errorEnProduccion = false;
    private boolean listaVariablesError = false;
    private boolean listaExpresionesError = false;
    private boolean listaTiposError = false;
    private SymbolEntry currentFunctionEntry = null;
    private int lambdaCounter = 0;
    private PolacaInversa lambdaBodyBuffer = null; 
    private PolacaInversa lambdaAssignBuffer = null; 
    private SymbolEntry currentLambdaFormal = null;
    private boolean  errorfuncion = false;
    private boolean error_comparacion = false;
    // --- LISTA DONDE SE GUARDAN LOS MENSAJES ---
    private List<String> listaErrores = new ArrayList<>();
    private List<String> listaWarnings = new ArrayList<>();

    public Parser() {
        this.symbolTable = new SymbolTable();
        this.polacaGenerada = new HashMap<>();
        this.pilaGestoresPolaca = new Stack<>();
    }

    private PolacaInversa PI() {
        if (pilaGestoresPolaca.isEmpty()) return new PolacaInversa(symbolTable); 
        return pilaGestoresPolaca.peek();
    }

    public SymbolTable getSymbolTable() { return this.symbolTable; }
    public Map<String, PolacaInversa> getPolacaGenerada() { return this.polacaGenerada; }
    public List<String> getListaErrores() { return this.listaErrores; }
    public List<String> getWarnings() { return listaWarnings; }

    // =========================================================================
    //  MÉTODOS DE ERROR (Sustituyen a System.err.println)
    // =========================================================================

    private void removeLastGenericError() {
        if (listaErrores.isEmpty()) return;
    
    // El mensaje genérico de BYACC/J siempre contiene el prefijo "Error Sintactico" y "syntax error"
        String lastError = listaErrores.get(listaErrores.size() - 1);
    
        if  (lastError.contains("syntax error")) {
            listaErrores.remove(listaErrores.size() - 1);
        // NO se toca errorEnProduccion aquí, ya que el error existe.
        }
}

    // Método para los errores manuales en la gramática (Sintácticos)
    private void addError(String mensaje) {
        removeLastGenericError();
        int linea=lexer.getContext().getLine();
        String error = "Línea " + linea + " " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }


    public void addWarning(String s) {
    String mensaje = "Línea " + lexer.getContext().getLine() + " - WARNING: " + s;
    listaWarnings.add(mensaje);
    }


    public void addErrorSemicolon(String msg) {
    removeLastGenericError();
    addWarning(msg); // Redirige a warning, no bloquea compilación
    }


    private void addErrorLex(String mensaje, int linea) {
        removeLastGenericError();
        String error = "Línea " + linea + ": " + mensaje;
        listaErrores.add(error);
        errorEnProduccion = true;
    }


    //  Método para errores SEMÁNTICOS (Tipos, declaraciones)
    // Se usa: yyerror("Mensaje", true);
    public void yyerror(String s, boolean semantico) {
        if(!errorEnProduccion){
            String error = "Línea " + lexer.getContext().getLine()+" "+ s;
            listaErrores.add(error);
        }
        errorEnProduccion = true;
    }

    // 3. Método automático de BYACC (Sintácticos por defecto)
    public void yyerror(String s) {
        if(!errorEnProduccion){
            String error = "Línea " + lexer.getContext().getLine()+" " + s;
            listaErrores.add(error);
        }
        errorEnProduccion = true;
    }

    public void setScanner(Scanner lexer) { this.lexer = lexer; }

    private int yylex() {
        Token tok = null;
        try {
            tok = lexer.nextToken();
        } catch (IOException e) {
            addError("Error Fatal de IO: " + e.getMessage());
            return 0;
        }
        if (tok == null) return 0;
        if (tok.getEntry() != null) {
            SymbolEntry entryDelScanner = tok.getEntry();
            
            // Buscamos si ya existe (para recuperar el objeto FUNCION original)
            SymbolEntry entryExistente = symbolTable.lookup(entryDelScanner.getLexeme());
            
            if (entryExistente != null) {
                yylval = new ParserVal(entryExistente); // <--- IMPORTANTE: new
            } else {
                yylval = new ParserVal(entryDelScanner); // <--- IMPORTANTE: new
            }
        }
        switch (tok.getType()) {
            case ID: return ID;
            case STRING: return STRING;
            case INT16: return INT16;
            case FLOAT32: return FLOAT32;
            case VAR: return VAR;
            case PRINT: return PRINT;
            case IF: return IF;
            case ELSE: return ELSE;
            case ENDIF: return ENDIF;
            case RETURN: return RETURN;
            case FOR: return FOR;
            case FROM: return FROM;
            case TO: return TO;
            case PLUS: return PLUS;
            case MINUS: return MINUS;
            case STAR: return STAR;
            case SLASH: return SLASH;
            case ASSIGN: return ASSIGN;
            case ASSIGN_COLON: return ASSIGN_COLON;
            case EQ: return EQ;
            case NEQ: return NEQ;
            case LT: return LT;
            case LE_OP: return LE_OP;
            case GT: return GT;
            case GE: return GE;
            case LPAREN: return LPAREN;
            case RPAREN: return RPAREN;
            case LBRACE: return LBRACE;
            case RBRACE: return RBRACE;
            case UNDERSCORE: return UNDERSCORE;
            case SEMICOLON: return SEMICOLON;
            case ARROW: return ARROW;
            case COMMA: return COMMA;
            case CV: return CV;
            case CR: return CR;
            case LE_KW: return LE_KW;
            case TOF: return TOF;
            case INT_KW: return INT_KW;
            case FLOAT_KW: return FLOAT_KW;
            case ERROR:
                addErrorLex(tok.getLexeme(), tok.getLine()); 
                SymbolEntry fakeEntry = new SymbolEntry("ERROR_IGNORE");
                fakeEntry.setTipo("error"); 
                yylval.entry = fakeEntry;
                return ID;
            case EOF: return 0;
            case POINT: return POINT;
            default: return ERROR;
        }
    }
%}

/* ======= Unión de valores semánticos ======= */
%union {
    String sval;
    Integer ival;
    Double dval;
    Object obj;
    SymbolEntry entry;
    PolacaElement Polacaelement;
    ForContext contextfor;
    ArrayList<String> listStr;
    String[] semantica;
    ParametroInvocacion paramInv;
    ArrayList<ParametroInvocacion> listParamInv;
}

/* ======= Tokens ======= */
%nonassoc PRIORIDAD_BAJA_ASIGNACION  /* Prioridad menor para la reducción */
%nonassoc ID INT16 FLOAT32 STRING
%nonassoc PRIORIDAD_ID  /* Prioridad muy baja */
%nonassoc LPAREN
%nonassoc error
%token <entry> ID STRING INT16 FLOAT32
%token IF ELSE ENDIF PRINT RETURN VAR FOR FROM TO
%token PLUS MINUS STAR SLASH
%token ASSIGN ASSIGN_COLON
%token <sval> EQ NEQ LT LE_OP GT GE
%token LPAREN RPAREN LBRACE RBRACE UNDERSCORE SEMICOLON ARROW COMMA POINT
%token CV CR LE_KW TOF
%token ERROR EOF
%token INT_KW FLOAT_KW STRING_KW
%type <entry> identificador ERROR

%type <contextfor> encabezado_for
%type <Polacaelement> expresion termino factor conversion_tof invocacion_funcion condicion lambda_argumento lambda_expresion
%type <entry> identificador_completo inicio_programa identificador_destino inicio_funcion  INT_KW FLOAT_KW parametro_formal 
%type <obj> lista_variables tipo parametros_formales lista_expresiones lista_variables_destino lista_tipos if_simple lambda_prefix 
%type <sval> operador_comparacion
%type <semantica> sem_pasaje
%type <listParamInv> parametros_reales
%type <paramInv> parametro_real

%left PLUS MINUS
%left STAR SLASH
%start input

%%

/* ======= Reglas de gramática ======= */

input
    : /* vacío */
    | programa
    | programa error  
        { 
            addError("Contenido inesperado después del final del programa. ¿Una '}' extra?");
        }
    ;

// programa
inicio_programa
    : identificador 
      { 
          SymbolEntry se_prog = (SymbolEntry)$1;
          SymbolTable symTab = symbolTable;
          se_prog.setUso("programa"); 
          symTab.pushScope(se_prog.getLexeme()); 
          symTab.add(se_prog);

          PolacaInversa mainPI = new PolacaInversa(symbolTable);
          polacaGenerada.put(se_prog.getMangledName(), mainPI);
          pilaGestoresPolaca.push(mainPI);

          $$ = $1; 
      }
    ;
programa
    : inicio_programa LBRACE lista_sentencias RBRACE
    {
        SymbolEntry se_prog = (SymbolEntry)$1;
        PolacaInversa polaca = new PolacaInversa(symbolTable);
        polaca.generateFunctionEnd(se_prog);

        SymbolTable symTab = symbolTable;
        symTab.popScope(); 
        pilaGestoresPolaca.pop();

        if (!errorEnProduccion) {
            System.out.println("Línea " + lexer.getContext().getLine() + ": Estructura detectada correctamente: PROGRAMA. Ámbito cerrado.");
        }
    }
    | error LBRACE lista_sentencias RBRACE
        {
            addError("Error Sintactico: Falta nombre de programa.");
        }
    ;
    

// Lista de sentencias
lista_sentencias
    : /* vacio */
    | lista_sentencias sentencia_declarativa
        { errorEnProduccion = false; } // REINICIAR

    | lista_sentencias sentencia_ejecutable
        { errorEnProduccion = false; } // REINICIAR

   | lista_sentencias error  
        { 
            // 1. Reinicia la bandera PRIMERO para permitir que se guarde este nuevo mensaje
            errorEnProduccion = false; 
            
            
            
            removeLastGenericError(); 
        }
    ;

lista_sentencias_sin_return
    : /* vacio */
    | lista_sentencias_sin_return sentencia_declarativa 
    | lista_sentencias_sin_return sentencia_ejecutable_sin_return 
    | lista_sentencias_sin_return error 
        { 
            addError("Error en cuerpo de función. Recuperando en ';'."); 
        }
    ;

sentencia_declarativa
    : declaracion_funcion
    | declaracion_variable
    ;

declaracion_variable
    : VAR lista_variables SEMICOLON
        {   ArrayList<SymbolEntry> entries = (ArrayList<SymbolEntry>)$2;
            if(!errorfuncion){
            boolean redeclared = false;

            for (SymbolEntry entry : entries) {
                entry.setUso("variable");
                entry.setTipo("untype"); 
                
                if (symbolTable.add(entry) ) { 
                    
                }else{
                    yyerror("Error Semantico: Variable '" + entry.getLexeme() + "' redeclarada en el ámbito actual.", true);
                    redeclared = true;
                }
            }
            if(listaVariablesError || redeclared) {
                errorEnProduccion = true;
            } else {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Declaración de variables registrada en TS.");
            }
            listaVariablesError = false; 
            }
        }
    | VAR SEMICOLON
        { 
            addError("Error Sintactico: Falta lista de variables a continuación de var.");
        } 
    | VAR lista_variables %prec PRIORIDAD_BAJA_ASIGNACION
        { 
            addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la declaración de variables (Se procede a ignorarlo y continuar).");
            
            /* --- RECUPERACIÓN: PROCESAR VARIABLES --- */
            /* Copiamos la lógica de la regla correcta */
            ArrayList<SymbolEntry> entries = (ArrayList<SymbolEntry>)$2;
            if(!errorfuncion && entries != null){
                boolean redeclared = false;
                for (SymbolEntry entry : entries) {
                    entry.setUso("variable");
                    entry.setTipo("untype"); 
                    
                    if (!symbolTable.add(entry)) { 
                        yyerror("Error Semantico: Variable '" + entry.getLexeme() + "' redeclarada en el ámbito actual.", true);
                        redeclared = true;
                    }
                }
                if(!listaVariablesError && !redeclared) {
                     System.out.println("Línea " + lexer.getContext().getLine() + ": Declaración de variables registrada (Recuperación).");
                }
            }
            listaVariablesError = false; 
        }
        | VAR error
        { 
            addError("Error Sintactico: Falta lista de variables a continuación de var.");
        } 
    ;
identificador
    : ID %prec PRIORIDAD_ID
      { 
          $$ = $1; 
      }
    | ID UNDERSCORE ID 
      { 
          /* Reportamos el error personalizado */
          yyerror("Error Lexico: Identificador inválido '" + $1.getLexeme() + "_" + $3.getLexeme() + "'. El caracter '_' no está permitido en los identificadores.", true);
          
          /* RECUPERACIÓN: Asumimos que el usuario quería usar el primer ID para seguir compilando */
          $$ = $1; 
      }
    | ID ERROR ID {
            yyerror("Error Lexico: Identificador inválido '" + $1.getLexeme() + "_" + $3.getLexeme() + "'. El caracter $2.getLexeme() no está permitido en los identificadores.", true);
    }
    ;
identificador_completo
    : identificador
    {    
        $$ = $1;
    }
    | identificador_completo POINT identificador {
        // Caso prefijado: MAIN.A
        SymbolEntry scopeID = (SymbolEntry)$1;
        SymbolEntry varID = (SymbolEntry)$3;
        
        // Guardamos "MAIN" dentro de "A" para usarlo después en el lookup
        varID.setScopePrefix(scopeID.getLexeme());
        
        $$ = varID;
    };

lista_variables
    : identificador_completo
        {
            ArrayList<SymbolEntry> list = new ArrayList<>();
            SymbolEntry se = (SymbolEntry)$1;
            
            // VALIDACIÓN: No se puede declarar con prefijo
            if (se.getScopePrefix() != null) {
                yyerror("Error sintáctico: No se puede utilizar prefijos ('" + se.getScopePrefix() + "') en la declaración de variables.", true);
            }
            
            list.add(se);
            $$ = list;
        }
    | lista_variables COMMA identificador_completo
        {
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)$1;
            SymbolEntry se = (SymbolEntry)$3;

            // VALIDACIÓN
            if (se.getScopePrefix() != null) {
                yyerror("Error sintáctico: No se puede utilizar prefijos en la declaración.", true);
            }
            
            if (lista == null) lista = new ArrayList<>();
            lista.add(se);
            $$ = lista; 
        }
    | lista_variables  identificador_completo
        {
            addError("Error Sintáctico: Falta coma ',' en la declaración de variables.");
            
            // Recuperación para que siga funcionando
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)$1;
            if (lista == null) lista = new ArrayList<>();
            lista.add((SymbolEntry)$2); // $2 es el identificador
            $$ = lista;
        }
    ;
        
identificador_destino:
    identificador_completo {
        SymbolEntry entradaParser = (SymbolEntry)$1;
        String lexema = entradaParser.getLexeme();
        if(lexema.equals("ERROR_IGNORE")){
            $$=entradaParser;
        }
        else{
            String prefijo = entradaParser.getScopePrefix();
        
        SymbolEntry encontrado = null;

        // --- LÓGICA TEMA 23 ---
        if (prefijo != null) {
            // Caso 2: Tiene prefijo (ej: MAIN.A) -> Buscar en ámbito específico
            // Usamos el método lookup(lexema, scope) que tenías en SymbolTable
            encontrado = symbolTable.lookup(lexema, prefijo);
            
            if (encontrado == null) {
                yyerror("Error Semantico: La variable '" + lexema + "' no existe en el ámbito '" + prefijo + "' o no es visible.", true);
            }
        } else {
            // Caso 1: No tiene prefijo (ej: A) -> Buscar en actual y hacia arriba
            encontrado = symbolTable.lookup(lexema);
            
            if (encontrado == null) {
                yyerror("Error Semantico: Variable no declarada: '" + lexema + "'", true);
            }
        }
        // ----------------------

        if (encontrado == null) {
             listaVariablesError = true; 
             errorEnProduccion = true; 
             $$ = entradaParser; // Retornamos el dummy para evitar null pointers
        } else {
             $$ = encontrado; // Retornamos la entrada REAL de la tabla
        }
    }
};
        

    lista_variables_destino
    : identificador_destino {
            ArrayList<SymbolEntry> list = new ArrayList<>();
            list.add((SymbolEntry)$1);
            $$ = list;
        }
    | lista_variables_destino COMMA identificador_destino{            
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)$1;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add($3);
            $$ = lista; };
    | lista_variables_destino  identificador_destino{
            addError(" Error sintáctico: Se esperaba una ',' en la asignacion de variables.");
    } ;


inicio_funcion
    : lista_tipos identificador
      { 
          @SuppressWarnings("unchecked")
          ArrayList<SymbolEntry> tiposRetorno = (ArrayList<SymbolEntry>)$1;
          SymbolEntry se_func = (SymbolEntry)$2;
          SymbolTable symTab = symbolTable;
           
          se_func.setUso("funcion");

          if (tiposRetorno != null) {
              for (SymbolEntry tipo : tiposRetorno) {
                  // Guardamos el lexema ("int", "float") en la definición de la función
                  se_func.addTipoRetorno(tipo.getLexeme()); 
              }
          }
           
          if (!symbolTable.add(se_func)) {
                yyerror("Error Semantico: Función redeclarada '" + se_func.getLexeme() + "'", true);
            }  

          currentFunctionEntry = se_func;

          symTab.pushScope(se_func.getLexeme()); 
          PolacaInversa funcPI = new PolacaInversa(symbolTable);
          polacaGenerada.put(se_func.getMangledName(), funcPI);
          pilaGestoresPolaca.push(funcPI); 
            
          PI().generateFunctionStart(se_func);

          $$ = $2; 
      }
    | lista_tipos error{
        addError("Error Sintactico: Falta nombre de funcion");
        errorfuncion=true;
        $$ = null;
    }
    ;
declaracion_funcion
    :inicio_funcion LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE 
    {
            SymbolEntry se = (SymbolEntry)$1;
                if(se == null){
                errorfuncion= false;
            }else{
                PI().generateFunctionEnd(se);

                if (listaTiposError) {
                    addError("Error Sintactico: Falta ',' en lista de tipos de retorno.");
                }
            
                if (!errorEnProduccion) { 
                    System.out.println("Línea " + lexer.getContext().getLine() + ": Declaracion de funcion detectada " + se.getLexeme());
            }
                symbolTable.popScope();
                pilaGestoresPolaca.pop();

                listaTiposError = false; 
                currentFunctionEntry = null;
        }
    }
    | inicio_funcion  LPAREN error RPAREN LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE
        { 
            addError("Error Sintactico: Se tiene que tener mínimo un parámetro formal.");
        }
    | inicio_funcion  LPAREN parametros_formales RPAREN error lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE 
        { 
            addError("Error Sintactico: Falta '{' de apertura de función.");
        }
    | inicio_funcion LPAREN parametros_formales error LBRACE lista_sentencias_sin_return sentencia_return lista_sentencias RBRACE 
        { 
            SymbolEntry se = (SymbolEntry)$1;
            listaTiposError = false; 
            addError("Error Sintactico: Falta ')' después de los parámetros de la función " + se.getLexeme() + ". Asumiendo inicio de bloque '{'."); 
        }
    | inicio_funcion LPAREN parametros_formales RPAREN LBRACE lista_sentencias_sin_return RBRACE
    {
        SymbolEntry se = (SymbolEntry)$1;
        
        // ** ERROR SINTÁCTICO/OBLIGATORIEDAD **
        addError("Error Sintactico: la sentencia RETURN es obligatoria.");
        
        PI().generateFunctionEnd(se);
        symbolTable.popScope();
        pilaGestoresPolaca.pop();
        currentFunctionEntry = null;
        
    }
    ;

lista_tipos
    : tipo
        {
            ArrayList<SymbolEntry> lista = new ArrayList<>();
            lista.add((SymbolEntry)$1);
            $$ = lista;
        }
    | lista_tipos COMMA tipo
        {
            @SuppressWarnings("unchecked")
            ArrayList<SymbolEntry> lista = (ArrayList<SymbolEntry>)$1;
            lista.add((SymbolEntry)$3);
            $$ = lista;
        }
    | lista_tipos tipo
        {
           listaTiposError = true;
           errorEnProduccion = true; 
        }
    ;

tipo
    : INT_KW {$$ = new SymbolEntry("int");}
    | FLOAT_KW {$$ = new SymbolEntry("float");}
    ;

parametros_formales
    : parametro_formal
    {$$=$1;}
    | parametros_formales error parametro_formal
        {
            addError("Falta ',' entre parametros.");
        }
    | parametros_formales COMMA parametro_formal
    ;

parametro_formal
    : tipo ID
    { 
        if (!errorEnProduccion) { 
            SymbolEntry tipo = (SymbolEntry)$1;
            SymbolEntry se = (SymbolEntry)$2;

            se.setTipo(tipo.getLexeme());

            se.setUso("parametro");
            
            se.setMecanismoPasaje("cvr"); 
            se.setModoParametro("le");    
            
            /* 1. VINCULAR CON LA FUNCIÓN PADRE (Ya lo tenías) */
            if (currentFunctionEntry != null) {
                currentFunctionEntry.addParametro(se);
            }

            if (!symbolTable.add(se)) {
                yyerror("Error Semantico: El parámetro '" + se.getLexeme() + "' ya fue declarado en este ámbito.", true);
            }
            
            System.out.println("Parametro (Defecto CVR): " + se.getLexeme());
        }
    }
    | sem_pasaje tipo ID
    { 
        if (!errorEnProduccion) { 
            SymbolEntry tipo = (SymbolEntry)$2;
            SymbolEntry se = (SymbolEntry)$3;
            se.setTipo(tipo.getLexeme());
            se.setUso("parametro");
            
            String[] config = (String[])$1;
            se.setMecanismoPasaje(config[0]); 
            se.setModoParametro(config[1]);   
            
            if (currentFunctionEntry != null) {
                currentFunctionEntry.addParametro(se);
            }

            if (!symbolTable.add(se)) {
                yyerror("Error Semantico: El parámetro '" + se.getLexeme() + "' ya fue declarado en este ámbito.", true);
            }

            System.out.println("Parametro (" + config[0] + " " + config[1] + "): " + se.getLexeme());
        }
    }
    | sem_pasaje ID
    {
        SymbolEntry se = (SymbolEntry)$2;
        
        addError("Error Sintáctico: Falta el tipo de dato  para el parámetro '" + se.getLexeme() + "'.");
        
        se.setTipo("error"); 
        se.setUso("parametro");
        
        String[] config = (String[])$1;
        se.setMecanismoPasaje(config[0]);
        se.setModoParametro(config[1]);

        if (currentFunctionEntry != null) currentFunctionEntry.addParametro(se);
        symbolTable.add(se); 
    }
    | ID
    {
        SymbolEntry se = (SymbolEntry)$1;
        
        addError("Error Sintáctico: Falta el tipo de dato para el parámetro '" + se.getLexeme() + "'.");
        
        se.setTipo("error");
        se.setUso("parametro");
        se.setMecanismoPasaje("cvr");
        se.setModoParametro("le");

        if (currentFunctionEntry != null) currentFunctionEntry.addParametro(se);
        symbolTable.add(se);
    }
    | tipo{
        addError("Error Sintáctico: Falta de nombre de parámetro formal en declaración de función.");
    }
    | sem_pasaje tipo {
        addError("Error Sintáctico: Falta de nombre de parámetro formal en declaración de función.");
        }
    | error LE_KW
    {
        addError("Error Sintactico: Falta la semantica de pasaje(cv o cr) antes de directiva 'le'");
        $$ = new String[]{"error_pasaje", "le"};
    }
    ;

sem_pasaje
    : CV LE_KW 
    { 
        $$ = new String[]{"cv", "le"}; 
    }
    | CR LE_KW 
    { 
        $$ = new String[]{"cr", "le"}; 
    }
    /* Manejo de errores comunes */
    | CV error
    { 
        addError("Falta la directiva 'le' después de 'cv'.");
        $$ = new String[]{"cv", "le"}; 
    }
    | CR error
    { 
        addError("Falta la directiva 'le' después de 'cr'.");
        $$ = new String[]{"cr", "le"}; 
    }
    ;
sentencia_return
    : RETURN LPAREN lista_expresiones RPAREN SEMICOLON
        {
            ArrayList<PolacaElement> retornosReales = (ArrayList<PolacaElement>)$3;
            if (!errorfuncion){
            // Validar si hay errores previos de sintaxis
            if (listaExpresionesError) { 
                addError("Error Sintactico: Falta de ',' en argumentos de return.");
            }

            // Validar contra la firma de la función actual
            if (currentFunctionEntry == null) {
                yyerror("Error semantico: Return fuera de contexto de función.");
            } else {
                List<String> tiposEsperados = currentFunctionEntry.getTiposRetorno();
                
                // 1. VALIDAR CANTIDAD EXACTA (La función debe cumplir su promesa)
                // Nota: El Tema 20 habla de flexibilidad en la ASIGNACIÓN, no en la DEFINICIÓN.
                // Por ende, un return debe coincidir con la firma.
                if (retornosReales.size() != tiposEsperados.size()) {
                    yyerror("Error Semantico: La función '" + currentFunctionEntry.getLexeme() + 
                            "' debe retornar exactamente " + tiposEsperados.size() + 
                            " valores (firma declarada), pero retorna " + retornosReales.size() + ".", true);
                } else {
                    // 2. CHEQUEO DE TIPOS INDIVIDUAL
                    for (int i = 0; i < retornosReales.size(); i++) {
                        String tipoReal = retornosReales.get(i).getResultType();
                        String tipoEsperado = tiposEsperados.get(i);
                        
                        if (!codigointermedio.TypeChecker.checkAssignment(tipoEsperado, tipoReal)) {
                            yyerror("Error Semantico: Error de Tipo en retorno #" + (i+1) + 
                                    ": Se esperaba '" + tipoEsperado + 
                                    "' pero se encontró '" + tipoReal + "'.", true);
                        }
                    }
                }
            }

            // Generar instrucción RETURN
            PI().generateOperation("RETURN", false);

            if (!errorEnProduccion) { 
                System.out.println("Línea " + lexer.getContext().getLine() + ": Return validado correctamente.");
            }
            listaExpresionesError = false;
        }
    }
    | RETURN lista_expresiones RPAREN SEMICOLON{
        addError("Error Sintactico: Falta de '(' en return.");
    }
    | RETURN LPAREN lista_expresiones error SEMICOLON{
        addError("Error Sintactico: Falta de ')' en return.");
    }
    | RETURN LPAREN lista_expresiones RPAREN{
        addErrorSemicolon("Error Sintactico: Falta ; al final de la sentencia return (Se procede a ignorarlo)");
        
        /* --- RECUPERACIÓN: GENERAR RETURN --- */
        ArrayList<PolacaElement> retornosReales = (ArrayList<PolacaElement>)$3;
        if (!errorfuncion && !errorEnProduccion){
             // Validamos cantidad y tipos (copia simplificada de la lógica principal)
             if (currentFunctionEntry != null) {
                List<String> tiposEsperados = currentFunctionEntry.getTiposRetorno();
                if (retornosReales.size() == tiposEsperados.size()) {
                     // Generamos el RETURN
                     PI().generateOperation("RETURN", false);
                     System.out.println("Línea " + lexer.getContext().getLine() + ": Return generado (Warning).");
                }
             }
        }
    }
    
    ;

sentencia_ejecutable
    : asignacion 
    | sentencia_return
    | sentencia_print 
    | sentencia_if
    | sentencia_for
    | lambda_expresion
    ;
sentencia_ejecutable_sin_return
    : asignacion 
    | sentencia_print
    | sentencia_if
    | sentencia_for
    | lambda_expresion
    ;
    ;

sentencia_print
    : PRINT LPAREN expresion RPAREN SEMICOLON
       { 
            PolacaElement expr = (PolacaElement)$3;
            if(!errorfuncion){
            // Verificamos que la expresión sea válida antes de generar
            if (expr != null && expr.getResultEntry() != null && !"error".equals(expr.getResultType())) {
                
                // Agregamos la instrucción a la Polaca SIEMPRE
                PI().generatePrint(expr); 
                
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ": Print generado para " + expr.getResultEntry().getLexeme());
                }
            }
        }
        }
    | PRINT LPAREN error RPAREN SEMICOLON
       {
            addError("Error Sintactico: Falta argumento en print.");
       }
    | PRINT LPAREN expresion RPAREN error { 
        addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final del PRINT.");
        
        /* --- RECUPERACIÓN: GENERAR PRINT --- */
        PolacaElement expr = (PolacaElement)$3;
        if(!errorfuncion && !errorEnProduccion){
            if (expr != null && expr.getResultEntry() != null && !"error".equals(expr.getResultType())) {
                PI().generatePrint(expr);
                System.out.println("Línea " + lexer.getContext().getLine() + ": Print generado (Warning).");
            }
        }
    };



lista_sentencias_ejecutables
    : sentencia_ejecutable
    | lista_sentencias_ejecutables sentencia_ejecutable
    {
    }
    | lista_sentencias_ejecutables error SEMICOLON 
        { 
            yyerror("Error en bloque. Recuperando en ';'."); 
        }
    ;

bloque_sentencias_ejecutables
    : LBRACE lista_sentencias_ejecutables RBRACE
    | LBRACE RBRACE
        {
            addError("Error Sintáctico: El cuerpo de la iteración o bloque no puede estar vacío.");
        }
    | LBRACE error RBRACE
        {
            addError("Error Sintáctico: Error en el contenido del bloque o cuerpo vacío.");
            yyerrflag = 0;
        }
    ;

fin_else
    : ENDIF SEMICOLON
      {
          /* CASO CORRECTO */
          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado correctamente.");
      }
    | SEMICOLON
      {
          /* CASO ERROR: Falta 'ENDIF' pero hay ';' */
          addWarning("Error Sintactico: Falta palabra clave 'endif' al finalizar la selección (else).");
          
          /* Lógica de recuperación: Hacemos el backpatch igual para que el programa siga funcionando */
          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado (recuperado por falta de ENDIF).");
      }
    ;

resto_if
    /* OPCIÓN A: IF SIMPLE (Termina con ENDIF) */
    : ENDIF SEMICOLON
      {
          /* Recuperamos el salto BF de la pila (guardado en sentencia_if) */
          if (!pilaSaltosBF.isEmpty()) {
              List<Integer> listaBF = pilaSaltosBF.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBF, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF Simple detectado.");
      }
    | SEMICOLON
      {
        addWarning ("Error Sintactico: Falta palabra clave 'endif' al finalizar la selección.");
          /* Recuperamos el salto BF de la pila (guardado en sentencia_if) */
          if (!pilaSaltosBF.isEmpty()) {
              List<Integer> listaBF = pilaSaltosBF.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBF, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF Simple detectado.");
      }
    /* OPCIÓN B: IF-ELSE (Sigue con ELSE) */
    | ELSE 
      {     
          /* 1. Generar BI del THEN (Saltar el Else) */
          List<Integer> listaBI = PI().generateUnconditionalJump();
          pilaSaltosElse.push(listaBI);

          /* 2. Parchear el BF de la condición (Entrar al Else) */
          if (!pilaSaltosBF.isEmpty()) {
              List<Integer> listaBF = pilaSaltosBF.pop();
              int target_else = PI().getCurrentAddress();
              PI().backpatch(listaBF, target_else);
          }
      }
     bloque_sentencias_ejecutables fin_else
      {

          if (!pilaSaltosElse.isEmpty()) {
              List<Integer> listaBI = pilaSaltosElse.pop();
              int target_endif = PI().getCurrentAddress();
              PI().backpatch(listaBI, target_endif);
          }
          if (!errorEnProduccion) System.out.println("IF-ELSE detectado.");
      }
    | error 
      { 
          addError("Error Sintactico: Falta palabra clave 'endif' o 'else' al finalizar la selección."); 
      }
    ;

sentencia_if
    : IF LPAREN condicion RPAREN 
      {
          if (!error_comparacion){
            PolacaElement cond = (PolacaElement)$3;
            pilaSaltosBF.push(cond.getFalseList());
          }
          else{
            error_comparacion = false;
          }
      }
      bloque_sentencias_ejecutables 
      resto_if 
    
    /* --- ERRORES DEL ENCABEZADO (Estos se quedan aquí) --- */
    | IF error condicion RPAREN bloque_sentencias_ejecutables resto_if 
        { addError("Error Sintactico: Falta paréntesis de apertura '(' en IF."); }
    | IF LPAREN condicion  bloque_sentencias_ejecutables resto_if 
        { addError("Error Sintactico: Falta paréntesis de cierre ')' en condición."); }
    | IF LPAREN condicion RPAREN error resto_if 
        { addError("Error Sintactico: Error en el cuerpo de la cláusula then."); }
    ;

if_simple
    : IF LPAREN condicion RPAREN sentencia_ejecutable 
    { 
        if ( !error_comparacion){
            PolacaElement cond = (PolacaElement)$3;
            List<Integer> listaBF = cond.getFalseList();

            
            if (!errorEnProduccion) { 
                System.out.println("Línea " + lexer.getContext().getLine() + 
                    ": If simple detectado (sin backpatch aún)");
            }
            
            $$ = listaBF;
        }
        else{
            error_comparacion = false;
        }
    }

encabezado_for
    : FOR LPAREN identificador_destino FROM factor TO factor RPAREN
    {
        SymbolEntry id = (SymbolEntry)$3;
        PolacaElement cte1 = $5; 
        PolacaElement cte2 = $7;
        
    if (symbolTable.lookup(id.getLexeme()) != null) {
         if (!id.getTipo().equals("int") && !id.getTipo().equals("untype")) {
             yyerror("Error Semantico: La variable del for '" + id.getLexeme() + "' debe ser de tipo 'int'.", true);
             errorEnProduccion = true;
        } else if (id.getTipo().equals("untype")) {
             id.setTipo("int");
        }
            
        if (!errorEnProduccion) {
        
        int val1 = 0;
        int val2 = 0;
        try {
             /* [CORRECCIÓN APLICADA AQUÍ] Eliminamos el sufijo 'I' o 'i' antes de parsear.*/
             String lexeme1 = cte1.getResultEntry().getLexeme().replaceAll("[Ii]", "");
             String lexeme2 = cte2.getResultEntry().getLexeme().replaceAll("[Ii]", "");
             
             val1 = Integer.parseInt(lexeme1);
             val2 = Integer.parseInt(lexeme2);
             
        } catch(Exception e) {
             /* Esto se dejaría para errores no capturados por el léxico.*/
        }
        
        PolacaElement opCte1 = PI().generateOperand(cte1.getResultEntry());
        PI().generateAssignment(id, opCte1);
        
        int labelStart = PI().getCurrentAddress();
        
        String operador;
        boolean esIncremento;
        
        /* La lógica de comparación val1 <= val2 determina si es ascendente o descendente.*/
        if (val1 <= val2) {
            operador = "<=";
            esIncremento = true;
        } else {
            operador = ">="; /* Correcto para descendente*/
            esIncremento = false; /* Correcto para decremento (-)*/
        }
        
        PolacaElement exprId = PI().generateOperand(id);
        PolacaElement exprCte2 = PI().generateOperand(cte2.getResultEntry());
        
        PolacaElement condicion = PI().generateCondition(exprId, exprCte2, operador, "boolean");
        
        ForContext nuevoContexto = new ForContext(
            id,                     
            labelStart,             
            condicion.getFalseList(), 
            esIncremento            
        );
        
        yyval.contextfor = nuevoContexto;
        }
    }
    else {
            addError("Error Semantico: La variable del for '" + id.getLexeme() + "' no está declarada en el ámbito actual.");
            yyval.contextfor = null; 
        }
    }

sentencia_for
    : encabezado_for bloque_sentencias_ejecutables SEMICOLON
    {
        ForContext ctx = $1; 
       
            if(!errorfuncion){
                if (ctx != null) {
                PolacaElement opId = PI().generateOperand(ctx.variableControl);
                
                SymbolEntry unoConst = new SymbolEntry("1", "constante", "int");
                PolacaElement opUno = PI().generateOperand(unoConst);
                
                String opAritmetico = ctx.esIncremento ? "+" : "-";
                
                PolacaElement resultado = PI().generateOperation(opId, opUno, opAritmetico, "int");
                PI().generateAssignment(ctx.variableControl, resultado);
                
                List<Integer> biList = PI().generateUnconditionalJump();
                PI().backpatch(biList, ctx.labelInicio);
                
                int finDelFor = PI().getCurrentAddress();
                PI().backpatch(ctx.listaBF, finDelFor);
                }   
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ": Fin de sentencia FOR generado. Tipo: " + (ctx.esIncremento ? "Ascendente" : "Descendente"));
                }
            }
    
    }
    | FOR error identificador_destino FROM factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON
        { 
            addError("Error Sintactico: Falta parentesis de apertura en encabezado del for.");
        }
    | FOR LPAREN error FROM factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON
        {
            addError("Error Sintactico:: Falta nombre de variable en for.");
        }
    | FOR LPAREN identificador_destino  factor TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON
        { 
            addError("Error  Sintactico: Falta palabra clave 'from' en encabezado del for.");
        }
    | FOR LPAREN identificador_destino FROM error TO factor RPAREN bloque_sentencias_ejecutables SEMICOLON
        { 
            addError("Error Sintactico: Falta constante inicial en encabezado del for.");
        }
    | FOR LPAREN identificador_destino FROM factor error factor RPAREN bloque_sentencias_ejecutables SEMICOLON
        { 
            addError("Error Sintactico: Falta palabra clave 'to' en encabezado del for.");
        }
    | FOR LPAREN identificador_destino FROM factor TO error RPAREN bloque_sentencias_ejecutables SEMICOLON
        { 
            addError("Error Sintactico: Falta constante final en encabezado del for.");
        }
    | FOR LPAREN identificador_destino FROM factor TO factor error bloque_sentencias_ejecutables SEMICOLON
        { 
            addError("Error Sintactico: Falta parentesis de cierre en encabezado for.");
        }
    | FOR error SEMICOLON 
        { 
            yyerror("Error sintáctico grave en 'for'. No se pudo analizar la estructura. Recuperando en ';'."); 
        }
    | encabezado_for LBRACE lista_sentencias_ejecutables RBRACE
    {
        addErrorSemicolon("Error Sintactico: Falta ; al final de for (Se procede a ignorarlo)");
        
        /* --- RECUPERACIÓN: CERRAR EL FOR --- */
        ForContext ctx = $1;
        if(!errorfuncion && !errorEnProduccion && ctx != null){
            // 1. Generar incremento/decremento
            PolacaElement opId = PI().generateOperand(ctx.variableControl);
            SymbolEntry unoConst = new SymbolEntry("1", "constante", "int");
            symbolTable.add(unoConst); // Asegurarnos que esté en la tabla
            PolacaElement opUno = PI().generateOperand(unoConst);
            
            String opAritmetico = ctx.esIncremento ? "+" : "-";
            PolacaElement resultado = PI().generateOperation(opId, opUno, opAritmetico, "int");
            PI().generateAssignment(ctx.variableControl, resultado);
            
            // 2. Salto incondicional al inicio (loop)
            List<Integer> biList = PI().generateUnconditionalJump();
            PI().backpatch(biList, ctx.labelInicio);
            
            // 3. Backpatch de la condición falsa (salida)
            int finDelFor = PI().getCurrentAddress();
            PI().backpatch(ctx.listaBF, finDelFor);
            
            System.out.println("Línea " + lexer.getContext().getLine() + ": Fin de FOR generado (Warning).");
        }
    }
    ;

condicion
    : expresion operador_comparacion expresion
        {
            PolacaElement op1 = (PolacaElement)$1;
            String operator = (String)$2; 
            PolacaElement op2 = (PolacaElement)$3;
            
            String resultType = codigointermedio.TypeChecker.checkArithmetic(op1.getResultType(), op2.getResultType());

            if (resultType.equals("error")) {
                yyerror("Tipos incompatibles: Comparación lógica inválida entre '" + op1.getResultType() + "' y '" + op2.getResultType() + "'.", true);
                $$ = PI().generateErrorElement("boolean"); 
            } else {
                $$ = PI().generateCondition(op1, op2, operator, "boolean"); 
            }
        }
    | expresion expresion
        { 
            error_comparacion= true;
            addError("Error Sintactico: Falta de comparador en comparación.");
        }
    | error operador_comparacion expresion
        { 
            error_comparacion= true;
            addError("Error Sintactico: Falta operando izquierdo en comparación.");
        }
    | expresion operador_comparacion error
        { 
            error_comparacion= true;
            addError("Error Sintactico: Falta operando derecho en comparación.");
        }
    ;

operador_comparacion
    : EQ    { $$ = "=="; }
    | NEQ   { $$ = "=!"; }  
    | LT    { $$ = "<";  }
    | LE_OP { $$ = "<="; }
    | GT    { $$ = ">";  }
    | GE    { $$ = ">="; }
    ;



asignacion
    : identificador_destino ASSIGN_COLON expresion SEMICOLON {
            SymbolEntry destino = (SymbolEntry)$1;
            PolacaElement fuente = (PolacaElement)$3;
            if(!errorfuncion){
            if (errorEnProduccion) {
                        errorEnProduccion = false; 
                    } else {
                    if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                        yyerror("Error Semantico: El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                    } else if(destino.getUso().equals("variable")){
                        if (destino.getTipo().equals("untype")){
                            // Si era untype, tomamos el tipo de la fuente
                            destino.setTipo(fuente.getResultType());
                        }
                    }

                    if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                        yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                    }
                    
                    if (!errorEnProduccion) {
                        destino.setValue(fuente.getResultEntry().getValue());
                        symbolTable.updateValue(destino);
                        System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación simple detectada a " + destino.getLexeme());
                        PI().generateAssignment(destino, fuente);
                    }
                }
        }
    }
| lista_variables_destino ASSIGN lista_expresiones SEMICOLON
    {    
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)$3;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)$1;
       if(!errorfuncion && !errorEnProduccion && listaFuentes != null && listaDestinos != null){
            // Reutilizamos la lógica. Para no duplicar tanto código en Java, 
            // idealmente esto debería estar en una función auxiliar en el parser, 
            // pero aquí lo expandimos para que funcione directo en el .y
            
            boolean esInvocacionMultiple = false;
            if (listaFuentes.size() == 1) {
                PolacaElement fuente = listaFuentes.get(0);
                SymbolEntry symFuente = fuente.getResultEntry(); 
                if (symFuente != null && "funcion".equals(symFuente.getUso()) 
                    && symFuente.getTiposRetorno() != null && !symFuente.getTiposRetorno().isEmpty()){
                        esInvocacionMultiple = true;
                }
                // ... (lógica de detección de función origen temporal omitida por brevedad, es igual a la regla principal) ...
                else if (symFuente != null && symFuente.getFuncionOrigen() != null) {
                        SymbolEntry funcionReal = symFuente.getFuncionOrigen();
                        if (funcionReal.getTiposRetorno() != null && !funcionReal.getTiposRetorno().isEmpty()) {
                            esInvocacionMultiple = true;
                            symFuente = funcionReal;
                        }
                }
                
                if (esInvocacionMultiple) {
                    // Lógica de función múltiple
                    List<String> tiposRetorno = symFuente.getTiposRetorno();
                    int numVars = listaDestinos.size();     
                    int numRets = tiposRetorno.size();  
                    
                    if (numRets >= numVars) {
                        for (int i = 0; i < numVars; i++) {
                            SymbolEntry destino = listaDestinos.get(i);
                            String tipoRetorno = tiposRetorno.get(i);
                            if ("untype".equals(destino.getTipo())) destino.setTipo(tipoRetorno);
                            
                            if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), tipoRetorno)) {
                                PI().generateAssignment(destino, fuente);
                            }
                        }
                    }
                }
            }
            
            if (!esInvocacionMultiple) {
                // Asignación estándar A,B = C,D
                if (listaDestinos.size() == listaFuentes.size()) {
                    for (int i = 0; i < listaDestinos.size(); i++) {
                        SymbolEntry destino = listaDestinos.get(i);
                        PolacaElement fuente = listaFuentes.get(i);
                        
                        if ("untype".equals(destino.getTipo())){
                            destino.setTipo(fuente.getResultType());
                        }
                        
                        if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                            PI().generateAssignment(destino, fuente);
                        }
                        else{
                             yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                        }
                    }
                }
            }
            System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación múltiple recuperada.");
        }
        
    }
    | identificador_destino ASSIGN_COLON expresion %prec PRIORIDAD_BAJA_ASIGNACION
        { 
            addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la asignación.(Se procede a ignorarlo)");
            SymbolEntry destino = (SymbolEntry)$1;
            PolacaElement fuente = (PolacaElement)$3;
        
        if(!errorfuncion){
            if (errorEnProduccion) {
                        errorEnProduccion = false; 
                    } else {
                    if (destino.getUso() == null && !destino.getUso().equals("variable")) {
                        yyerror("Error Semantico: El identificador '" + destino.getLexeme() + "' no es una variable.", true);
                    } else if(destino.getUso().equals("variable")){
                        if (destino.getTipo().equals("untype")){
                            // Si era untype, tomamos el tipo de la fuente
                            destino.setTipo(fuente.getResultType());
                        }
                    }

                    if (!codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                        yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                    }
                    
                    if (!errorEnProduccion) {
                        destino.setValue(fuente.getResultEntry().getValue());
                        symbolTable.updateValue(destino);
                        System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación simple detectada a " + destino.getLexeme());
                        PI().generateAssignment(destino, fuente);
                    }
                }
        }
            
        }
    | lista_variables_destino ASSIGN lista_expresiones %prec PRIORIDAD_BAJA_ASIGNACION
    { 
        addErrorSemicolon("Error Sintáctico: Falta punto y coma ';' al final de la asignación múltiple.(Se procede a ignorarlo)");
        
        /* --- RECUPERACIÓN: ASIGNACIÓN MÚLTIPLE --- */
        ArrayList<PolacaElement> listaFuentes = (ArrayList<PolacaElement>)$3;
        ArrayList<SymbolEntry> listaDestinos = (ArrayList<SymbolEntry>)$1;
        
        if(!errorfuncion && !errorEnProduccion && listaFuentes != null && listaDestinos != null){
            // Reutilizamos la lógica. Para no duplicar tanto código en Java, 
            // idealmente esto debería estar en una función auxiliar en el parser, 
            // pero aquí lo expandimos para que funcione directo en el .y
            
            boolean esInvocacionMultiple = false;
            if (listaFuentes.size() == 1) {
                PolacaElement fuente = listaFuentes.get(0);
                SymbolEntry symFuente = fuente.getResultEntry(); 
                if (symFuente != null && "funcion".equals(symFuente.getUso()) 
                    && symFuente.getTiposRetorno() != null && !symFuente.getTiposRetorno().isEmpty()){
                        esInvocacionMultiple = true;
                }
                // ... (lógica de detección de función origen temporal omitida por brevedad, es igual a la regla principal) ...
                else if (symFuente != null && symFuente.getFuncionOrigen() != null) {
                        SymbolEntry funcionReal = symFuente.getFuncionOrigen();
                        if (funcionReal.getTiposRetorno() != null && !funcionReal.getTiposRetorno().isEmpty()) {
                            esInvocacionMultiple = true;
                            symFuente = funcionReal;
                        }
                }
                
                if (esInvocacionMultiple) {
                    // Lógica de función múltiple
                    List<String> tiposRetorno = symFuente.getTiposRetorno();
                    int numVars = listaDestinos.size();     
                    int numRets = tiposRetorno.size();  
                    
                    if (numRets >= numVars) {
                        for (int i = 0; i < numVars; i++) {
                            SymbolEntry destino = listaDestinos.get(i);
                            String tipoRetorno = tiposRetorno.get(i);
                            if ("untype".equals(destino.getTipo())) destino.setTipo(tipoRetorno);
                            
                            if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), tipoRetorno)) {
                                PI().generateAssignment(destino, fuente);
                            }
                            
                        }
                    }
                }
            }
            
            if (!esInvocacionMultiple) {
                // Asignación estándar A,B = C,D
                if (listaDestinos.size() == listaFuentes.size()) {
                    for (int i = 0; i < listaDestinos.size(); i++) {
                        SymbolEntry destino = listaDestinos.get(i);
                        PolacaElement fuente = listaFuentes.get(i);
                        
                        if ("untype".equals(destino.getTipo())){
                            destino.setTipo(fuente.getResultType());
                        }
                        
                        if (codigointermedio.TypeChecker.checkAssignment(destino.getTipo(), fuente.getResultType())) {
                            PI().generateAssignment(destino, fuente);
                        }
                        else{
                             yyerror("Error Semantico: Tipos incompatibles: No se puede asignar '" + fuente.getResultType() + 
                                "' a la variable '" + destino.getTipo() + "'.", true);
                        }
                    }
                }
            }
            System.out.println("Línea " + lexer.getContext().getLine() + ": Asignación múltiple recuperada.");
        }
        yyerrflag = 0; 
    } ;

lista_expresiones
    : expresion
           {List<PolacaElement> list = new ArrayList<>();
            list.add((PolacaElement)$1);
            $$ = list;
        }
    | lista_expresiones COMMA expresion{
            @SuppressWarnings("unchecked")
            List<PolacaElement> lista = (List<PolacaElement>)$1;
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.add($3);
            $$ = lista; 
    }
| lista_expresiones expresion
        {
            // 1. Reportamos el error
            addError("Error Sintáctico: Falta coma ',' entre las expresiones del lado derecho.");
            
            // 2. RECUPERACIÓN:
            @SuppressWarnings("unchecked")
            List<PolacaElement> lista = (List<PolacaElement>)$1;
            if (lista == null) lista = new ArrayList<>();
            
            // ¡OJO! Ahora la expresión es $2, porque quitamos el token 'error' del medio
            lista.add((PolacaElement)$2); 
            
            // 3. Retorno
            $$ = lista;
        }
    ;

expresion
    : termino 
        { $$ = $1; }
    | expresion PLUS termino
        {   
            PolacaElement elem1 = $1;
            PolacaElement elem2 = $3;
            if (!errorfuncion){
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Error Semantico: Tipos incompatibles para la suma " + "elem1" + ": " + elem1.getResultType() + "elem2" + ": " + elem2.getResultType(), true);
                $$ = PI().generateErrorElement("error"); 
            } else {
                $$ = PI().generateOperation(elem1, elem2, "+", tipoResultante);
            }
        }
        }
    | expresion PLUS error
        { 
            addError("Falta de operando en expresión.");
        }
    | expresion MINUS termino
            {
            PolacaElement elem1 = $1;
            PolacaElement elem2 = $3;
            if (!errorfuncion){
            String tipoResultante = TypeChecker.checkArithmetic(elem1.getResultType(), elem2.getResultType());
            if (tipoResultante.equals("error")) {
                yyerror("Error Semantico: Tipos incompatibles para la resta " + "elem1" + ": " + elem1.getResultType() + "elem2" + ": " + elem2.getResultType(), true);
                $$ = PI().generateErrorElement("error"); 
            } else {
                $$ = PI().generateOperation(elem1, elem2, "-", tipoResultante);
            }
            }
        }
    | expresion MINUS error
        { 
            addError("Falta de operando en expresión.");
        }
    ;

termino
    : factor
    {
        $$ = $1;
    }
    | termino STAR factor{
        PolacaElement term = $1;
        PolacaElement fact = $3;
        if (!errorfuncion){
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Error Semantico: Tipos incompatibles para la multiplicación " + "elem1" + ": " + term.getResultType() + "elem2" + ": " + fact.getResultType(), true);
            $$ = PI().generateErrorElement("error"); 
        } else {
            $$ = PI().generateOperation(term, fact, "*", tipoResultante);
        }
        }
    }
    | termino STAR error
        { 
            addError("Falta de operando en término.");
        }
    | termino SLASH factor
    {
        PolacaElement term = $1;
        PolacaElement fact = $3;
        if (!errorfuncion){
        String tipoResultante = TypeChecker.checkArithmetic(term.getResultType(), fact.getResultType());
        if (tipoResultante.equals("error")) {
            yyerror("Error Semantico: Tipos incompatibles para la división  " + "elem1" + ": " + term.getResultType() + "elem2" + ": " + fact.getResultType(), true);
            $$ = PI().generateErrorElement("error"); 
        } else {
            $$ = PI().generateOperation(term, fact, "/", tipoResultante);
        }
        }
    }
    | termino SLASH error
        { 
            addError("Falta de operando en término.");
        }
    
    ;

factor
    : identificador_completo
        { 
            
            SymbolEntry entradaParser = (SymbolEntry)$1;
            String lexema = entradaParser.getLexeme();
            if (!errorfuncion){
            if(lexema.equals("ERROR_IGNORE")){
                $$=PI().generateErrorElement("error");
            }
            else{
                String prefijo = entradaParser.getScopePrefix();
            SymbolEntry entry = null;

            // --- LÓGICA DE BÚSQUEDA (Igual que en identificador_destino) ---
            if (prefijo != null) {
                entry = symbolTable.lookup(entradaParser.getLexeme(), prefijo);
                if (entry == null) yyerror("Error Semantico: Variable '" + entradaParser.getLexeme() + "' no encontrada en ámbito '" + prefijo + "'.", true);
            } else {
                entry = symbolTable.lookup(entradaParser.getLexeme());
                if (entry == null) yyerror("Error Semantico: Variable '" + entradaParser.getLexeme() + "' no declarada.", true);
            }
            
            // Generación de error o código
            if (entry == null) {
                 $$ = PI().generateErrorElement("error");
            } else if (entry.getUso().equals("funcion")) {
                 yyerror("Error Semantico: Uso incorrecto de función como variable.", true);
                 $$ = PI().generateErrorElement("error");
            } else {
                 $$ = PI().generateOperand(entry);
            }
        }
    }
        }
| INT16 
        {
            // CORRECCIÓN: Capturar y añadir la constante a la TS
            SymbolEntry se_const = (SymbolEntry)$1;
            symbolTable.add(se_const);
            $$ = PI().generateOperand($1);
        }
| FLOAT32 
        { 
            // CORRECCIÓN: Capturar y añadir la constante a la TS
            SymbolEntry se_const = (SymbolEntry)$1;
            symbolTable.add(se_const);
            $$ = PI().generateOperand($1);
        }
| STRING  
      { 
          // CORRECCIÓN: Capturar y añadir la constante a la TS
          SymbolEntry se_const = (SymbolEntry)$1;
          symbolTable.add(se_const);
          $$ = PI().generateOperand($1); 
      }
| invocacion_funcion
        { $$ = $1; } 
| conversion_tof
        { $$ = $1; } 
    ;


invocacion_funcion
    : ID LPAREN parametros_reales RPAREN 
    {   
        SymbolEntry funcion = (SymbolEntry)$1;
        
        // 1. Validar que sea una función
        if (!"funcion".equals(funcion.getUso())) {
            yyerror("Error Semantico; El identificador '" + funcion.getLexeme() + "' no es una función.", true);
            $$ = PI().generateErrorElement("error");
        } else {
            @SuppressWarnings("unchecked")
            ArrayList<ParametroInvocacion> reales = (ArrayList<ParametroInvocacion>)$3;
            if (reales == null) reales = new ArrayList<>();

            List<SymbolEntry> formales = funcion.getParametros(); // Lista ordenada de params formales
            
            // 2. Validar Cantidad
            if (reales.size() != formales.size()) {
                 yyerror("Error Semantico: La función '" + funcion.getLexeme() + "' espera " + formales.size() + 
                         " parámetros, pero se recibieron " + reales.size() + ".", true);
            }
            
            for (SymbolEntry formal : formales) {
                boolean encontrado = false;
                ParametroInvocacion paramRealMatch = null;

                // Buscamos en la lista desordenada de la invocación
                for (ParametroInvocacion real : reales) {
                    if (real.getNombreDestino().equals(formal.getLexeme())) {
                        encontrado = true;
                        paramRealMatch = real;
                        break;
                    }
                }

                if (!encontrado) {
                    yyerror("Erro Semantico: Falta el parámetro obligatorio '" + formal.getLexeme() + 
                            "' en la invocación de '" + funcion.getLexeme() + "'.", true);
                } else {

                    if ("string".equals(paramRealMatch.getValor().getResultType())) {
                    yyerror("Error Semántico: No se permite pasar cadenas de caracteres (STRING) como parámetros a funciones.", true);
                    }
                    
                    if (!codigointermedio.TypeChecker.checkAssignment(formal.getTipo(), paramRealMatch.getValor().getResultType())) {
                         yyerror("Error Semantico: Error de Tipo en parámetro '" + formal.getLexeme() + 
                                 "'. Se esperaba '" + formal.getTipo() + 
                                 "' pero se recibió '" + paramRealMatch.getValor().getResultType() + "'.", true);
                    }
                    
                   PI().generateAssignment(formal, paramRealMatch.getValor());
                }
            }

            // 6. Generar la llamada a la función (CALL)
            $$ = PI().generateCall(funcion); 

            for (SymbolEntry formal : formales) {
                ParametroInvocacion paramRealMatch = null;
                for (ParametroInvocacion real : reales) {
                    if (real.getNombreDestino().equals(formal.getLexeme())) {
                        paramRealMatch = real;
                        break;
                    }
                }

                if (paramRealMatch != null) {
                    String mecanismo = formal.getMecanismoPasaje(); 
                    
                    if ("cr".equals(mecanismo) || "cvr".equals(mecanismo)) {
                        
                        SymbolEntry realSym = paramRealMatch.getValor().getResultEntry();
                        
                        if (realSym != null && "variable".equals(realSym.getUso())) {
                            
                            PolacaElement valorFormal = new PolacaElement(formal.getTipo(), formal);
                            PI().generateAssignment(realSym, valorFormal);
                            
                            if (!errorEnProduccion) {
                                System.out.println("   -> Copia-Resultado (Tema 26): " + realSym.getLexeme() + " actualizado con " + formal.getLexeme());
                            }
                        } else {
                             yyerror("Error Semántico: El parámetro '" + formal.getLexeme() + 
                                     "' es de tipo Copia-Resultado, por lo que el parámetro real debe ser una variable, no una constante o expresión.", true);
                        }
                    }
                }
            }
            
            if (!errorEnProduccion) {
                System.out.println("Línea " + lexer.getContext().getLine() + ": Invocación correcta a " + funcion.getLexeme());
            }
        }
    }
    ;

conversion_tof
    : TOF LPAREN expresion RPAREN
        { 
            PolacaElement expr = (PolacaElement)$3;
            
            if (!expr.getResultType().equals("int")) {
                 yyerror("Error Semantico: Se intentó convertir a float una expresión que es de tipo '" + expr.getResultType() + "'.", true);
                 $$ = expr; 
            } else {
                $$ = PI().generateTOF(expr);
                if (!errorEnProduccion) {
                    System.out.println("Línea " + lexer.getContext().getLine() + ":  Conversión explícita TOF generada correctamente."); 
                }
            }
        }
    | TOF error expresion RPAREN 
        { 
            addError("Error Sintactico: Falta el '(' en la conversión explícita.");
        }
    | TOF LPAREN expresion error 
        { 
            addError("Error Sintactico: Falta el ')' en la conversión explícita.");
        }
    ;

parametros_reales
    : parametro_real
    {
        ArrayList<ParametroInvocacion> lista = new ArrayList<>();
        lista.add($1);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
    | parametros_reales COMMA parametro_real
    {
        @SuppressWarnings("unchecked")
        ArrayList<ParametroInvocacion> lista = (ArrayList<ParametroInvocacion>)$1;
        lista.add($3);
        yyval = new ParserVal();
        yyval.listParamInv = lista;
    }
    ;

parametro_real
    : expresion ARROW ID
    {
        PolacaElement expr = (PolacaElement)$1;
        SymbolEntry idParam = (SymbolEntry)$3;
        
        $$ = new ParametroInvocacion(idParam.getLexeme(), expr);
        
        if (!errorEnProduccion) { 
            System.out.println("   -> Parametro nombrado detectado: " + idParam.getLexeme());
        }
    }
    | expresion error
    { 
        addError("Error Sintactico: declaracion incorrecta del parámetro real. Se espera 'valor -> nombre'.");
        $$ = new ParametroInvocacion("error", (PolacaElement)$1); // Dummy para no romper todo
    }
    ;

lambda_prefix
    : LPAREN tipo ID RPAREN
    { 
        SymbolEntry tipoFormal = (SymbolEntry)$2;
        SymbolEntry formal = (SymbolEntry)$3;
        
        formal.setTipo(tipoFormal.getLexeme());
        formal.setUso("parametro_lambda");

        String uniqueScopeName = "LAMBDA_ANON_" + (++lambdaCounter);
        symbolTable.pushScope(uniqueScopeName);
        symbolTable.add(formal);

        currentLambdaFormal = formal;
        
        lambdaBodyBuffer = new PolacaInversa(symbolTable);
        
        /* Cambiar el gestor activo a la Polaca temporal */
        pilaGestoresPolaca.push(lambdaBodyBuffer);
        
        $$ = formal;
    }
    ;
lambda_expresion
    : lambda_prefix LBRACE if_simple RBRACE LPAREN lambda_argumento RPAREN 
    {
        SymbolEntry formal = currentLambdaFormal;
        PolacaElement real = (PolacaElement)$6; // lambda_argumento
    
        @SuppressWarnings("unchecked")
        List<Integer> listaBF = (List<Integer>)$3; // if_simple retorna la lista
        

        pilaGestoresPolaca.pop(); // Sacamos lambdaBodyBuffer
        
        lambdaAssignBuffer = new PolacaInversa(symbolTable);
        
        /* Generar la asignación en el buffer temporal */
        lambdaAssignBuffer.generateAssignment(formal, real);
        
        
        PolacaInversa mainPI = PI(); // Polaca principal (MAINLAMBDATEST)
        
        /* 4.1: Añadir la asignación primero */
        int offsetAsignacion = mainPI.getCurrentAddress();
        mainPI.appendPolaca(lambdaAssignBuffer);
        
        int offsetCuerpo = mainPI.getCurrentAddress();
        mainPI.appendPolaca(lambdaBodyBuffer);
        
        
        int desplazamientoAsignacion = lambdaAssignBuffer.getSize();
        
        List<Integer> listaBFAjustada = new ArrayList<>();
        for (int bf : listaBF) {
            listaBFAjustada.add(bf + offsetAsignacion + desplazamientoAsignacion - 1);
        }
        
        int targetEndIf = mainPI.getCurrentAddress();
        
        mainPI.backpatch(listaBFAjustada, targetEndIf);
        
        lambdaBodyBuffer = null;
        lambdaAssignBuffer = null;
        currentLambdaFormal = null;
        
        symbolTable.popScope();
        
        if (!errorEnProduccion) {
            System.out.println("Línea " + lexer.getContext().getLine() + 
                ": Lambda correcta para " + formal.getLexeme());
        }
        
        yyval.Polacaelement = new PolacaElement("void");
    }
    | lambda_prefix error if_simple RBRACE 
      LPAREN lambda_argumento RPAREN
    { 
        addError("Error Sintactico: Falta delimitador '{' de apertura en la función Lambda.");
        symbolTable.popScope(); 
        $$ = new PolacaElement("error");
    }


    | lambda_prefix LBRACE if_simple error 
      LPAREN lambda_argumento RPAREN
    { 
        addError("Error Sintactico: Falta delimitador '}' de cierre en la función Lambda.");
        symbolTable.popScope(); 
        $$ = new PolacaElement("error");
    }

    
    | lambda_prefix LBRACE if_simple
     RBRACE 
      LPAREN lambda_argumento error
    { 
        addError("Error Sintactico: Falta el paréntesis de cierre ')' para la invocación Lambda.");
        symbolTable.popScope(); 
        $$ = new PolacaElement("error");
    }
;

lambda_argumento
    : ID
    {
        $$ = PI().generateOperand($1);
    }
    | INT16
    {
        $$ = PI().generateOperand($1);
    }
    | FLOAT32;
    {
        $$ = PI().generateOperand($1);
    }

%%

/* ======= Código Java adicional (opcional) ======= */