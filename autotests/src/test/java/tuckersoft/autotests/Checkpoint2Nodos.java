package tuckersoft.autotests;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * Generado por tuckersoft-qa-gen 0.9 a partir del contrato v1.2 del enunciado.
 * Pendiente de regenerar: aun no refleja la fe de erratas v1.3 de README.md
 * (branchCapacity = 0 como capacidad ilimitada, GET /api/v1/nodes paginado).
 * Ante diferencias con README.md, el contrato vigente es el README.
 */
@Checkpoint(value = 2, nombre = "NODOS")
@Order(2)
@ExtendWith(Checkpoints.class)
class Checkpoint2Nodos {

    static ObjectNode nodo(String codigo, int capacidad, String primaria, String glitch) {
        ObjectNode b = Api.json();
        b.put("nodeCode", codigo);
        b.put("title", "Escena " + codigo);
        b.put("sceneText", "Escena generada por el control de calidad de Tuckersoft para " + codigo + ".");
        b.put("branchCapacity", capacidad);
        b.put("primaryBranchCode", primaria);
        b.put("glitchBranchCode", glitch);
        return b;
    }

    @Test
    @Order(1)
    void crear_nodos_exige_ser_admin() {
        ObjectNode cuerpo = nodo(Estado.codigo("INTENTO"), 5, null, null);

        Api.Res sinToken = Api.post("/api/v1/nodes", cuerpo, null);
        Verificar.estado(401, sinToken, "POST /api/v1/nodes sin token responde 401");

        Api.Res comoUsuario = Api.post("/api/v1/nodes", cuerpo, Estado.tokenUno);
        Verificar.estado(403, comoUsuario, "POST /api/v1/nodes con un usuario normal responde 403",
                "Restringe POST /api/v1/nodes a hasRole(\"ADMIN\"). Ojo: hasRole(\"ADMIN\") espera "
                        + "que la autoridad se llame ROLE_ADMIN.");
        Verificar.formatoError(comoUsuario, "El 403 de crear nodo");
    }

    @Test
    @Order(2)
    void el_admin_crea_un_nodo_con_los_valores_iniciales() {
        Estado.nodoCereal = Estado.codigo("CEREAL");
        Estado.nodoBus = Estado.codigo("BUS");
        Estado.nodoEspejo = Estado.codigo("ESPEJO");

        Api.Res res = Api.post("/api/v1/nodes",
                nodo(Estado.nodoCereal, 1, Estado.nodoBus, Estado.nodoEspejo), Estado.tokenAdmin);

        Verificar.estado(201, res, "POST /api/v1/nodes con el admin responde 201");
        Verificar.campo(res, "id", "El nodo creado");
        Verificar.igual(Estado.nodoCereal, res.texto("nodeCode"), "El nodo conserva su nodeCode");
        Verificar.igual(0, res.numero("currentBranches"), "currentBranches inicia en 0",
                "Ese campo lo fija el service, no llega en el request.");
        Verificar.igual(1, res.numero("branchCapacity"), "branchCapacity se persiste tal cual");
        Verificar.igual(Estado.nodoBus, res.texto("primaryBranchCode"), "primaryBranchCode se persiste");
        Verificar.igual(Estado.nodoEspejo, res.texto("glitchBranchCode"), "glitchBranchCode se persiste");
        Verificar.campo(res, "createdAt", "El nodo creado");

        Estado.idNodoCereal = res.numero("id");
    }

    @Test
    @Order(3)
    void el_nodo_persiste_y_se_recupera_igual() {
        Api.Res res = Api.get("/api/v1/nodes/" + Estado.idNodoCereal, Estado.tokenUno);
        Verificar.estado(200, res, "GET /api/v1/nodes/{id} responde 200");
        Verificar.igual(Estado.nodoCereal, res.texto("nodeCode"),
                "El nodo persistio en PostgreSQL y se recupera igual");
    }

    @Test
    @Order(4)
    void codigo_repetido_responde_409() {
        Api.Res res = Api.post("/api/v1/nodes",
                nodo(Estado.nodoCereal, 3, null, null), Estado.tokenAdmin);
        Verificar.estado(409, res, "Un nodeCode repetido responde 409");
        Verificar.formatoError(res, "El 409 de nodeCode repetido");
    }

    @Test
    @Order(5)
    void datos_invalidos_responden_400() {
        Api.Res capacidadCero = Api.post("/api/v1/nodes",
                nodo(Estado.codigo("CAP0"), 0, null, null), Estado.tokenAdmin);
        Verificar.estado(400, capacidadCero, "branchCapacity = 0 responde 400",
                "Anota @Min(1) en branchCapacity.");

        ObjectNode sinCodigo = nodo(Estado.codigo("SINCODIGO"), 5, null, null);
        sinCodigo.remove("nodeCode");
        Verificar.estado(400, Api.post("/api/v1/nodes", sinCodigo, Estado.tokenAdmin),
                "Un nodo sin nodeCode responde 400",
                "Anota @NotBlank en nodeCode.");

        ObjectNode escenaCorta = nodo(Estado.codigo("CORTA"), 5, null, null);
        escenaCorta.put("sceneText", "corto");
        Verificar.estado(400, Api.post("/api/v1/nodes", escenaCorta, Estado.tokenAdmin),
                "Un sceneText de menos de 10 caracteres responde 400",
                "Anota @Size(min = 10) en sceneText.");
    }

    @Test
    @Order(6)
    void un_nodo_inexistente_responde_404() {
        Api.Res res = Api.get("/api/v1/nodes/999999999", Estado.tokenUno);
        Verificar.estado(404, res, "GET /api/v1/nodes/{id} inexistente responde 404");
        Verificar.formatoError(res, "El 404 de nodo inexistente");
    }

    @Test
    @Order(7)
    void se_construye_el_grafo_de_pruebas() {
        Estado.nodoFinal = Estado.codigo("FINAL");
        Estado.nodoBucle = Estado.codigo("BUCLE");
        Estado.nodoRotoLink = Estado.codigo("ROTO");
        Estado.nodoOrigen = Estado.codigo("ORIGEN");
        Estado.nodoFantasma = Estado.codigo("FANTASMA");

        crear(nodo(Estado.nodoBus, 50, Estado.nodoFinal, Estado.nodoEspejo));
        crear(nodo(Estado.nodoEspejo, 50, Estado.nodoOrigen, Estado.nodoEspejo));
        crear(nodo(Estado.nodoFinal, 50, null, Estado.nodoEspejo));
        crear(nodo(Estado.nodoBucle, 50, Estado.nodoBucle, Estado.nodoBucle));
        crear(nodo(Estado.nodoRotoLink, 50, Estado.nodoFantasma, Estado.nodoFantasma));
        crear(nodo(Estado.nodoOrigen, 50, Estado.nodoBus, Estado.nodoEspejo));

        Api.Res lista = Api.get("/api/v1/nodes", Estado.tokenUno);
        Verificar.estado(200, lista, "GET /api/v1/nodes responde 200");
        Verificar.arrayNoVacio(lista, "GET /api/v1/nodes devuelve un array de nodos",
                "Devuelve una lista de DTOs, no un objeto paginado.");
    }

    private void crear(ObjectNode cuerpo) {
        Api.Res res = Api.post("/api/v1/nodes", cuerpo, Estado.tokenAdmin);
        Verificar.estado(201, res, "Se crea el nodo de pruebas " + cuerpo.get("nodeCode").asText());
    }
}
