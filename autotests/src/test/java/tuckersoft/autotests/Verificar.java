package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/**
 * Aserciones con mensajes largos a proposito.
 *
 * El alumno no tiene al TA al lado: el mensaje de fallo es toda la ayuda que va a
 * recibir, asi que dice que se esperaba, que llego, la respuesta cruda y una pista
 * concreta de donde mirar.
 */
final class Verificar {

    static void que(boolean condicion, String queSeProbaba, Object esperado, Object recibido, String pista) {
        if (condicion) {
            Estado.sumarComprobacion();
            return;
        }
        StringBuilder sb = new StringBuilder("\n\n  ✘ ").append(queSeProbaba).append('\n');
        sb.append("      esperaba : ").append(esperado).append('\n');
        sb.append("      recibido : ").append(recibido).append('\n');
        if (pista != null && !pista.isBlank()) {
            sb.append("      pista    : ").append(pista.replace("\n", "\n                 ")).append('\n');
        }
        throw new AssertionError(sb.toString());
    }

    static void igual(Object esperado, Object recibido, String queSeProbaba, String pista) {
        que(Objects.equals(esperado, recibido), queSeProbaba, esperado, recibido, pista);
    }

    static void igual(Object esperado, Object recibido, String queSeProbaba) {
        igual(esperado, recibido, queSeProbaba, null);
    }

    static void estado(int esperado, Api.Res res, String queSeProbaba, String pista) {
        que(res.status() == esperado, queSeProbaba,
                "HTTP " + esperado,
                "HTTP " + res.status() + "  ->  " + res.resumen(),
                pista);
    }

    static void estado(int esperado, Api.Res res, String queSeProbaba) {
        estado(esperado, res, queSeProbaba, pistaPorEstado(esperado));
    }

    private static String pistaPorEstado(int esperado) {
        return switch (esperado) {
            case 400 -> "Anota @Valid en el @RequestBody y deja que un @RestControllerAdvice traduzca "
                    + "MethodArgumentNotValidException a 400.";
            case 401 -> "El 401 lo produce Spring Security. Necesitas un AuthenticationEntryPoint propio "
                    + "para que el cuerpo tenga el formato del enunciado.";
            case 403 -> "El 403 sale de una regla de autorizacion o de que el recurso no es del usuario "
                    + "del token. Necesitas un AccessDeniedHandler propio para el cuerpo.";
            case 404 -> "Lanza tu excepcion de 'no encontrado' y mapeala a 404 en el @RestControllerAdvice.";
            case 409 -> "Comprueba la unicidad ANTES de guardar y lanza una excepcion que el advice "
                    + "traduzca a 409.";
            default -> null;
        };
    }

    /** Comprueba que el cuerpo trae el campo y no es nulo. */
    static void campo(Api.Res res, String campo, String contexto) {
        que(res.body() != null && res.body().hasNonNull(campo),
                contexto + " debe incluir el campo '" + campo + "'",
                campo + " presente y no nulo",
                res.resumen(),
                "Revisa tu DTO de respuesta: probablemente ese campo no esta mapeado.");
    }

    /** Comprueba que el cuerpo NO trae el campo. Se usa para 'password'. */
    static void sinCampo(Api.Res res, String campo, String contexto) {
        boolean aparece = res.raw() != null && res.raw().contains("\"" + campo + "\"");
        que(!aparece,
                contexto + " NUNCA debe incluir el campo '" + campo + "'",
                "sin '" + campo + "' en la respuesta",
                res.resumen(),
                "Devuelve un DTO, no la entidad JPA. Si expones la entidad, expones la contrasena.");
    }

    /** El formato de error del enunciado: error, message, timestamp, path. */
    static void formatoError(Api.Res res, String contexto) {
        for (String campo : List.of("error", "message", "timestamp", "path")) {
            que(res.body() != null && res.body().hasNonNull(campo),
                    contexto + " debe usar el formato de error del enunciado (falta '" + campo + "')",
                    "{ error, message, timestamp, path }",
                    res.resumen(),
                    "Centralizalo en un @RestControllerAdvice. Para 401 y 403 ademas hacen falta "
                            + "un AuthenticationEntryPoint y un AccessDeniedHandler, porque esos dos "
                            + "los responde Spring Security antes de llegar a tu controller.");
        }
    }

    /** La estructura de pagina del enunciado. */
    static void formatoPagina(Api.Res res, int page, int size, String contexto) {
        que(res.campo("content") != null && res.campo("content").isArray(),
                contexto + " debe devolver 'content' como array",
                "content: []", res.resumen(),
                "Estructura exacta: content, totalElements, totalPages, currentPage, size.");
        for (String campo : List.of("totalElements", "totalPages")) {
            que(res.campo(campo) != null && res.campo(campo).isNumber(),
                    contexto + " debe devolver '" + campo + "' numerico",
                    campo + " numerico", res.resumen(), null);
        }
        igual(page, res.numero("currentPage"), contexto + " devuelve currentPage");
        igual(size, res.numero("size"), contexto + " devuelve size");
    }

    static void arrayNoVacio(Api.Res res, String contexto, String pista) {
        que(res.body() != null && res.body().isArray() && res.body().size() > 0,
                contexto, "un array con al menos un elemento", res.resumen(), pista);
    }

    /** Busca dentro de un array el primer objeto cuyo campo valga lo pedido. */
    static JsonNode buscar(Api.Res res, String campo, String valor) {
        if (res.body() == null || !res.body().isArray()) return null;
        for (JsonNode n : res.body()) {
            if (valor.equals(n.path(campo).asText(null))) return n;
        }
        return null;
    }

    private Verificar() {
    }
}
