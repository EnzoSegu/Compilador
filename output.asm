; --- Encabezado y Directivas (MASM 32-bit) ---
.386
.model flat, stdcall
option casemap:none
include \masm32\include\windows.inc
include \masm32\include\kernel32.inc
includelib \masm32\lib\kernel32.lib
include \masm32\include\masm32.inc
includelib \masm32\lib\masm32.lib

; --- Importar Rutinas Externas (Asumiendo que existen) ---
extern _print_float:PROC
extern _print_string:PROC
extern _print_int:PROC

.DATA
; Variables Auxiliares (DD ?) y Constantes
A_PRUEBAINTEGRAL_LAMBDA_ANON_1 DD ?
B_PRUEBAINTEGRAL DD ?
RESULTADO_PRUEBAINTEGRAL DD ?
CONTADOR_PRUEBAINTEGRAL DD ?
FLOTANTE_PRUEBAINTEGRAL DD ?
CTE_10I         DD 10I
CTE_5I          DD 5I
CTE_____INICIO_DE_PRUEBAS____ DB "-- INICIO DE PRUEBAS --", 0
CTE_A_vale_(10)_ DB " vale (10)", 0
@T1_PRUEBAINTEGRAL DD ?
CTE_Correcto__A_es_mayor_que_B DB "orrecto: A es mayor que ", 0
CTE_Error__A_no_deberia_ser_menor DB "rror: A no deberia ser meno", 0
@T2_PRUEBAINTEGRAL DD ?
CTE_2_5         DD 2.5
@T3_PRUEBAINTEGRAL DD ?
CTE_Prueba_TOF_(12_5)_ DB "rueba TOF (12.5)", 0
CTE_For_Ascendente_(1_to_3)_ DB "or Ascendente (1 to 3)", 0
CTE_1I          DD 1I
CTE_3I          DD 3I
@T4_PRUEBAINTEGRAL DD ?
@T5_PRUEBAINTEGRAL DD ?
CTE_For_Descendente_(3_to_1)_ DB "or Descendente (3 to 1)", 0
@T6_PRUEBAINTEGRAL DD ?
@T7_PRUEBAINTEGRAL DD ?
CTE_Asignacion_Multiple_A,B_=_1,2_ DB "signacion Multiple A,B = 1,2", 0
CTE_Prueba_Lambda_(Imprime_100)_ DB "rueba Lambda (Imprime 100)", 0
A_PRUEBAINTEGRAL_LAMBDA_ANON_1 DD ?
CTE_5I          DD 5I
@T8_PRUEBAINTEGRAL_LAMBDA_ANON_1 DD ?
CTE_A_es_mayor  DB " es mayo", 0
CTE_____FIN_DE_PRUEBAS____ DB "-- FIN DE PRUEBAS --", 0

; Rutinas de error de Runtime
_DIV_CERO       DB "Error en runtime: Division por cero!", 0
_OVERFLOW_FLOAT DB "Error en runtime: Overflow de flotante!", 0
_RECURSION_ERR  DB "Error en runtime: Recursion directa prohibida!", 0

.CODE
_RTH_DIV_CERO:
	PUSH OFFSET _DIV_CERO
	CALL _print_string
	ADD ESP, 4
	JMP _EXIT_PROGRAM
_RTH_OVERFLOW_FLOAT:
	PUSH OFFSET _OVERFLOW_FLOAT
	CALL _print_string
	ADD ESP, 4
	JMP _EXIT_PROGRAM
_RTH_RECURSION_DIRECTA:
	PUSH OFFSET _RECURSION_ERR
	CALL _print_string
	ADD ESP, 4
	JMP _EXIT_PROGRAM

_PRUEBAINTEGRAL_PRUEBAINTEGRAL PROC
	PUSH EBP
	MOV EBP, ESP

	; --- Traduccion Polaca Inversa (PRUEBAINTEGRAL:PRUEBAINTEGRAL) --- 
L_1:	L_2:	L_3:		MOV EAX, _CTE_10I
	MOV _A_PRUEBAINTEGRAL_LAMBDA_ANON_1, EAX
L_4:	L_5:	L_6:		MOV EAX, _CTE_5I
	MOV _B_PRUEBAINTEGRAL, EAX
L_7:	L_8:		PUSH OFFSET _CTE_____INICIO_DE_PRUEBAS____
	CALL _print_string
	ADD ESP, 4
L_9:	L_10:		PUSH OFFSET _CTE_A_vale_(10)_
	CALL _print_string
	ADD ESP, 4
L_11:	L_12:		PUSH _A_PRUEBAINTEGRAL_LAMBDA_ANON_1
	CALL _print_int
	ADD ESP, 4
L_13:	L_14:	L_15:		MOV EAX, _A_PRUEBAINTEGRAL_LAMBDA_ANON_1
	CMP EAX, _B_PRUEBAINTEGRAL
	MOV EAX, 0
	JG L_SET_1_15
	JMP L_STORE_15
L_SET_1_15:
	MOV EAX, 1
L_STORE_15:
	MOV _@T1_PRUEBAINTEGRAL, EAX
L_16:	L_17:		; ASIGNACION REDUNDANTE OMITIDA (Operando no es SymbolEntry)
L_18:	L_19:		; Salto si Falso (BF) a L_23
	MOV EAX, _@T1_PRUEBAINTEGRAL
	CMP EAX, 0
	JE L_23
L_20:	L_21:		PUSH OFFSET _CTE_Correcto__A_es_mayor_que_B
	C