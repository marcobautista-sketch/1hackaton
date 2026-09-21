package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * Generado por tuckersoft-qa-gen 0.9 a partir del contrato v1.2 del enunciado.
 * Pendiente de regenerar: aun no refleja la fe de erratas v1.3 de README.md
 * (subject "[TUCKERSOFT] <playerTag> - <branchType> (Impacto <impactLevel>)" y
 * envio a ADMIN_EMAIL). Ante diferencias con README.md, el contrato vigente es el README.
 */
@Checkpoint(value = 5, nombre = "ASINCRONIA")
@Order(5)
@ExtendWith(Checkpoints.class)
class Checkpoint5Asincronia {

    private static int partida;
    private static int decisionNormal;
    private static String ramaNormal;
    private static String etiquetaJugador;
    private static String asuntoEsperado;
    private static String textoDecision;

    private static final String PISTA_SMTP = """
                Los autotests levantan un servidor SMTP en localhost:2525 y esperan el
                correo ahi. Comprueba que tu .env tiene MAIL_HOST=localhost y
                MAIL_PORT=2525, y que de verdad usas JavaMailSender: guardar la fila
                en RealityLog sin enviar nada no cuenta.""";

    /** Espera a que la decision llegue al estado pedido. Devuelve la ultima respuesta vista. */
    private static Api.Res esperarEstado(int decision, String objetivo, int segundos) {
        long limite = System.currentTimeMillis() + segundos * 1000L;
        Api.Res ultima = null;
        while (System.currentTimeMillis() < limite) {
            ultima = Api.get("/api/v1/decisions/" + decision, Estado.tokenUno);
            if (objetivo.equals(ultima.texto("status"))) return ultima;
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return ultima;
    }

    @Test
    @Order(1)
    void el_201_llega_sin_esperar_al_correo() {
        partida = Decisiones.nuevaPartida(Estado.nodoBucle, "ASYNC");
        Api.Res detalle = Api.get("/api/v1/playthroughs/" + partida, Estado.tokenUno);
        etiquetaJugador = detalle.texto("playerTag");

        textoDecision = "Stefan acepta la oferta y se pone a programar hasta el amanecer.";
        Api.Res res = Decisiones.decidir(partida, textoDecision, "LEVE");

        Verificar.estado(201, res, "POST /api/v1/decisions responde 201");
        Verificar.que(res.millis() < 1500,
                "El 201 llega sin esperar a que salga el correo",
                "menos de 1500 ms", res.millis() + " ms",
                "Si tarda segundos, el envio esta ocurriendo dentro de la peticion. "
                        + "Te falta @EnableAsync, o @Async en el listener, o el listener no es "
                        + "un bean aparte (Spring no aplica @Async a llamadas internas).");
        Verificar.igual("REGISTRADA", res.texto("status"),
                "La decision nace REGISTRADA, antes de que el listener la toque",
                "El service la guarda como REGISTRADA; es el listener quien la mueve despues.");

        decisionNormal = res.numero("id");
        ramaNormal = res.texto("branchType");
    }

    @Test
    @Order(2)
    void el_listener_asincrono_estabiliza_la_decision() {
        Api.Res res = esperarEstado(decisionNormal, "ESTABILIZADA", 25);
        Verificar.igual("ESTABILIZADA", res.texto("status"),
                "En menos de 25 s la decision pasa a ESTABILIZADA",
                """
                Si se quedo en REGISTRADA, el listener no llego a ejecutarse:
                  · falta publicar DecisionCommittedEvent con ApplicationEventPublisher
                  · o falta @TransactionalEventListener(phase = AFTER_COMMIT)
                  · o falta @EnableAsync en alguna @Configuration
                Si llego a ERROR, el listener SI corrio pero el correo fallo de verdad:
                  · revisa el log.error del listener en la consola de tu app
                  · revisa MAIL_HOST=localhost y MAIL_PORT=2525 en tu .env: ahi escucha
                    el servidor SMTP de los autotests""");
    }

    @Test
    @Order(3)
    void el_envio_queda_auditado_en_reality_log() {
        Api.Res logs = Api.get("/api/v1/decisions/" + decisionNormal + "/reality-logs", Estado.tokenUno);
        Verificar.estado(200, logs, "GET /api/v1/decisions/{id}/reality-logs responde 200");
        Verificar.arrayNoVacio(logs, "El envio quedo auditado en RealityLog",
                "El listener necesita su propio @Transactional para que sus saves hagan commit. "
                        + "Sin el, el cambio de status se ve pero el log no se guarda.");

        JsonNode enviado = Verificar.buscar(logs, "logStatus", "SENT");
        Verificar.que(enviado != null, "Existe un RealityLog con logStatus SENT",
                "un log con logStatus = SENT", logs.resumen(), null);
        Verificar.que(enviado.hasNonNull("sentAt"), "El log exitoso guarda sentAt",
                "un instante", "null", "sentAt solo se rellena cuando el envio sale bien.");
        Verificar.igual(Estado.emailUno, enviado.path("recipientEmail").asText(null),
                "El informe va al email del dueno de la partida",
                "El destinatario sale del usuario dueno de la partida, no del admin.");

        asuntoEsperado = "[TUCKERSOFT] " + ramaNormal + " en " + etiquetaJugador + " | Impacto LEVE";
        Verificar.igual(asuntoEsperado, enviado.path("subject").asText(null),
                "El subject respeta el formato exacto del enunciado",
                "Formato: [TUCKERSOFT] <branchType> en <playerTag> | Impacto <impactLevel>");
    }

    @Test
    @Order(4)
    void el_correo_llega_de_verdad_al_servidor_smtp() {
        MimeMessage mensaje = Buzon.esperarPorAsunto(asuntoEsperado, 20);
        Verificar.que(mensaje != null,
                "El Informe de Realidad llega al servidor SMTP",
                "un correo con asunto: " + asuntoEsperado,
                "correos recibidos: " + Buzon.asuntosRecibidos(),
                PISTA_SMTP);

        String destino = Buzon.destinatario(mensaje);
        Verificar.que(destino != null && destino.contains(Estado.emailUno),
                "El correo va dirigido al dueno de la partida",
                Estado.emailUno, destino,
                "El destinatario sale del usuario dueno de la partida, no del admin.");
    }

    @Test
    @Order(5)
    void el_cuerpo_del_correo_trae_los_datos_del_enunciado() {
        MimeMessage mensaje = Buzon.esperarPorAsunto(asuntoEsperado, 10);
        Verificar.que(mensaje != null, "Hay un correo que revisar",
                "el correo de la decision", "ninguno", null);

        String cuerpo = Buzon.cuerpo(mensaje);
        String muestra = cuerpo.length() > 400 ? cuerpo.substring(0, 400) + "..." : cuerpo;

        String[][] obligatorios = {
                {"el id de la decision", "#" + decisionNormal},
                {"la etiqueta del jugador", etiquetaJugador},
                {"la rama", ramaNormal},
                {"la decision original del jugador", textoDecision},
        };
        for (String[] dato : obligatorios) {
            Verificar.que(cuerpo.contains(dato[1]),
                    "El cuerpo del correo incluye " + dato[0],
                    dato[1], muestra,
                    "El cuerpo tiene el formato que fija el enunciado, con todos sus campos.");
        }

        for (String etiqueta : new String[]{"Decision ID", "Jugador", "Rama", "Impacto",
                "Departamento", "Lucidez", "Nivel de control"}) {
            Verificar.que(cuerpo.contains(etiqueta),
                    "El cuerpo del correo trae la etiqueta '" + etiqueta + "'",
                    etiqueta + " presente", muestra,
                    "Copia el bloque del Informe de Realidad tal como esta en el enunciado.");
        }
    }

    @Test
    @Order(6)
    void un_fallo_de_correo_no_tumba_el_sistema() {
        Api.Res res = Decisiones.decidir(partida,
                "Stefan desconecta el telefono para que nadie lo interrumpa.", "LEVE", "MAIL_FAILURE");
        Verificar.estado(201, res, "Con el correo caido el cliente igual recibe 201",
                "El fallo de SMTP ocurre en otro hilo, despues de responder: nunca llega al cliente.");

        int decision = res.numero("id");
        Api.Res fallada = esperarEstado(decision, "ERROR", 25);
        Verificar.igual("ERROR", fallada.texto("status"),
                "Un fallo de correo deja la decision en ERROR",
                "El catch del listener tiene que actualizar el status y crear el log, "
                        + "no solo escribir en la consola.");

        Api.Res logs = Api.get("/api/v1/decisions/" + decision + "/reality-logs", Estado.tokenUno);
        JsonNode fallo = Verificar.buscar(logs, "logStatus", "FAILED");
        Verificar.que(fallo != null, "Existe un RealityLog con logStatus FAILED",
                "un log con logStatus = FAILED", logs.resumen(),
                "La cabecera X-Bandersnatch-Simulate: MAIL_FAILURE tiene que llegar hasta el "
                        + "listener, normalmente como un campo del evento.");
        Verificar.que(fallo.hasNonNull("errorMessage"), "El log fallido guarda el mensaje del error",
                "el texto de la excepcion", "null", null);
        Verificar.que(!fallo.hasNonNull("sentAt"), "El log fallido no tiene sentAt",
                "null", fallo.path("sentAt").asText(null), null);

        int antes = Buzon.recibidos();
        Verificar.que(antes > 0, "El servidor SMTP recibio los correos de las decisiones correctas",
                "al menos uno", antes, null);
    }

    @Test
    @Order(7)
    void el_sistema_sigue_aceptando_decisiones_despues_del_fallo() {
        Api.Res res = Decisiones.decidir(partida,
                "Stefan vuelve a la maquina y sigue programando pese a todo.", "LEVE");
        Verificar.estado(201, res, "Tras el fallo de correo el sistema sigue vivo");
    }

    @Test
    @Order(8)
    void el_recorrido_refleja_las_decisiones_que_movieron_la_historia() {
        Api.Res res = Api.get("/api/v1/playthroughs/" + partida + "/path", Estado.tokenUno);
        Verificar.estado(200, res, "GET /api/v1/playthroughs/{id}/path responde 200");

        JsonNode pasos = res.campo("steps");
        Verificar.que(pasos != null && pasos.isArray() && pasos.size() == 3,
                "El recorrido lista las 3 decisiones de esta partida",
                "3 pasos", pasos == null ? "null" : pasos.size(),
                "Entran solo las decisiones con resolvedNodeCode no nulo.");

        Verificar.igual(1, pasos.get(0).path("order").asInt(-1), "El primer paso tiene order 1");
        Verificar.igual(2, pasos.get(1).path("order").asInt(-1), "El segundo paso tiene order 2");
        Verificar.igual(3, pasos.get(2).path("order").asInt(-1), "El tercer paso tiene order 3");
        Verificar.igual(Estado.nodoBucle, pasos.get(0).path("fromNodeCode").asText(null),
                "El primer paso sale del nodo de arranque");
        Verificar.igual(Estado.nodoBucle, pasos.get(0).path("toNodeCode").asText(null),
                "El primer paso llega al nodo que resolvio la rama");
        Verificar.campo(res, "currentNodeCode", "El recorrido");
    }

    @Test
    @Order(9)
    void los_informes_de_una_decision_ajena_no_se_ven() {
        Api.Res res = Api.get("/api/v1/decisions/" + decisionNormal + "/reality-logs", Estado.tokenDos);
        Verificar.estado(403, res,
                "Los reality-logs de una decision ajena responden 403",
                "La decision pertenece a una partida de otro usuario: el aislamiento tambien "
                        + "aplica a los recursos hijos.");
    }
}
