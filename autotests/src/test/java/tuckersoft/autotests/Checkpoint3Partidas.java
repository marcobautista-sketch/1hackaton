package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * Generado por tuckersoft-qa-gen 0.9 a partir del contrato v1.2 del enunciado.
 * Pendiente de regenerar: aun no refleja la fe de erratas v1.3 de README.md
 * (nodo lleno -> 409, el supervisor puede decidir sobre partidas ajenas, listado
 * de partidas filtrado por usuario para todos los roles). Ante diferencias con
 * README.md, el contrato vigente es el README.
 */
@Checkpoint(value = 3, nombre = "PARTIDAS")
@Order(3)
@ExtendWith(Checkpoints.class)
class Checkpoint3Partidas {

    static ObjectNode partida(String etiqueta, String nodoInicial) {
        ObjectNode b = Api.json();
        b.put("playerTag", etiqueta);
        b.put("startNodeCode", nodoInicial);
        return b;
    }

    @Test
    @Order(1)
    void abrir_una_partida_usa_los_valores_iniciales_y_el_dueno_del_token() {
        String etiqueta = Estado.codigo("STEFAN-01");
        Api.Res res = Api.post("/api/v1/playthroughs",
                partida(etiqueta, Estado.nodoCereal), Estado.tokenUno);

        Verificar.estado(201, res, "POST /api/v1/playthroughs responde 201");
        Verificar.igual(100, res.numero("lucidity"), "lucidity inicia en 100");
        Verificar.igual(0, res.numero("controlLevel"), "controlLevel inicia en 0");
        Verificar.igual("ACTIVA", res.texto("status"), "status inicia en ACTIVA");
        Verificar.que(res.esNulo("endingCode"), "endingCode inicia nulo",
                "null", res.texto("endingCode"), null);
        Verificar.igual(Estado.nodoCereal, res.texto("startNodeCode"), "startNodeCode es el nodo de arranque");
        Verificar.igual(Estado.nodoCereal, res.texto("currentNodeCode"),
                "currentNodeCode arranca igual que startNodeCode");
        Verificar.igual(Estado.emailUno, res.texto("ownerEmail"),
                "El dueno de la partida sale del token",
                "No aceptes un userId por el cuerpo: toma el usuario del SecurityContextHolder.");

        Estado.partidaDeUno = res.numero("id");
    }

    @Test
    @Order(2)
    void abrir_una_partida_ocupa_una_rama_del_nodo() {
        Api.Res res = Api.get("/api/v1/nodes/" + Estado.idNodoCereal, Estado.tokenUno);
        Verificar.igual(1, res.numero("currentBranches"),
                "Abrir una partida sube currentBranches del nodo a 1",
                "Incrementa y guarda el nodo dentro del mismo service que crea la partida.");
    }

    @Test
    @Order(3)
    void un_nodo_lleno_rechaza_la_partida() {
        Api.Res res = Api.post("/api/v1/playthroughs",
                partida(Estado.codigo("STEFAN-02"), Estado.nodoCereal), Estado.tokenUno);
        Verificar.estado(400, res, "Un nodo lleno (currentBranches >= branchCapacity) responde 400",
                "Ese nodo se creo con branchCapacity = 1 y ya tiene una partida.");
        Verificar.formatoError(res, "El 400 de nodo lleno");
    }

    @Test
    @Order(4)
    void etiqueta_repetida_y_nodo_inexistente() {
        Api.Res repetida = Api.post("/api/v1/playthroughs",
                partida(Estado.codigo("STEFAN-01"), Estado.nodoBucle), Estado.tokenUno);
        Verificar.estado(409, repetida, "Un playerTag repetido responde 409");

        Api.Res sinNodo = Api.post("/api/v1/playthroughs",
                partida(Estado.codigo("STEFAN-03"), Estado.codigo("NO-EXISTE")), Estado.tokenUno);
        Verificar.estado(404, sinNodo, "Un startNodeCode inexistente responde 404");

        Api.Res sinToken = Api.post("/api/v1/playthroughs",
                partida(Estado.codigo("STEFAN-04"), Estado.nodoBucle), null);
        Verificar.estado(401, sinToken, "Abrir una partida sin token responde 401");
    }

    @Test
    @Order(5)
    void cada_usuario_solo_ve_sus_partidas() {
        Api.Res deDos = Api.post("/api/v1/playthroughs",
                partida(Estado.codigo("STEFAN-DOS"), Estado.nodoBucle), Estado.tokenDos);
        Verificar.estado(201, deDos, "El segundo usuario abre su propia partida");
        Estado.partidaDeDos = deDos.numero("id");

        Api.Res listaUno = Api.get("/api/v1/playthroughs", Estado.tokenUno);
        Verificar.estado(200, listaUno, "GET /api/v1/playthroughs responde 200");
        Verificar.que(listaUno.body() != null && listaUno.body().isArray(),
                "GET /api/v1/playthroughs devuelve un array",
                "[...]", listaUno.resumen(),
                "Es una lista simple, no una estructura paginada.");

        boolean vePropia = false;
        boolean veAjena = false;
        for (JsonNode p : listaUno.body()) {
            int id = p.path("id").asInt(-1);
            if (id == Estado.partidaDeUno) vePropia = true;
            if (id == Estado.partidaDeDos) veAjena = true;
        }
        Verificar.que(vePropia, "El usuario ve su propia partida en el listado",
                "la partida #" + Estado.partidaDeUno, listaUno.resumen(), null);
        Verificar.que(!veAjena, "El usuario NO ve las partidas de otro usuario",
                "el listado sin la partida #" + Estado.partidaDeDos, listaUno.resumen(),
                "Filtra por el usuario del token en el repositorio, no en memoria.");
    }

    @Test
    @Order(6)
    void una_partida_ajena_responde_403_a_un_usuario_normal() {
        Api.Res detalle = Api.get("/api/v1/playthroughs/" + Estado.partidaDeUno, Estado.tokenDos);
        Verificar.estado(403, detalle, "Ver el detalle de una partida ajena responde 403",
                "Compara el dueno de la partida con el usuario del token antes de devolver nada.");
        Verificar.formatoError(detalle, "El 403 de partida ajena");

        Api.Res recorrido = Api.get("/api/v1/playthroughs/" + Estado.partidaDeUno + "/path", Estado.tokenDos);
        Verificar.estado(403, recorrido, "Ver el recorrido de una partida ajena responde 403");
    }

    @Test
    @Order(7)
    void el_administrador_supervisa_pero_no_juega() {
        Api.Res detalle = Api.get("/api/v1/playthroughs/" + Estado.partidaDeUno, Estado.tokenAdmin);
        Verificar.estado(200, detalle, "El administrador SI puede ver una partida ajena",
                "El administrador supervisa: para LEER no se le aplica la regla de propiedad.");
        Verificar.igual(Estado.emailUno, detalle.texto("ownerEmail"),
                "La partida que ve el administrador sigue siendo del usuario que la creo");

        Api.Res recorrido = Api.get("/api/v1/playthroughs/" + Estado.partidaDeUno + "/path", Estado.tokenAdmin);
        Verificar.estado(200, recorrido, "El administrador SI puede ver el recorrido de una partida ajena");

        Api.Res listaAdmin = Api.get("/api/v1/playthroughs", Estado.tokenAdmin);
        Verificar.estado(200, listaAdmin, "GET /api/v1/playthroughs con el administrador responde 200");
        boolean veLaDeUno = false;
        boolean veLaDeDos = false;
        for (JsonNode p : listaAdmin.body()) {
            int id = p.path("id").asInt(-1);
            if (id == Estado.partidaDeUno) veLaDeUno = true;
            if (id == Estado.partidaDeDos) veLaDeDos = true;
        }
        Verificar.que(veLaDeUno && veLaDeDos,
                "El listado del administrador incluye las partidas de TODOS los usuarios",
                "las partidas #" + Estado.partidaDeUno + " y #" + Estado.partidaDeDos,
                listaAdmin.resumen(),
                "Para ROLE_ADMIN el listado no se filtra por dueno.");

        ObjectNode decision = Api.json();
        decision.put("playthroughId", Estado.partidaDeUno);
        decision.put("rawInput", "El administrador intenta decidir sobre una partida que no es suya.");
        decision.put("impactLevel", "LEVE");
        Verificar.estado(403, Api.post("/api/v1/decisions", decision, Estado.tokenAdmin),
                "El administrador NO puede decidir sobre una partida ajena",
                "Supervisar no es jugar: la regla de propiedad para ESCRIBIR no tiene excepcion, "
                        + "ni siquiera para el administrador. Si aqui te sale 201, pusiste el "
                        + "atajo de admin en el sitio equivocado.");
    }

    @Test
    @Order(8)
    void el_recorrido_de_una_partida_nueva_esta_vacio() {
        Api.Res res = Api.get("/api/v1/playthroughs/" + Estado.partidaDeUno + "/path", Estado.tokenUno);
        Verificar.estado(200, res, "GET /api/v1/playthroughs/{id}/path responde 200");
        Verificar.igual(Estado.partidaDeUno, res.numero("playthroughId"), "El recorrido identifica su partida");
        Verificar.igual(Estado.nodoCereal, res.texto("startNodeCode"), "El recorrido expone el nodo de arranque");
        Verificar.que(res.campo("steps") != null && res.campo("steps").isArray(),
                "El recorrido expone 'steps' como array", "[]", res.resumen(), null);
        Verificar.igual(0, res.campo("steps").size(),
                "Una partida sin decisiones tiene el recorrido vacio");
    }

    @Test
    @Order(9)
    void una_partida_inexistente_responde_404() {
        Api.Res res = Api.get("/api/v1/playthroughs/999999999", Estado.tokenUno);
        Verificar.estado(404, res, "GET /api/v1/playthroughs/{id} inexistente responde 404");
    }
}
