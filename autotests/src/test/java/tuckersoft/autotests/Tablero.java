package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * Publica el resultado de la corrida en el tablero del auditorio.
 *
 * Es informativo: la nota sale de la revision del TA, no de aqui. Por eso nada de lo
 * que pase en esta clase puede afectar a los tests. Si el tablero no responde, si no
 * hay internet o si equipo.json esta a medias, se imprime una linea y ya.
 */
final class Tablero {

    private static final String URL_POR_DEFECTO = "https://hackaton-backend-v1-20262.vercel.app";

    /**
     * Firma de los reportes. Viaja en texto plano dentro de este repositorio, asi que no
     * es un secreto frente a los equipos: solo evita que cualquiera con la URL del
     * tablero publique resultados desde fuera.
     */
    private static final String SECRETO = "tuckersoft-1984-qa";

    private static final List<String> MARCADORES = List.of(
            "g00", "nombre apellido", "codigo utec", "codigo", "");

    /** Devuelve la linea que se imprime en el resumen. */
    static String reportar(int estrellas) {
        try {
            String destino = System.getProperty("tablero.url", URL_POR_DEFECTO);
            if ("off".equalsIgnoreCase(destino)) {
                return "tablero: desactivado";
            }

            JsonNode equipo = leerEquipo();
            if (equipo == null) {
                return "tablero: no se publico — no encuentro equipo.json en la raiz del repositorio";
            }
            String nombre = texto(equipo, "equipo");
            JsonNode integrantes = equipo.get("integrantes");
            if (esMarcador(nombre) || integrantes == null || !integrantes.isArray() || integrantes.size() != 3) {
                return "tablero: no se publico — completa equipo.json con tu equipo y tus 3 integrantes";
            }

            ObjectNode cuerpo = Api.json();
            cuerpo.put("team", nombre.trim());
            ArrayNode miembros = cuerpo.putArray("members");
            for (JsonNode persona : integrantes) {
                String quien = texto(persona, "nombre");
                String codigo = texto(persona, "codigo");
                if (esMarcador(quien) || esMarcador(codigo)) {
                    return "tablero: no se publico — hay integrantes sin completar en equipo.json";
                }
                miembros.add(quien.trim() + " (" + codigo.trim() + ")");
            }
            cuerpo.put("runId", Estado.RUN);
            cuerpo.put("stars", estrellas);
            cuerpo.put("signature", firmar(nombre.trim() + "|" + Estado.RUN + "|" + estrellas));

            ArrayNode resultados = cuerpo.putArray("results");
            for (int i = 1; i <= 5; i++) {
                ObjectNode r = resultados.addObject();
                r.put("star", i);
                r.put("name", Estado.NOMBRES.getOrDefault(i, "ESTRELLA " + i));
                r.put("passed", Estado.aprobado(i));
            }

            HttpClient cliente = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(quitarBarra(destino) + "/api/report"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() / 100 == 2) {
                return "tablero: publicado como \"" + nombre.trim() + "\"";
            }
            return "tablero: no acepto el reporte (HTTP " + respuesta.statusCode() + ") — tus estrellas no cambian";
        } catch (Exception e) {
            return "tablero: no alcanzable — tus estrellas no cambian";
        }
    }

    /** equipo.json vive en la raiz del repositorio, un nivel por encima de autotests/. */
    private static JsonNode leerEquipo() {
        String base = System.getProperty("basedir", System.getProperty("user.dir"));
        for (File candidato : new File[]{new File(base, "../equipo.json"), new File(base, "equipo.json")}) {
            if (candidato.isFile()) {
                try {
                    return Api.MAPPER.readTree(candidato);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String firmar(String contenido) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo == null ? null : nodo.get(campo);
        return valor == null || valor.isNull() ? "" : valor.asText();
    }

    private static boolean esMarcador(String valor) {
        return valor == null || MARCADORES.contains(valor.trim().toLowerCase());
    }

    private static String quitarBarra(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private Tablero() {
    }
}
