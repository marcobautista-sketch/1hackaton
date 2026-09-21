package tuckersoft.autotests;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;

/**
 * Servidor SMTP de pruebas.
 *
 * Los autotests reciben los correos de verdad en vez de creerse la fila que el equipo
 * guardo en RealityLog. Asi se comprueba el asunto y el cuerpo reales, y un
 * JavaMailSender falso no cuela.
 *
 * Se levanta una sola vez y escucha durante toda la corrida, porque las decisiones de
 * las estrellas anteriores tambien disparan correos.
 */
final class Buzon {

    /** Puerto del servidor de pruebas. El .env del equipo apunta aqui. */
    static final int PUERTO = Integer.getInteger("smtp.port", 2525);

    private static GreenMail servidor;

    static synchronized void arrancar() {
        if (servidor != null) return;
        try {
            ServerSetup config = new ServerSetup(PUERTO, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
            config.setServerStartupTimeout(5000);
            servidor = new GreenMail(config);
            // Acepta cualquier usuario y contrasena: al equipo no le pedimos credenciales
            // reales, pero su cliente si tiene que hablar SMTP de verdad.
            servidor.withConfiguration(
                    com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig()
                            .withDisabledAuthentication());
            servidor.start();
            Runtime.getRuntime().addShutdownHook(new Thread(Buzon::parar));
        } catch (Exception e) {
            servidor = null;
            throw new IllegalStateException(
                    "No se pudo levantar el servidor SMTP de pruebas en el puerto " + PUERTO
                            + ". Si ese puerto esta ocupado, corre los autotests con "
                            + "-Dsmtp.port=OTRO y pon ese mismo puerto en tu .env.", e);
        }
    }

    static synchronized void parar() {
        if (servidor != null) {
            servidor.stop();
            servidor = null;
        }
    }

    /** Espera a que llegue un correo con ese asunto exacto. */
    static MimeMessage esperarPorAsunto(String asunto, int segundos) {
        long limite = System.currentTimeMillis() + segundos * 1000L;
        while (System.currentTimeMillis() < limite) {
            MimeMessage encontrado = buscar(asunto);
            if (encontrado != null) return encontrado;
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    private static MimeMessage buscar(String asunto) {
        if (servidor == null) return null;
        for (MimeMessage m : servidor.getReceivedMessages()) {
            try {
                if (asunto.equals(m.getSubject())) return m;
            } catch (Exception ignorado) {
                // Un mensaje ilegible no es el que buscamos.
            }
        }
        return null;
    }

    static int recibidos() {
        return servidor == null ? 0 : servidor.getReceivedMessages().length;
    }

    /** Asuntos recibidos, para el mensaje de error cuando no aparece el esperado. */
    static String asuntosRecibidos() {
        if (servidor == null || servidor.getReceivedMessages().length == 0) {
            return "(no llego ningun correo al servidor de pruebas)";
        }
        StringBuilder sb = new StringBuilder();
        for (MimeMessage m : servidor.getReceivedMessages()) {
            try {
                sb.append("\n                 · ").append(m.getSubject());
            } catch (Exception ignorado) {
                sb.append("\n                 · (mensaje ilegible)");
            }
        }
        return sb.toString();
    }

    /** Cuerpo del mensaje en texto plano, ya decodificado. */
    static String cuerpo(MimeMessage mensaje) {
        try {
            Object contenido = mensaje.getContent();
            if (contenido instanceof String texto) return texto;
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            mensaje.writeTo(salida);
            return salida.toString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(no se pudo leer el cuerpo: " + e + ")";
        }
    }

    static String destinatario(MimeMessage mensaje) {
        try {
            var destinos = mensaje.getAllRecipients();
            return destinos == null || destinos.length == 0 ? null : destinos[0].toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Buzon() {
    }
}
