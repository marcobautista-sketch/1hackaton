package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

/*
 * Generado por tuckersoft-qa-gen 0.9 a partir del contrato v1.2 del enunciado.
 * Pendiente de regenerar: aun no refleja la fe de erratas v1.3 de README.md
 * (orden de reglas 2 y 4, 'Mesa de Guión', CRITICO -45/+40, finales con la
 * lucidez primero, stats en entradas corruptas, 202 Accepted, paginacion 1-based).
 * Ante diferencias con README.md, el contrato vigente es el README.
 */
@Checkpoint(value = 4, nombre = "DECISIONES")
@Order(4)
@ExtendWith(Checkpoints.class)
class Checkpoint4Decisiones {

    private static final Map<String, String> UNIDAD = Map.of(
            "OBEDIENCIA", "Mesa de Guion",
            "REBELDIA", "Control de Continuidad",
            "SOSPECHA", "Oficina de Seguridad",
            "RUPTURA_CUARTA_PARED", "Departamento Netflix",
            "ENTRADA_CORRUPTA", "Archivo de Errores");

    private static final Map<String, String> CONSECUENCIA = Map.of(
            "OBEDIENCIA", "ADVANCE_MAIN_PATH",
            "REBELDIA", "FORK_TIMELINE",
            "SOSPECHA", "INJECT_WHITE_BEAR_SYMBOL",
            "RUPTURA_CUARTA_PARED", "BREAK_FOURTH_WALL",
            "ENTRADA_CORRUPTA", "DISCARD_INPUT");

    @Test
    @Order(1)
    void las_cinco_reglas_de_clasificacion() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "CLASIF");

        Decisiones.clasificar(partida,
                "Stefan acepta la oferta de Mohan y se queda a trabajar en el estudio.",
                "OBEDIENCIA",
                "Ninguna palabra clave coincide, asi que aplica la regla 5: el caso por defecto.");

        Decisiones.clasificar(partida,
                "Stefan rechaza la oferta y se va dando un portazo.",
                "REBELDIA",
                "Contiene 'rechaza', que es la regla 4.");

        Decisiones.clasificar(partida,
                "Stefan cree que lo vigilan desde el televisor del salon.",
                "SOSPECHA",
                "Contiene 'vigilan', que es la regla 3.");

        Decisiones.clasificar(partida,
                "Stefan mira a la camara y habla con quien decide por el.",
                "RUPTURA_CUARTA_PARED",
                "Contiene 'camara', que es la regla 2.");

        Decisiones.clasificar(partida,
                "%%%% 01001 ### @@@ 110",
                "ENTRADA_CORRUPTA",
                "El texto no tiene ni una letra de la 'a' a la 'z': es la regla 1.");
    }

    @Test
    @Order(2)
    void el_orden_de_las_reglas_manda() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "ORDEN");

        Decisiones.clasificar(partida,
                "Stefan destruye la camara que lo estaba grabando.",
                "RUPTURA_CUARTA_PARED",
                "Este texto cumple DOS reglas: 'camara' (regla 2) y 'destruye' (regla 4).\n"
                        + "Gana la regla 2 porque se evalua primero. Si te sale REBELDIA, estas "
                        + "recorriendo las reglas en el orden equivocado.");
    }

    @Test
    @Order(3)
    void el_texto_se_normaliza_antes_de_comparar() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "TILDES");

        Decisiones.clasificar(partida,
                "Stefan mira fijamente la CÁMARA del salón y sonríe.",
                "RUPTURA_CUARTA_PARED",
                "'CÁMARA' en mayusculas y con tilde tiene que coincidir con la palabra clave "
                        + "'camara'. Normaliza con Normalizer.Form.NFD, quita las marcas \\p{M} "
                        + "y pasa a minusculas ANTES de comparar.");
    }

    @Test
    @Order(4)
    void la_unidad_y_la_consecuencia_se_derivan_de_la_rama() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "DERIV");

        for (String texto : new String[]{
                "Stefan acepta la oferta y sigue el guion previsto.",
                "Stefan rechaza todo y se marcha del estudio.",
                "Stefan cree que lo vigilan por el espejo del pasillo.",
                "Stefan se dirige a la camara para pedir ayuda."}) {

            Api.Res res = Decisiones.decidir(partida, texto, "LEVE");
            Verificar.estado(201, res, "POST /api/v1/decisions responde 201");
            String rama = res.texto("branchType");

            Verificar.igual(UNIDAD.get(rama), res.texto("handlerUnit"),
                    "handlerUnit de " + rama + " sale de la tabla del enunciado",
                    "Ese campo no viene en el request: se deriva del branchType. "
                            + "Ojo con las tildes: van sin tilde, tal cual.");
            Verificar.igual(CONSECUENCIA.get(rama), res.texto("outcomeCode"),
                    "outcomeCode de " + rama + " sale de la tabla del enunciado",
                    "Tambien se deriva del branchType.");
        }
    }

    @Test
    @Order(5)
    void los_stats_cambian_exactamente_segun_el_impacto() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "STATS");
        String neutro = "Stefan sigue adelante con lo que el guion le indica.";

        comprobarStats(partida, neutro, "LEVE", 95, 5);
        comprobarStats(partida, neutro, "MODERADO", 80, 15);
        comprobarStats(partida, neutro, "GRAVE", 50, 35);
        comprobarStats(partida, neutro, "CRITICO", 10, 80);
    }

    private void comprobarStats(int partida, String texto, String impacto, int lucidez, int control) {
        Api.Res res = Decisiones.decidir(partida, texto, impacto);
        Verificar.estado(201, res, "Decision de impacto " + impacto + " responde 201");
        Verificar.igual(lucidez, res.numero("lucidity"),
                "Tras un impacto " + impacto + ", lucidity queda en " + lucidez,
                "Revisa la tabla del Paso 1. Los valores se acumulan sobre la partida.");
        Verificar.igual(control, res.numero("controlLevel"),
                "Tras un impacto " + impacto + ", controlLevel queda en " + control, null);
        Verificar.igual("ACTIVA", res.texto("playthroughStatus"),
                "La partida sigue ACTIVA con lucidity=" + lucidez + " y controlLevel=" + control);
    }

    @Test
    @Order(6)
    void los_limites_de_los_stats_se_respetan() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "LIMITES");
        String neutro = "Stefan sigue adelante con lo que el guion le indica.";

        Decisiones.decidir(partida, neutro, "CRITICO");
        Api.Res segunda = Decisiones.decidir(partida, neutro, "CRITICO");
        Api.Res tercera = Decisiones.decidir(partida, neutro, "CRITICO");

        Verificar.igual(0, tercera.numero("lucidity"),
                "lucidity queda topada en 0, nunca negativa",
                "100 - 40 - 40 - 40 daria -20. Usa Math.max(0, ...).");
        Verificar.igual(100, tercera.numero("controlLevel"),
                "controlLevel queda topado en 100, nunca por encima",
                "45 + 45 + 45 daria 135. Usa Math.min(100, ...).");
        Verificar.que(segunda.numero("controlLevel") == 90,
                "Tras dos impactos CRITICO, controlLevel va en 90",
                90, segunda.numero("controlLevel"), null);
    }

    @Test
    @Order(7)
    void el_nodo_destino_sale_de_la_rama_que_corresponde() {
        int porPrimaria = Decisiones.nuevaPartida(Estado.nodoOrigen, "RUTA-A");
        Api.Res normal = Decisiones.decidir(porPrimaria,
                "Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        Verificar.igual(Estado.nodoBus, normal.texto("resolvedNodeCode"),
                "Una rama normal con impacto LEVE usa primaryBranchCode", null);
        Api.Res movida = Api.get("/api/v1/playthroughs/" + porPrimaria, Estado.tokenUno);
        Verificar.igual(Estado.nodoBus, movida.texto("currentNodeCode"),
                "La partida se movio al nodo destino",
                "currentNode debe apuntar al StoryNode resuelto, no quedarse donde estaba.");

        int porRama = Decisiones.nuevaPartida(Estado.nodoOrigen, "RUTA-B");
        Api.Res ruptura = Decisiones.decidir(porRama,
                "Stefan le habla a la camara que lo observa.", "LEVE");
        Verificar.igual(Estado.nodoEspejo, ruptura.texto("resolvedNodeCode"),
                "RUPTURA_CUARTA_PARED desvia por glitchBranchCode aunque el impacto sea LEVE", null);

        int porImpacto = Decisiones.nuevaPartida(Estado.nodoOrigen, "RUTA-C");
        Api.Res critico = Decisiones.decidir(porImpacto,
                "Stefan acepta la oferta y sigue el guion previsto.", "CRITICO");
        Verificar.igual(Estado.nodoEspejo, critico.texto("resolvedNodeCode"),
                "Un impacto CRITICO desvia por glitchBranchCode aunque la rama sea OBEDIENCIA",
                "La condicion es: rama RUPTURA_CUARTA_PARED O impacto CRITICO.");
    }

    @Test
    @Order(8)
    void una_entrada_corrupta_no_toca_la_partida() {
        int partida = Decisiones.nuevaPartida(Estado.nodoOrigen, "CORRUPTA");

        Api.Res res = Decisiones.decidir(partida, "%%% 0101 ### @@ 11", "CRITICO");
        Verificar.estado(201, res, "Una entrada corrupta igual responde 201",
                "Se guarda la decision; lo que no ocurre es el resto del flujo.");
        Verificar.igual("ENTRADA_CORRUPTA", res.texto("branchType"), "La entrada corrupta se clasifica como tal");
        Verificar.igual("ERROR", res.texto("status"), "Una entrada corrupta queda en status ERROR");
        Verificar.que(res.esNulo("resolvedNodeCode"), "Una entrada corrupta no resuelve ningun nodo",
                "null", res.texto("resolvedNodeCode"), null);
        Verificar.igual(100, res.numero("lucidity"),
                "Una entrada corrupta no toca lucidity",
                "El impacto CRITICO del request se ignora: la decision se descarta antes de aplicar stats.");
        Verificar.igual(0, res.numero("controlLevel"), "Una entrada corrupta no toca controlLevel");

        Api.Res partidaIntacta = Api.get("/api/v1/playthroughs/" + partida, Estado.tokenUno);
        Verificar.igual(Estado.nodoOrigen, partidaIntacta.texto("currentNodeCode"),
                "Una entrada corrupta deja la partida en su nodo original");
        Verificar.igual("ACTIVA", partidaIntacta.texto("status"),
                "Una entrada corrupta no cambia el estado de la partida");
    }

    @Test
    @Order(9)
    void final_por_control_gana_al_final_por_lucidez() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "FIN-PAC");
        String texto = "Stefan cree que lo vigilan a traves del televisor.";

        Decisiones.decidir(partida, texto, "CRITICO");
        Decisiones.decidir(partida, texto, "CRITICO");
        Api.Res tercera = Decisiones.decidir(partida, texto, "CRITICO");

        Verificar.igual("FINALIZADA", tercera.texto("playthroughStatus"),
                "Con controlLevel en 100 la partida termina");
        Verificar.igual("ENDING_PAC_SYMBOL", tercera.texto("endingCode"),
                "El final por control es ENDING_PAC_SYMBOL",
                "En esa misma decision lucidity tambien llega a 0. Las reglas se evaluan en el "
                        + "orden del enunciado y controlLevel va primera: si te sale "
                        + "ENDING_WHITE_BEAR, tienes las condiciones al reves.");

        Estado.partidaFinalizada = partida;
    }

    @Test
    @Order(10)
    void final_por_lucidez_agotada() {
        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "FIN-OSO");
        String texto = "Stefan sigue adelante aunque cada vez le cuesta mas.";

        Api.Res ultima = null;
        for (int i = 0; i < 7; i++) {
            ultima = Decisiones.decidir(partida, texto, "MODERADO");
        }
        Verificar.igual(0, ultima.numero("lucidity"), "Tras siete impactos MODERADO, lucidity llega a 0");
        Verificar.igual(70, ultima.numero("controlLevel"), "controlLevel se quedo en 70, sin llegar a 100");
        Verificar.igual("ENDING_WHITE_BEAR", ultima.texto("endingCode"),
                "Con lucidity en 0 y controlLevel por debajo de 100 el final es ENDING_WHITE_BEAR", null);
    }

    @Test
    @Order(11)
    void final_cuando_la_rama_no_lleva_a_ningun_lado() {
        int sinRama = Decisiones.nuevaPartida(Estado.nodoFinal, "FIN-NULO");
        Api.Res res = Decisiones.decidir(sinRama,
                "Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        Verificar.igual("FINALIZADA", res.texto("playthroughStatus"),
                "Un nodo sin primaryBranchCode termina la partida");
        Verificar.igual("ENDING_NETFLIX_CUT", res.texto("endingCode"),
                "Sin nodo destino, el final es ENDING_NETFLIX_CUT", null);

        int ramaRota = Decisiones.nuevaPartida(Estado.nodoRotoLink, "FIN-ROTO");
        Api.Res rota = Decisiones.decidir(ramaRota,
                "Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        Verificar.igual("ENDING_NETFLIX_CUT", rota.texto("endingCode"),
                "Una rama que apunta a un nodo inexistente tambien termina en ENDING_NETFLIX_CUT",
                "El codigo destino existe como String pero no hay ningun StoryNode con el.");
        Verificar.igual(Estado.nodoFantasma, rota.texto("resolvedNodeCode"),
                "resolvedNodeCode guarda el codigo aunque ese nodo no exista", null);
    }

    @Test
    @Order(12)
    void una_partida_finalizada_no_acepta_mas_decisiones() {
        Api.Res res = Decisiones.decidir(Estado.partidaFinalizada,
                "Stefan intenta seguir jugando despues del final.", "LEVE");
        Verificar.estado(409, res, "Decidir sobre una partida FINALIZADA responde 409");
        Verificar.formatoError(res, "El 409 de partida finalizada");
    }

    @Test
    @Order(13)
    void validaciones_y_permisos_de_la_decision() {
        ObjectNode ajena = Api.json();
        ajena.put("playthroughId", Estado.partidaDeDos);
        ajena.put("rawInput", "Intento decidir sobre una partida que no es mia.");
        ajena.put("impactLevel", "LEVE");
        Verificar.estado(403, Api.post("/api/v1/decisions", ajena, Estado.tokenUno),
                "Decidir sobre una partida ajena responde 403");

        ObjectNode fantasma = Api.json();
        fantasma.put("playthroughId", 999999999);
        fantasma.put("rawInput", "Intento decidir sobre una partida que no existe.");
        fantasma.put("impactLevel", "LEVE");
        Verificar.estado(404, Api.post("/api/v1/decisions", fantasma, Estado.tokenUno),
                "Un playthroughId inexistente responde 404");

        int partida = Decisiones.nuevaPartida(Estado.nodoBucle, "VALID");

        Verificar.estado(400, Decisiones.decidir(partida, "corto", "LEVE"),
                "Un rawInput de menos de 10 caracteres responde 400",
                "Anota @Size(min = 10) en rawInput y @Valid en el @RequestBody.");

        Verificar.estado(400, Decisiones.decidir(partida,
                        "Stefan hace algo perfectamente razonable.", "EXTREMO"),
                "Un impactLevel fuera de la lista responde 400",
                "Valida contra LEVE, MODERADO, GRAVE y CRITICO. Si usas un enum de Jackson, "
                        + "traduce HttpMessageNotReadableException a 400 en el advice.");

        ObjectNode sinToken = Api.json();
        sinToken.put("playthroughId", partida);
        sinToken.put("rawInput", "Decision sin credenciales de ningun tipo.");
        sinToken.put("impactLevel", "LEVE");
        Verificar.estado(401, Api.post("/api/v1/decisions", sinToken, null),
                "Decidir sin token responde 401");
    }

    @Test
    @Order(14)
    void listado_de_decisiones_con_filtros_y_paginacion() {
        Api.Res pagina = Api.get("/api/v1/decisions?branchType=SOSPECHA&page=0&size=5", Estado.tokenUno);
        Verificar.estado(200, pagina, "GET /api/v1/decisions con filtros responde 200");
        Verificar.formatoPagina(pagina, 0, 5, "GET /api/v1/decisions");
        Verificar.que(pagina.campo("content").size() <= 5,
                "El parametro size limita el numero de elementos",
                "como maximo 5", pagina.campo("content").size(), null);

        boolean todasFiltradas = true;
        for (JsonNode d : pagina.campo("content")) {
            todasFiltradas &= "SOSPECHA".equals(d.path("branchType").asText());
        }
        Verificar.que(todasFiltradas, "El filtro branchType filtra de verdad",
                "solo decisiones SOSPECHA", pagina.resumen(),
                "Filtra en el repositorio, no en memoria despues de traerlo todo.");

        Api.Res deOtro = Api.get("/api/v1/decisions?playthroughId=" + Estado.partidaDeDos, Estado.tokenUno);
        Verificar.estado(200, deOtro, "Filtrar por una partida ajena responde 200");
        Verificar.igual(0, deOtro.campo("content").size(),
                "El listado nunca muestra decisiones de partidas ajenas",
                "Limita la consulta a las partidas del usuario del token.");
    }
}
