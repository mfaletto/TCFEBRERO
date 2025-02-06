package tc.dec;

import java.util.*;

public class OptimizadorCodigo {
    private final List<String> codigoOptimizado = new ArrayList<>();
    private final Map<String, String> constantes = new HashMap<>();
    private final Map<String, String> aliasVariables = new HashMap<>();
    private final Set<String> variablesUtilizadas = new HashSet<>();

    public List<String> optimizarCodigo(List<String> codigoIntermedio) {
        System.out.println("\n🚀 Iniciando optimización...");
        
        for (String instr : codigoIntermedio) {
            System.out.println("\n🔍 Procesando: " + instr);
            String optimizada = optimizarInstruccion(instr);
            if (optimizada != null) {
                System.out.println("✅ Optimizada: " + optimizada);
                codigoOptimizado.add(optimizada);
            } else {
                System.out.println("❌ Eliminada o optimizada completamente");
            }
        }
        
        return eliminarCodigoMuerto();
    }

    private String optimizarInstruccion(String instr) {
        System.out.println("   ↪️ Estado constantes: " + constantes);
        System.out.println("   ↪️ Estado aliasVariables: " + aliasVariables);
        
        String[] partes = instr.split(" = ");
    
        if (partes.length == 2) {
            String izquierda = partes[0].trim();
            String derecha = partes[1].trim();
    
            // 📌 Propagar valores constantes y alias
            if (constantes.containsKey(derecha)) {
                System.out.println("   🔄 Propagando constante: " + derecha + " -> " + constantes.get(derecha));
                derecha = constantes.get(derecha);
            }
            if (aliasVariables.containsKey(derecha)) {
                System.out.println("   🔄 Propagando alias: " + derecha + " -> " + aliasVariables.get(derecha));
                derecha = aliasVariables.get(derecha);
            }
    
            // 📌 Evaluar expresiones constantes (Ej: `c = 1 - 1` → `c = 0`)
            if (derecha.matches("\\d+ [+-/*] \\d+")) {
                System.out.println("   🧮 Evaluando expresión matemática: " + derecha);
                derecha = String.valueOf(evaluarExpresion(derecha));
            }
    
            // 📌 Si la asignación es redundante, eliminarla
            if (izquierda.equals(derecha)) {
                return null;
            }
    
            // 📌 Guardar alias y constantes
            if (izquierda.startsWith("t")) {
                aliasVariables.put(izquierda, derecha);
                return null; // ❌ Eliminar la variable temporal
            }
            if (derecha.matches("\\d+")) {
                constantes.put(izquierda, derecha);
            }
            
            variablesUtilizadas.add(izquierda);
            return izquierda + " = " + derecha;
        }
    
        // 📌 Optimización de `goto` innecesarios
        if (instr.startsWith("goto ") && !codigoOptimizado.isEmpty() && codigoOptimizado.get(codigoOptimizado.size() - 1).startsWith("goto")) {
            System.out.println("   🚫 Eliminando goto innecesario: " + instr);
            return null;
        }
    
        // 📌 Optimización de `if` con constantes
        if (instr.startsWith("if ")) {
            String[] partesIf = instr.split(" ");
            if (partesIf.length == 4 && constantes.containsKey(partesIf[1])) {
                int valorCondicion = Integer.parseInt(constantes.get(partesIf[1]));
                System.out.println("   🧐 Evaluando if con constante: " + partesIf[1] + " = " + valorCondicion);
                if (valorCondicion == 0) {
                    System.out.println("   ❌ Eliminando if (0), nunca se ejecutará");
                    return null; // ❌ `if 0` se elimina
                } else {
                    System.out.println("   ✅ Reemplazando if (1) con goto " + partesIf[3]);
                    return "goto " + partesIf[3]; // ✅ `if 1` siempre se ejecuta
                }
            }
        }
    
        return instr;
    }

    private List<String> eliminarCodigoMuerto() {
        List<String> resultadoFinal = new ArrayList<>();
        for (String instr : codigoOptimizado) {
            if (instr.contains(" = ")) {
                String variable = instr.split(" = ")[0].trim();
                if (!variablesUtilizadas.contains(variable) && !instr.contains("main")) {
                    continue; // ❌ Eliminar asignaciones de variables no usadas
                }
            }
            resultadoFinal.add(instr);
        }
        return resultadoFinal;
    }
    
    // 🚀 Evaluar expresiones matemáticas básicas (Ej: "5 + 2" → 7)
    private int evaluarExpresion(String expr) {
        String[] partes = expr.split(" ");
        int num1 = Integer.parseInt(partes[0]);
        String operador = partes[1];
        int num2 = Integer.parseInt(partes[2]);

        return switch (operador) {
            case "+" -> num1 + num2;
            case "-" -> num1 - num2;
            case "*" -> num1 * num2;
            case "/" -> num2 != 0 ? num1 / num2 : 0; // Evitar división por cero
            default -> 0;
        };
    }

    // 🚀 Mostrar código optimizado
    public void imprimirCodigoOptimizado() {
        System.out.println("\n🔹 Código Optimizado:");
        for (String instr : codigoOptimizado) {
            System.out.println(instr);
        }
    }
}
