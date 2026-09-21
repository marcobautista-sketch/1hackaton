package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Cliente HTTP contra el Branch Engine del equipo. Caja negra: solo habla por HTTP. */
final class Api {

    static final ObjectMapper MAPPER = new ObjectMapper();

    /** URL de la app. Se puede cambiar con -Dbase.url=... o la variable QA_URL. */
    static final String BASE = resolverBase();

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            // HTTP/1.1 explicito: el cliente de Java negocia HTTP/2 por defecto y
            // algunos servidores embebidos cierran esa conexion sin responder.
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static String resolverBase() {
        String url = System.getProperty("base.url");
        if (url == null || url.isBlank()) url = System.getenv("QA_URL");
        if (url == null || url.isBlank()) url = "http://localhost:8080";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Respuesta con el tiempo que tardo: la estrella 5 lo necesita. */
    record Res(int status, JsonNode body, String raw, long millis) {

        JsonNode campo(String nombre) {
            return body == null ? null : body.get(nombre);
        }

        String texto(String nombre) {
            JsonNode n = campo(nombre);
            return n == null || n.isNull() ? null : n.asText();
        }

        Integer numero(String nombre) {
            JsonNode n = campo(nombre);
            return n == null || n.isNull() || !n.isNumber() ? null : n.asInt();
        }

        boolean esNulo(String nombre) {
            JsonNode n = campo(nombre);
            return n == null || n.isNull();
        }

        /** Trozo legible del cuerpo para los mensajes de error. */
        String resumen() {
            if (raw == null || raw.isBlank()) return "(cuerpo vacio)";
            String plano = raw.replaceAll("\\s+", " ").trim();
            return plano.length() > 300 ? plano.substring(0, 300) + "..." : plano;
        }
    }

    /** La app no responde: no tiene sentido seguir evaluando. */
    static final class SinRespuesta extends RuntimeException {
        SinRespuesta(String mensaje) {
            super(mensaje);
        }
    }

    static ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    static Res get(String ruta, String token) {
        return enviar(peticion(ruta, token).GET(), ruta);
    }

    static Res post(String ruta, ObjectNode cuerpo, String token) {
        return post(ruta, cuerpo, token, null);
    }

    static Res post(String ruta, ObjectNode cuerpo, String token, String simulate) {
        HttpRequest.Builder b = peticion(ruta, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString()));
        if (simulate != null) b.header("X-Bandersnatch-Simulate", simulate);
        return enviar(b, ruta);
    }

    static Res patch(String ruta, ObjectNode cuerpo, String token) {
        return enviar(peticion(ruta, token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(cuerpo.toString())), ruta);
    }

    /** POST con una cabecera Authorization cruda, para probar tokens rotos. */
    static Res postConAuthCrudo(String ruta, ObjectNode cuerpo, String authorization) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString()));
        if (authorization != null) b.header("Authorization", authorization);
        return enviar(b, ruta);
    }

    static Res getConAuthCrudo(String ruta, String authorization) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + ruta));
        if (authorization != null) b.header("Authorization", authorization);
        return enviar(b.GET(), ruta);
    }

    private static HttpRequest.Builder peticion(String ruta, String token) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + ruta));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private static Res enviar(HttpRequest.Builder builder, String ruta) {
        HttpRequest peticion = builder.timeout(Duration.ofSeconds(30)).build();
        long inicio = System.nanoTime();
        try {
            HttpResponse<String> respuesta = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            long millis = (System.nanoTime() - inicio) / 1_000_000;
            String raw = respuesta.body();
            JsonNode parsed = null;
            if (raw != null && !raw.isBlank()) {
                try {
                    parsed = MAPPER.readTree(raw);
                } catch (Exception ignorado) {
                    // Un cuerpo que no es JSON tambien es informacion: se reporta en crudo.
                }
            }
            return new Res(respuesta.statusCode(), parsed, raw, millis);
        } catch (ConnectException e) {
            throw new SinRespuesta("""
                    No hay nadie escuchando en %s

                    Levanta tu aplicacion en OTRA terminal antes de correr los autotests:
                        ./mvnw spring-boot:run     (desde la raiz del repositorio)
                    """.formatted(BASE));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new SinRespuesta("Timeout de 30s en " + ruta
                    + ". Tu endpoint se quedo colgado: revisa si algo bloquea el hilo de la peticion.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SinRespuesta("Ejecucion interrumpida");
        } catch (Exception e) {
            throw new SinRespuesta("Fallo de red contra " + BASE + ruta + ": " + e);
        }
    }

    private Api() {
    }
}
