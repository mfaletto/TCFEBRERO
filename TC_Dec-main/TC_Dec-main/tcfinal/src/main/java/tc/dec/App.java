package tc.dec;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {
    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Iniciando el compilador...");

        try {
            // Crear un CharStream a partir del archivo de entrada
            CharStream input = CharStreams.fromFileName("D:\\LAST\\TC_Dec-main\\TC_Dec-main\\tcfinal\\src\\main\\java\\tc\\dec\\3d.txt");

            // Crear el lexer y el parser
            compiladorLexer lexer = new compiladorLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            compiladorParser parser = new compiladorParser(tokens);

            // Configurar el listener de errores personalizados
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine, String msg, RecognitionException e) {

                    if (msg.contains("missing ';'")) {
                        msg = "Error sintáctico en línea " + line + ":" + charPositionInLine + " → Falta un punto y coma.";
                    } else if (msg.contains("missing '('")) {
                        msg = "Error sintáctico en línea " + line + ":" + charPositionInLine + " → Falta apertura de paréntesis.";
                    } else if (msg.contains("extraneous input")) {
                        msg = "Error sintáctico en línea " + line + ":" + charPositionInLine + " → Formato incorrecto en la declaración.";
                    } else if (msg.contains("no viable alternative at input")) {
                        msg = "Error sintáctico en línea " + line + ":" + charPositionInLine +
                              " → Falta apertura de paréntesis o la estructura es inválida.";
                    }

                    throw new RuntimeException(msg);
                }
            });

            // 🚀 1. Etapa de ANÁLISIS con Listener (Escucha)
            ParseTree tree = parser.programa();
            ParseTreeWalker walker = new ParseTreeWalker();
            Escucha escucha = new Escucha();
            walker.walk(escucha, tree);

            // 🚀 2. Etapa de SÍNTESIS con Visitor (Caminante)
            Caminante caminante = new Caminante();
            System.out.println("🚀 Iniciando generación de código intermedio...");
            caminante.visit(tree);
            System.out.println("✅ Finalizó el recorrido del árbol.");
            caminante.guardarCodigoIntermedio();
            List<String> codigoIntermedio = caminante.getCodigoIntermedio();

            // 🚀 Iniciar el proceso de optimización
            OptimizadorCodigo optimizador = new OptimizadorCodigo();
            List<String> codigoOptimizado = optimizador.optimizarCodigo(codigoIntermedio);
            optimizador.imprimirCodigoOptimizado();
            System.out.println("✅ Optimización completada.");

            // 🚀 Guardar código optimizado en archivo
            //guardarCodigoOptimizado(codigoOptimizado, "codigo_optimizado.tac");

            System.out.println("Ejecucion completada sin errores.");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error durante el análisis: " + e.getMessage());
        }
    }
}
