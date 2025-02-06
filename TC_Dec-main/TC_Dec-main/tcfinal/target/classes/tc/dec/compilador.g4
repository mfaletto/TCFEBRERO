grammar compilador;

@header {
package tc.dec;
}

// -----------------------
// 🎯 1️⃣ Definición de Tokens (Lexer)
// -----------------------
// --- TIPOS DE DATOS ---
tipo : 'int' | 'float' | 'char' | 'void' ;

// --- TOKENS ---
L_LLAVE : '{' ;
R_LLAVE : '}' ;
NUMERO : [0-9]+ ('.' [0-9]+)? ;
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
WS : [ \t\r\n]+ -> skip ;
NUMBER: [0-9]+ ('.' [0-9]+)?;
// Token para cadenas de texto
STRING : '"' (~["\r\n])* '"' ;
COMENTARIO : '//' ~[\r\n]* -> skip ;

// --- OPERACIONES DE INCREMENTO/DECREMENTO ---
incrementoDecremento : ID ('++' | '--') ;

BLOCK_COMMENT: '/*' .*? '*/' -> skip;  // Comentarios multilínea

// -----------------------
// 🎯 2️⃣ Reglas del Parser
// -----------------------

// Regla principal del programa
programa : (instruccion | funcionDecl)* EOF ;

// Instrucciones permitidas dentro de bloques o a nivel global
instruccion : bloque
            | declaracion
            | asignacion
            | expr ';'  // Expresiones aritméticas y lógicas
            | ifStmt
            | forStmt
            | whileStmt
            | returnStmt
            | funcionCall
            ;

// --- BLOQUES Y CONTEXTO ---
bloque : L_LLAVE (instruccion*) R_LLAVE ;

// --- DECLARACIONES Y ASIGNACIONES ---
declaracion : tipo ID ('=' expr)? ';' ;
asignacion  : ID '=' expr ';' ;

// --- EXPRESIONES ---
expr : expr op=('*' | '/' | '%') expr
     | expr op=('+' | '-') expr
     | expr op=('<' | '<=' | '>' | '>=') expr
     | expr op=('==' | '!=') expr
     | expr op=('&&' | '||') expr
     | '(' expr ')'
     | ID
     | NUMERO
     | STRING
     | funcionCall
     ;

// --- ESTRUCTURAS DE CONTROL ---
ifStmt : 'if' '(' expr ')' bloque ( 'else' bloque )? ;

forStmt : 'for' '(' declaracion? expr? ';' incrementoDecremento? ')' bloque ;

whileStmt : 'while' '(' expr ')' bloque ;

// --- RETORNO DE FUNCIONES ---
returnStmt : 'return' expr? ';' ;

// --- FUNCIONES ---
funcionDecl : tipo ID '(' parametros? ')' bloque ;
funcionCall : ID '(' argumentos? ')';

// --- PARÁMETROS Y ARGUMENTOS ---
parametros : parametro (',' parametro)* ;
parametro  : tipo ID ;
argumentos : expr (',' expr)* ;

