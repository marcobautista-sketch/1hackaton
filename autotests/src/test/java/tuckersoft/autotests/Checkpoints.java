package tuckersoft.autotests;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * Encadena las cinco estrellas: cada una solo se evalua si la anterior paso entera.
 *
 * Sin esto, un fallo en la ★1 produce cuarenta fallos en cascada y el alumno no sabe
 * por donde empezar. Con esto ve un unico fallo real y el resto como "no evaluado".
 */
final class Checkpoints implements BeforeAllCallback, BeforeEachCallback, TestWatcher, AfterAllCallback {

    private static boolean sondeada = false;
    private static String claseRota = null;

    @Override
    public void beforeAll(ExtensionContext contexto) {
        Checkpoint cp = contexto.getRequiredTestClass().getAnnotation(Checkpoint.class);
        Estado.iniciar(cp.value(), cp.nombre());

        if (!sondeada) {
            sondeada = true;
            // El SMTP de pruebas escucha desde el principio: las decisiones de las
            // estrellas anteriores a la ★5 tambien disparan correos.
            Buzon.arrancar();
            try {
                // Una peticion cualquiera: solo interesa que algo responda.
                Api.get("/api/v1/users/me", null);
            } catch (Api.SinRespuesta e) {
                Estado.appCaida(e.getMessage());
            }
        }
        if (Estado.estaCaida()) {
            // En la primera estrella se falla de verdad: si solo se saltaran los tests,
            // Maven terminaria en BUILD SUCCESS con 0 ejecutados y el equipo creeria
            // que va bien. En las demas se omite, para no repetir el mismo error cinco veces.
            if (cp.value() == 1) {
                throw new AssertionError("\n\n  ✘ " + Estado.PRIMER_FALLO.get(0) + "\n");
            }
            Assumptions.assumeFalse(true, "La aplicacion no responde");
        }

        if (cp.value() > 1) {
            Assumptions.assumeTrue(Estado.aprobado(cp.value() - 1),
                    "★" + (cp.value() - 1) + " no paso: esta estrella no se evalua");
        }
    }

    /**
     * Una vez que un test de la clase falla, el resto se aborta.
     *
     * Sin esto el alumno ve el fallo real seguido de cinco fallos derivados
     * ("no existe la decision con id 0") que no le dicen nada y le hacen perder
     * el tiempo buscando en el sitio equivocado.
     */
    @Override
    public void beforeEach(ExtensionContext contexto) {
        String clase = contexto.getRequiredTestClass().getName();
        Assumptions.assumeFalse(clase.equals(claseRota),
                "Se omite: ya fallo una comprobacion anterior de esta estrella");
    }

    @Override
    public void testFailed(ExtensionContext contexto, Throwable causa) {
        claseRota = contexto.getRequiredTestClass().getName();
        registrarFallo(contexto, causa);
    }

    @Override
    public void testAborted(ExtensionContext contexto, Throwable causa) {
        // Abortado por una suposicion: no cuenta como fallo del equipo.
    }

    @Override
    public void afterAll(ExtensionContext contexto) {
        Checkpoint cp = contexto.getRequiredTestClass().getAnnotation(Checkpoint.class);
        if (Estado.estaCaida()) return;
        if (cp.value() > 1 && !Estado.aprobado(cp.value() - 1)) return;
        Estado.cerrar(cp.value());
    }

    private void registrarFallo(ExtensionContext contexto, Throwable causa) {
        Checkpoint cp = contexto.getRequiredTestClass().getAnnotation(Checkpoint.class);
        String nombre = Optional.ofNullable(contexto.getDisplayName()).orElse("test");
        Estado.marcarFallo(cp.value(), nombre, causa);
    }
}
