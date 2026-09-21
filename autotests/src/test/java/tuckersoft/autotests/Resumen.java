package tuckersoft.autotests;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Imprime el marcador al final de la corrida.
 *
 * Se registra por ServiceLoader (src/test/resources/META-INF/services). Surefire ya
 * imprime los fallos; esto es lo que el alumno lee primero para saber donde esta.
 */
public final class Resumen implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan plan) {
        int estrellas = Estado.estrellas();

        System.out.println();
        System.out.println("  ──────────────────────────────────────────────────────────────");
        System.out.println("   TUCKERSOFT · CONTROL DE CALIDAD");
        System.out.println("   motor: " + Api.BASE + "        corrida: " + Estado.RUN);
        System.out.println("  ──────────────────────────────────────────────────────────────");

        if (Estado.estaCaida()) {
            System.out.println();
            System.out.println("   ✘  La aplicacion no respondio.");
            System.out.println();
            for (String linea : String.valueOf(Estado.PRIMER_FALLO.get(0)).split("\n")) {
                System.out.println("   " + linea);
            }
            System.out.println("  ──────────────────────────────────────────────────────────────");
            System.out.println();
            return;
        }

        System.out.println();
        System.out.println("   " + "★".repeat(estrellas) + "☆".repeat(5 - estrellas)
                + "   " + estrellas + " / 5   " + veredicto(estrellas));
        System.out.println();

        for (int i = 1; i <= 5; i++) {
            String nombre = Estado.NOMBRES.getOrDefault(i, "?");
            int hechas = Estado.COMPROBACIONES.getOrDefault(i, 0);
            String etiqueta = String.format("★%d  %-12s", i, nombre);

            if (Estado.aprobado(i)) {
                System.out.printf("   ✔  %s %d comprobaciones%n", etiqueta, hechas);
            } else if (Estado.PRIMER_FALLO.containsKey(i)) {
                System.out.printf("   ✘  %s %d comprobaciones antes de fallar%n", etiqueta, hechas);
                System.out.println("        └ " + primeraLinea(Estado.PRIMER_FALLO.get(i)));
            } else {
                System.out.printf("   ·  %s no evaluado%n", etiqueta);
            }
        }

        System.out.println();
        System.out.println("   " + Tablero.reportar(estrellas));
        System.out.println();
        if (estrellas < 5) {
            System.out.println("   Arriba, en la salida de Maven, esta el detalle del fallo:");
            System.out.println("   que se esperaba, que llego y una pista de donde mirar.");
        } else {
            System.out.println("   Las cinco estrellas. Bandersnatch sale para Navidad.");
        }
        System.out.println("  ──────────────────────────────────────────────────────────────");
        System.out.println();
    }

    private static String primeraLinea(String texto) {
        if (texto == null) return "";
        for (String linea : texto.split("\n")) {
            String limpia = linea.strip();
            if (!limpia.isEmpty() && !limpia.startsWith("esperaba") && !limpia.startsWith("recibido")) {
                return limpia.replace("✘", "").strip();
            }
        }
        return texto.strip();
    }

    private static String veredicto(int estrellas) {
        return switch (estrellas) {
            case 0 -> "Nadie va a jugar esto.";
            case 1 -> "Compila, y hasta ahi.";
            case 2 -> "Se juega, con esfuerzo.";
            case 3 -> "Prometedor. Pero se repite.";
            case 4 -> "Casi. Le falta el final.";
            default -> "Cinco estrellas.";
        };
    }
}
