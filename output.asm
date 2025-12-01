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
@T1_PRUEBAINTEGRAL DD ?
@T2_PRUEBAINTEGRAL DD ?
@T3_PRUEBAINTEGRAL DD ?
@T4_PRUEBAINTEGRAL DD ?
@T5_PRUEBAINTEGRAL DD ?
@T6_PRUEBAINTEGRAL DD ?
@T7_PRUEBAINTEGRAL DD ?
A_PRUEBAINTEGRAL_LAMBDA_ANON_1 DD ?
@T8_PRUEBAINTEGRAL_LAMBDA_ANON_1 DD ?

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
L_STORE_1