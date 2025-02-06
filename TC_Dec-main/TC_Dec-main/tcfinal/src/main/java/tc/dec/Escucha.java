package tc.dec;

import java.util.*;

public class Escucha extends compiladorBaseListener {
    private final TablaSimbolos tablaSimbolos;
    private final List<String> errores = new ArrayList<>();


    public Escucha() {
        tablaSimbolos = new TablaSimbolos();
    }

    // Al entrar en un bloque, agregamos un nuevo contexto
    @Override
    public void enterBloque(compiladorParser.BloqueContext ctx) {
        tablaSimbolos.agregarContexto();
        System.out.println("\n🟢 Entrando a un nuevo contexto...");
        //tablaSimbolos.imprimirTablaSimbolos();
    }

    // Al salir de un bloque, eliminamos el contexto actual
    @Override
    public void exitBloque(compiladorParser.BloqueContext ctx) {
        System.out.println("\n🔴 Saliendo del contexto actual...");
        // Verificar variables no usadas ANTES de eliminar el contexto
        for (TablaSimbolos.Simbolo simbolo : tablaSimbolos.getVariablesEnContextoActual()) {
            if (!simbolo.usada) {
                System.err.println("⚠ Advertencia en línea " + ctx.getStart().getLine() +
                                " → La variable '" + simbolo.nombre + "' fue declarada pero no utilizada.");
            }
        }
        tablaSimbolos.almacenarEstadoTabla(); // Guardamos el estado antes de eliminar el contexto
        tablaSimbolos.eliminarContexto();
    }

    // Declaración de variables
    @Override
    public void enterDeclaracion(compiladorParser.DeclaracionContext ctx) {
        String tipo = ctx.tipo().getText();
        String nombre = ctx.ID().getText();
        boolean inicializada = (ctx.expr() != null);
    
        if (tablaSimbolos.existeEnContextoActual(nombre)) {
            errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                        " → La variable '" + nombre + "' ya está declarada en este contexto.");
            return;
        }
    
        if (ctx.expr() != null) {  // Verificar tipo si hay asignación en la declaración
            String tipoValor = obtenerTipoDeExpresion(ctx.expr());
            if (!esCompatible(tipo, tipoValor)) {
                errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                            " → Asignación incompatible: Se esperaba '" + tipo +
                            "', pero se asignó '" + tipoValor + "'.");
                return;
            }
        }
    
        tablaSimbolos.agregarVariable(nombre, tipo);
        TablaSimbolos.Simbolo variable = tablaSimbolos.buscarSimbolo(nombre);
        variable.inicializada = inicializada;
    }

    @Override
    public void enterAsignacion(compiladorParser.AsignacionContext ctx) {
        String nombre = ctx.ID().getText();
        TablaSimbolos.Simbolo variable = tablaSimbolos.buscarSimbolo(nombre);
        if (variable == null) {
            errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                        " → La variable '" + nombre + "' no está declarada.");
            return;
        }
    
        
    
        // Obtener el tipo esperado de la variable
        String tipoEsperado = variable.tipo;

        // Verificar el tipo del valor asignado
        if (ctx.expr() != null) {
            String tipoValor = obtenerTipoDeExpresion(ctx.expr());
            if (!esCompatible(tipoEsperado, tipoValor)) {
                errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                            " → Asignación incompatible: Se esperaba '" + tipoEsperado +
                            "', pero se asignó '" + tipoValor + "'.");
            }
        }

        variable.inicializada = true; // Marcar como inicializada
    }
    
    private String obtenerTipoDeExpresion(compiladorParser.ExprContext ctx) {
        if (ctx.NUMERO() != null) {
            return ctx.getText().contains(".") ? "float" : "int";
        } else if (ctx.STRING() != null) {
            return "string";
        } else if (ctx.ID() != null) {
            return tablaSimbolos.getTipoDeSimbolo(ctx.ID().getText());
        } else if (ctx.funcionCall() != null) {  
            String nombreFuncion = ctx.funcionCall().ID().getText();
            TablaSimbolos.Simbolo funcion = tablaSimbolos.buscarSimbolo(nombreFuncion);
    
            if (funcion != null && funcion.categoria.equals("funcion")) {
                return funcion.tipo;
            } else {
                errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                            " → La función '" + nombreFuncion + "' no está declarada antes de su uso.");
                return "desconocido";
            }
        } else if (ctx.op != null) {  
            String izquierda = obtenerTipoDeExpresion(ctx.expr(0));
            String derecha = obtenerTipoDeExpresion(ctx.expr(1));
    
            if (!esCompatible(izquierda, derecha)) {
                return "desconocido";
            }
            return izquierda.equals("float") || derecha.equals("float") ? "float" : "int";
        }
        return "desconocido";
    }
    
    

    
    private boolean esCompatible(String esperado, String real) {
        if (esperado.equals(real)) {
            return true;
        }
        return (esperado.equals("float") && real.equals("int")); // Permitir conversión implícita de int → float
    }

    
    @Override
    public void enterExpr(compiladorParser.ExprContext ctx) {
        if (ctx.ID() != null) {
            String nombre = ctx.ID().getText();
            TablaSimbolos.Simbolo variable = tablaSimbolos.buscarSimbolo(nombre);
    
            if (variable == null) {
                errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                            " → La variable '" + nombre + "' no está declarada.");
                return;
            }
    
            if (!variable.inicializada) {
                errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                            " → La variable '" + nombre + "' no está inicializada antes de su uso.");
            }
    
            // Marcar la variable como utilizada
            variable.usada = true;
        }
    }

    @Override
    public void enterFuncionDecl(compiladorParser.FuncionDeclContext ctx) {
        String tipo = ctx.tipo().getText();
        String nombre = ctx.ID().getText();
        List<String> parametros = new ArrayList<>();
    
        // 🚀 REGISTRAR LA FUNCIÓN **ANTES** DE AGREGAR EL CONTEXTO
        tablaSimbolos.agregarFuncion(nombre, tipo, parametros);
    
        // 🚀 Agregar un nuevo contexto para la función
        tablaSimbolos.agregarContexto();
    
        if (ctx.parametros() != null) {
            for (var parametroCtx : ctx.parametros().parametro()) {
                String paramTipo = parametroCtx.tipo().getText();
                String paramNombre = parametroCtx.ID().getText();
                parametros.add(paramTipo);
    
                tablaSimbolos.agregarVariable(paramNombre, paramTipo);
                TablaSimbolos.Simbolo variable = tablaSimbolos.buscarSimbolo(paramNombre);
                variable.inicializada = true;
            }
        }
    
        // 🚀 Actualizar la función con la lista de parámetros
        tablaSimbolos.actualizarFuncion(nombre, parametros);
    }
    
    
    @Override
    public void exitFuncionDecl(compiladorParser.FuncionDeclContext ctx) {
        tablaSimbolos.eliminarContexto();
    } 

    @Override
    public void enterFuncionCall(compiladorParser.FuncionCallContext ctx) {
        String nombreFuncion = ctx.ID().getText();
        TablaSimbolos.Simbolo funcion = tablaSimbolos.buscarSimbolo(nombreFuncion);

        if (funcion == null || !funcion.categoria.equals("funcion")) {
            errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                        " → La función '" + nombreFuncion + "' no está declarada.");
            return;
        }

        // 🚀 Marcar los parámetros como inicializados al llamar la función
        if (ctx.argumentos() != null) {
            List<String> parametrosEsperados = funcion.parametros;
            List<compiladorParser.ExprContext> argumentos = ctx.argumentos().expr();

            if (parametrosEsperados.size() != argumentos.size()) {
                errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                            " → Número incorrecto de argumentos en la llamada a '" + nombreFuncion + "'.");
                return;
            }

            for (int i = 0; i < parametrosEsperados.size(); i++) {
                String tipoEsperado = parametrosEsperados.get(i);
                String tipoArgumento = obtenerTipoDeExpresion(argumentos.get(i));

                if (!esCompatible(tipoEsperado, tipoArgumento)) {
                    errores.add("Error semántico en línea " + ctx.getStart().getLine() +
                                " → Tipo incorrecto en el argumento " + (i+1) + 
                                " de la llamada a '" + nombreFuncion + "'. Se esperaba '" + 
                                tipoEsperado + "', pero se recibió '" + tipoArgumento + "'.");
                }
            }
        }

        funcion.usada = true;
    }

    @Override
    public void exitPrograma(compiladorParser.ProgramaContext ctx) {
        for (TablaSimbolos.Simbolo funcion : tablaSimbolos.getFuncionesNoUsadas()) {
            System.err.println("⚠ Advertencia en línea " + ctx.getStart().getLine() +
                               " → La función '" + funcion.nombre + "' fue declarada pero no utilizada.");
        }
        if (!errores.isEmpty()) {
            System.err.println("\n❌ Errores encontrados:");
            for (String error : errores) {
                System.err.println(error);
            }
            System.exit(1);  // Salir con código de error
        }else{
            System.out.println("\n🔎 Resumen final de la Tabla de Símbolos:");
            tablaSimbolos.almacenarEstadoTabla(); // Guardamos el estado final antes de imprimir
            tablaSimbolos.imprimirHistorialTabla();
        }
    }
}