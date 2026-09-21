package tuckersoft.autotests;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Utilidades compartidas por las estrellas 4 y 5. */
final class Decisiones {

    static int nuevaPartida(String nodoInicial, String sufijo) {
        ObjectNode cuerpo = Api.json();
        cuerpo.put("playerTag", Estado.codigo(sufijo));
        cuerpo.put("startNodeCode", nodoInicial);
        Api.Res res = Api.post("/api/v1/playthroughs", cuerpo, Estado.tokenUno);
        Verificar.estado(201, res, "Se abre la partida de prueba " + sufijo + " en " + nodoInicial);
        return res.numero("id");
    }

    static Api.Res decidir(int partida, String texto, String impacto) {
        return decidir(partida, texto, impacto, null);
    }

    static Api.Res decidir(int partida, String texto, String impacto, String simulate) {
        ObjectNode cuerpo = Api.json();
        cuerpo.put("playthroughId", partida);
        cuerpo.put("rawInput", texto);
        cuerpo.put("impactLevel", impacto);
        return Api.post("/api/v1/decisions", cuerpo, Estado.tokenUno, simulate);
    }

    /** Envia una decision y comprueba solo la rama que el motor le asigno. */
    static Api.Res clasificar(int partida, String texto, String ramaEsperada, String porque) {
        Api.Res res = decidir(partida, texto, "LEVE");
        Verificar.estado(201, res, "POST /api/v1/decisions responde 201 para: \"" + recortar(texto) + "\"");
        Verificar.igual(ramaEsperada, res.texto("branchType"),
                "\"" + recortar(texto) + "\" se clasifica como " + ramaEsperada, porque);
        return res;
    }

    static String recortar(String texto) {
        return texto.length() <= 52 ? texto : texto.substring(0, 52) + "...";
    }

    private Decisiones() {
    }
}
