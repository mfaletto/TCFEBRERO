package tc.dec;

import java.util.*;

public class Caminante extends compiladorBaseVisitor<String> {
    private final List<String> codigoIntermedio = new ArrayList<>();
    private int tempCount = 0;
    private int etiquetaCount = 0;

    private String nuevaEtiqueta() {
        return "L" + (etiquetaCount++);
    }
    
    private String nuevaTemporal() {
        return "t" + (tempCount++);
    }

    public List<String> getCodigoIntermedio() {
        return codigoIntermedio;
    }

    @Override
    public String visitPrograma(compiladorParser.ProgramaContext ctx) {
        System.out.println("🏁 Iniciando generación de código...");
    
        // 🚀 Lista de variables globales
        Set<String> variablesGlobales = new HashSet<>();
    
        // 🚀 Procesar declaraciones globales
        for (compiladorParser.InstruccionContext inst : ctx.instruccion()) {
            if (inst.declaracion() != null) {
                String var = inst.declaracion().ID().getText();
                variablesGlobales.add(var);
                visit(inst.declaracion()); // Procesar la variable global
            }
        }
    
        // 🚀 Luego visitar todas las funciones
        for (compiladorParser.FuncionDeclContext func : ctx.funcionDecl()) {
            visit(func);
        }
    
        // 🚀 Agregar el punto de entrada `main:`
        if (!codigoIntermedio.contains("main:")) {
            codigoIntermedio.add("main:");
            System.out.println("🚀 Generando etiqueta 'main:'...");
        }
    
        // 🚀 Visitar las instrucciones dentro de main, excluyendo globales
        for (compiladorParser.InstruccionContext inst : ctx.instruccion()) {
            if (inst.declaracion() != null) {
                String var = inst.declaracion().ID().getText();
                if (!variablesGlobales.contains(var)) {
                    visit(inst);
                }
            } else {
                visit(inst);
            }
        }
    
        return null;
    }
    
    

    // 🚀 Generación de código para expresiones matemáticas
    @Override
    public String visitExpr(compiladorParser.ExprContext ctx) {
        if (ctx.op != null) {  // Es una operación matemática o lógica
            String izquierda = visit(ctx.expr(0));
            String derecha = visit(ctx.expr(1));
            String operador = ctx.op.getText();

            String temp = nuevaTemporal();
            codigoIntermedio.add(temp + " = " + izquierda + " " + operador + " " + derecha);
            System.out.println("✅ Código generado: " + temp + " = " + izquierda + " " + operador + " " + derecha);
            return temp;
        } else if (ctx.ID() != null) {
            return ctx.ID().getText();
        } else if (ctx.NUMERO() != null) {
            return ctx.NUMERO().getText();
        }
        return null;
    }

    // 🚀 Generación de código para llamadas a funciones
    @Override
    public String visitFuncionCall(compiladorParser.FuncionCallContext ctx) {
        String nombreFuncion = ctx.ID().getText();
        List<String> parametros = new ArrayList<>();
    
        if (ctx.argumentos() != null) {
            for (compiladorParser.ExprContext argumento : ctx.argumentos().expr()) {
                String valorArg = visit(argumento);
                parametros.add(valorArg);
                codigoIntermedio.add("param " + valorArg);
            }
        }
    
        String temp = nuevaTemporal();
        codigoIntermedio.add(temp + " = call " + nombreFuncion + ", " + parametros.size());
        System.out.println("📞 Llamada generada: " + temp + " = call " + nombreFuncion + ", " + parametros.size());
    
        return temp;
    }
    
    

    @Override
    public String visitFuncionDecl(compiladorParser.FuncionDeclContext ctx) {
        String nombreFuncion = ctx.ID().getText();
        
        codigoIntermedio.add(nombreFuncion + ":"); // 🚀 Agregar etiqueta de función
        System.out.println("📌 Generando función: " + nombreFuncion);
        
        visit(ctx.bloque()); // Generar el cuerpo de la función
        
        return null;
    }

    // 🚀 Generación de código para asignaciones
    @Override
    public String visitAsignacion(compiladorParser.AsignacionContext ctx) {
        System.out.println("📝 Generando código para asignación...");
        String variable = ctx.ID().getText();
        String valor = visit(ctx.expr());

        if (valor != null) {
            codigoIntermedio.add(variable + " = " + valor);
            System.out.println("✅ Código generado: " + variable + " = " + valor);
        } else {
            System.out.println("⚠️ Advertencia: No se generó código para la asignación de " + variable);
        }

        return null;
    }

    @Override
    public String visitDeclaracion(compiladorParser.DeclaracionContext ctx) {
        String variable = ctx.ID().getText();
        String valor = "null";
    
        if (ctx.expr() != null) {
            // 🚀 Verificar si la expresión contiene una llamada a función
            if (ctx.expr().getChild(0) instanceof compiladorParser.FuncionCallContext) {
                valor = visitFuncionCall((compiladorParser.FuncionCallContext) ctx.expr().getChild(0));
            } else {
                valor = visit(ctx.expr());
            }
        }
    
        codigoIntermedio.add(variable + " = " + valor);
        System.out.println("✅ Declaración generada: " + variable + " = " + valor);
    
        return null;
    }


    @Override
    public String visitReturnStmt(compiladorParser.ReturnStmtContext ctx) {
        String valorRetorno = visit(ctx.expr());
        codigoIntermedio.add("return " + valorRetorno);
        System.out.println("📤 Generando return: return " + valorRetorno);
        return valorRetorno;
    }

    @Override
    public String visitIfStmt(compiladorParser.IfStmtContext ctx) {
        String cond = visit(ctx.expr()); // Evaluar la condición
        String labelIf = nuevaEtiqueta();
        String labelElse = nuevaEtiqueta();
        String labelEnd = nuevaEtiqueta();

        codigoIntermedio.add("if " + cond + " goto " + labelIf);
        codigoIntermedio.add("goto " + labelElse);

        // 🚀 Bloque del if
        codigoIntermedio.add(labelIf + ":");
        visit(ctx.bloque(0));  // Visitamos el bloque del if
        codigoIntermedio.add("goto " + labelEnd); // Salto al final

        // 🚀 Bloque del else (si existe)
        codigoIntermedio.add(labelElse + ":");
        if (ctx.bloque().size() > 1) {
            visit(ctx.bloque(1));  // Visitamos el bloque del else
        }

        // 🚀 Etiqueta de salida
        codigoIntermedio.add(labelEnd + ":");

        return null;
    }

    @Override
    public String visitWhileStmt(compiladorParser.WhileStmtContext ctx) {
        String labelInicio = nuevaEtiqueta();
        String labelFin = nuevaEtiqueta();

        // 🚀 Etiqueta de inicio del while
        codigoIntermedio.add(labelInicio + ":");

        // 🚀 Evaluar condición
        String cond = visit(ctx.expr());
        codigoIntermedio.add("if not " + cond + " goto " + labelFin);

        // 🚀 Cuerpo del while
        visit(ctx.bloque());

        // 🚀 Volver a evaluar la condición
        codigoIntermedio.add("goto " + labelInicio);
        codigoIntermedio.add(labelFin + ":");

        return null;
    }

    @Override
    public String visitForStmt(compiladorParser.ForStmtContext ctx) {
        String labelInicio = nuevaEtiqueta();
        String labelFin = nuevaEtiqueta();
    
        // 🚀 Inicialización (solo soporta declaración)
        if (ctx.declaracion() != null) {
            visit(ctx.declaracion());
        }
    
        // 🚀 Etiqueta de inicio
        codigoIntermedio.add(labelInicio + ":");
    
        // 🚀 Evaluar condición
        if (ctx.expr() != null) {
            String cond = visit(ctx.expr());
            codigoIntermedio.add("if not " + cond + " goto " + labelFin);
        }
    
        // 🚀 Cuerpo del for
        visit(ctx.bloque());
    
        // 🚀 Incremento/Decremento
        if (ctx.incrementoDecremento() != null) {
            visit(ctx.incrementoDecremento());
        }
    
        // 🚀 Volver a evaluar la condición
        codigoIntermedio.add("goto " + labelInicio);
        codigoIntermedio.add(labelFin + ":");
    
        return null;
    }
    
    
    @Override
    public String visitIncrementoDecremento(compiladorParser.IncrementoDecrementoContext ctx) {
        String variable = ctx.ID().getText();
        String operador = ctx.getChild(1).getText(); // Puede ser `++` o `--`

        String temp = nuevaTemporal();
        if (operador.equals("++")) {
            codigoIntermedio.add(temp + " = " + variable + " + 1");
        } else {
            codigoIntermedio.add(temp + " = " + variable + " - 1");
        }

        codigoIntermedio.add(variable + " = " + temp);
        return null;
    }


    // 🚀 Guardar código en archivo y mostrar en consola
    public void guardarCodigoIntermedio() {
        System.out.println("\n🔹 Código Intermedio Generado:");
        if (codigoIntermedio.isEmpty()) {
            System.err.println("⚠️ ERROR: No se generó código intermedio.");
            return;
        }

        for (String instr : codigoIntermedio) {
            System.out.println(instr);
        }
    }
}
