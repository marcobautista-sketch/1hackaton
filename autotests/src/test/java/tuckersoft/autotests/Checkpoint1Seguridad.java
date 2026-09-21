package tuckersoft.autotests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * Generado por tuckersoft-qa-gen 0.9 a partir del contrato v1.2 del enunciado.
 * Pendiente de regenerar: aun no refleja la fe de erratas v1.3 de README.md
 * (claim 'role' en el JWT, 'role' respetado en el registro, 404 para email
 * inexistente). Ante diferencias con README.md, el contrato vigente es el README.
 */
@Checkpoint(value = 1, nombre = "SEGURIDAD")
@Order(1)
@ExtendWith(Checkpoints.class)
class Checkpoint1Seguridad {

    private static final String PISTA_ROL_BD = """
                Las autoridades se cargan de la base de datos en cada peticion, via
                UserDetailsService. Si metes el rol dentro del JWT, el token viejo
                sigue diciendo ROLE_USER y esto falla.""";

    private static ObjectNode registro(String email, String password, String displayName) {
        ObjectNode b = Api.json();
        b.put("email", email);
        b.put("password", password);
        b.put("displayName", displayName);
        return b;
    }

    private static ObjectNode credenciales(String email, String password) {
        ObjectNode b = Api.json();
        b.put("email", email);
        b.put("password", password);
        return b;
    }

    @Test
    @Order(1)
    void registro_crea_la_cuenta_y_devuelve_token() {
        Estado.emailUno = "qa-" + Estado.RUN.toLowerCase() + "-uno@tuckersoft.test";
        Api.Res res = Api.post("/api/v1/auth/register",
                registro(Estado.emailUno, "bandersnatch84", "Ada Lovelace"), null);

        Verificar.estado(201, res, "POST /api/v1/auth/register responde 201",
                "El registro crea un recurso: devuelve ResponseEntity.status(HttpStatus.CREATED).");
        Verificar.campo(res, "token", "El registro");
        Verificar.igual(Estado.emailUno, res.texto("email"), "El registro devuelve el email registrado");
        Verificar.igual("Ada Lovelace", res.texto("displayName"), "El registro devuelve el displayName");
        Verificar.igual("ROLE_USER", res.texto("role"),
                "Quien se registra queda como ROLE_USER",
                "El rol nunca se toma del request: se fija en el service.");
        Verificar.sinCampo(res, "password", "El registro");

        Estado.tokenUno = res.texto("token");
    }

    @Test
    @Order(2)
    void registro_con_email_repetido_responde_409() {
        Api.Res res = Api.post("/api/v1/auth/register",
                registro(Estado.emailUno, "otraclave123", "Otro Nombre"), null);
        Verificar.estado(409, res, "Un email ya registrado responde 409");
        Verificar.formatoError(res, "El 409 de email repetido");
    }

    @Test
    @Order(3)
    void registro_con_datos_invalidos_responde_400() {
        Api.Res sinArroba = Api.post("/api/v1/auth/register",
                registro("no-es-un-email", "bandersnatch84", "Ada Lovelace"), null);
        Verificar.estado(400, sinArroba, "Un email sin formato valido responde 400",
                "Anota @Email en el campo del DTO y @Valid en el @RequestBody.");

        Api.Res claveCorta = Api.post("/api/v1/auth/register",
                registro("qa-" + Estado.RUN.toLowerCase() + "-corta@tuckersoft.test", "123", "Ada Lovelace"), null);
        Verificar.estado(400, claveCorta, "Una contrasena de menos de 6 caracteres responde 400",
                "Anota @Size(min = 6) en el campo password del DTO de registro.");

        Api.Res nombreCorto = Api.post("/api/v1/auth/register",
                registro("qa-" + Estado.RUN.toLowerCase() + "-nombre@tuckersoft.test", "bandersnatch84", "Ad"), null);
        Verificar.estado(400, nombreCorto, "Un displayName de menos de 3 caracteres responde 400",
                "Anota @Size(min = 3, max = 60) en displayName.");
    }

    @Test
    @Order(4)
    void no_se_puede_pedir_ser_admin_desde_el_request() {
        Estado.emailDos = "qa-" + Estado.RUN.toLowerCase() + "-dos@tuckersoft.test";
        ObjectNode cuerpo = registro(Estado.emailDos, "bandersnatch84", "Alan Turing");
        cuerpo.put("role", "ROLE_ADMIN");

        Api.Res res = Api.post("/api/v1/auth/register", cuerpo, null);
        Verificar.estado(201, res, "El registro ignora el campo 'role' y responde 201");
        Verificar.igual("ROLE_USER", res.texto("role"),
                "Pedir ROLE_ADMIN en el registro no concede ROLE_ADMIN",
                "Escalada de privilegios: el rol se asigna en el service, nunca se copia del DTO.");

        Estado.tokenDos = res.texto("token");
    }

    @Test
    @Order(5)
    void login_correcto_devuelve_token() {
        Api.Res res = Api.post("/api/v1/auth/login",
                credenciales(Estado.emailUno, "bandersnatch84"), null);
        Verificar.estado(200, res, "POST /api/v1/auth/login responde 200");
        Verificar.campo(res, "token", "El login");
        Verificar.igual("ROLE_USER", res.texto("role"), "El login devuelve el rol");
        Verificar.sinCampo(res, "password", "El login");
        Estado.tokenUno = res.texto("token");
    }

    @Test
    @Order(6)
    void login_con_credenciales_malas_responde_401() {
        Api.Res claveMala = Api.post("/api/v1/auth/login",
                credenciales(Estado.emailUno, "esta-no-es"), null);
        Verificar.estado(401, claveMala, "Una contrasena incorrecta responde 401",
                "Captura BadCredentialsException. Si te sale 403 o 500, tu AuthenticationManager "
                        + "no esta configurado o la excepcion no esta mapeada.");
        Verificar.formatoError(claveMala, "El 401 de credenciales malas");

        Api.Res noExiste = Api.post("/api/v1/auth/login",
                credenciales("fantasma-" + Estado.RUN + "@tuckersoft.test", "loquesea"), null);
        Verificar.estado(401, noExiste, "Un email inexistente responde 401, no 404",
                "No se revela si el email existe: los dos casos son 401.");
    }

    @Test
    @Order(7)
    void sin_token_o_con_token_invalido_responde_401() {
        Api.Res sinToken = Api.get("/api/v1/users/me", null);
        Verificar.estado(401, sinToken, "GET /api/v1/users/me sin token responde 401");
        Verificar.formatoError(sinToken, "El 401 sin token");

        Api.Res basura = Api.getConAuthCrudo("/api/v1/users/me", "Bearer esto.no.es.un.jwt");
        Verificar.estado(401, basura, "Un token con firma invalida responde 401",
                "Tu filtro debe capturar las excepciones de jjwt y dejar pasar sin autenticar, "
                        + "no reventar con un 500.");

        Api.Res sinBearer = Api.getConAuthCrudo("/api/v1/users/me", Estado.tokenUno);
        Verificar.estado(401, sinBearer, "Una cabecera Authorization sin el prefijo 'Bearer ' responde 401",
                "El filtro solo debe aceptar cabeceras que empiecen exactamente con 'Bearer '.");
    }

    @Test
    @Order(8)
    void usuario_autenticado_ve_su_propia_cuenta() {
        Api.Res res = Api.get("/api/v1/users/me", Estado.tokenUno);
        Verificar.estado(200, res, "GET /api/v1/users/me con token responde 200");
        Verificar.igual(Estado.emailUno, res.texto("email"), "El token identifica al usuario correcto");
        Verificar.campo(res, "id", "GET /api/v1/users/me");
        Verificar.campo(res, "createdAt", "GET /api/v1/users/me");
        Verificar.sinCampo(res, "password", "GET /api/v1/users/me");
    }

    @Test
    @Order(9)
    void el_admin_existe_y_entra_con_las_credenciales_del_env() {
        Api.Res res = Api.post("/api/v1/auth/login",
                credenciales(Estado.ADMIN_EMAIL, Estado.ADMIN_PASSWORD), null);
        Verificar.estado(200, res, "El admin del DataInitializer puede entrar",
                "El DataInitializer crea al admin leyendo ADMIN_EMAIL y ADMIN_PASSWORD del .env, "
                        + "y guarda la contrasena codificada con BCrypt. Si esto falla: o no se creo, "
                        + "o guardaste la contrasena en texto plano y BCrypt no la reconoce.");
        Verificar.igual("ROLE_ADMIN", res.texto("role"),
                "El usuario administrador tiene ROLE_ADMIN",
                "Si aqui ves ROLE_USER, el administrador quedo degradado en tu base de datos. "
                + "El DataInitializer no lo arregla porque el usuario ya existe: o corriges el "
                + "rol a mano en PostgreSQL, o borras y recreas la base.");
        Estado.tokenAdmin = res.texto("token");
    }

    @Test
    @Order(10)
    void listar_usuarios_es_solo_para_admin() {
        Api.Res comoUsuario = Api.get("/api/v1/users", Estado.tokenUno);
        Verificar.estado(403, comoUsuario, "GET /api/v1/users con un usuario normal responde 403",
                "Restringe esa ruta a hasRole(\"ADMIN\") en el SecurityFilterChain.");
        Verificar.formatoError(comoUsuario, "El 403 de /api/v1/users");

        Api.Res comoAdmin = Api.get("/api/v1/users", Estado.tokenAdmin);
        Verificar.estado(200, comoAdmin, "GET /api/v1/users con el admin responde 200");
        Verificar.arrayNoVacio(comoAdmin, "GET /api/v1/users devuelve la lista de usuarios",
                "Debe devolver un array de DTOs.");
        Verificar.sinCampo(comoAdmin, "password", "El listado de usuarios");

        JsonNode admin = Verificar.buscar(comoAdmin, "email", Estado.ADMIN_EMAIL);
        Verificar.que(admin != null, "El listado incluye al administrador",
                "un elemento con email " + Estado.ADMIN_EMAIL, comoAdmin.resumen(), null);

        JsonNode dos = Verificar.buscar(comoAdmin, "email", Estado.emailDos);
        Verificar.que(dos != null, "El listado incluye a los usuarios registrados",
                "un elemento con email " + Estado.emailDos, comoAdmin.resumen(), null);
        Estado.idUsuarioDos = dos.path("id").asInt();
    }

    @Test
    @Order(11)
    void cambiar_el_rol_de_un_usuario_es_solo_para_admin() {
        ObjectNode aAdmin = Api.json();
        aAdmin.put("role", "ROLE_ADMIN");

        Api.Res intento = Api.patch("/api/v1/users/" + Estado.idUsuarioDos + "/role", aAdmin, Estado.tokenUno);
        Verificar.estado(403, intento, "PATCH /api/v1/users/{id}/role con un usuario normal responde 403",
                "Restringe PATCH /api/v1/users/*/role a hasRole(\"ADMIN\").");
        Verificar.formatoError(intento, "El 403 de cambio de rol");

        Api.Res sinToken = Api.patch("/api/v1/users/" + Estado.idUsuarioDos + "/role", aAdmin, null);
        Verificar.estado(401, sinToken, "Cambiar un rol sin token responde 401");
    }

    @Test
    @Order(12)
    void el_cambio_de_rol_valida_sus_casos_limite() {
        ObjectNode rolRaro = Api.json();
        rolRaro.put("role", "ROLE_SUPREMO");
        Verificar.estado(400, Api.patch("/api/v1/users/" + Estado.idUsuarioDos + "/role", rolRaro, Estado.tokenAdmin),
                "Un rol fuera de ROLE_USER / ROLE_ADMIN responde 400",
                "Valida el valor contra la lista antes de guardarlo.");

        ObjectNode aUser = Api.json();
        aUser.put("role", "ROLE_USER");
        Verificar.estado(404, Api.patch("/api/v1/users/999999999/role", aUser, Estado.tokenAdmin),
                "Un usuario inexistente responde 404");
    }

    @Test
    @Order(13)
    void el_rol_se_lee_de_la_base_de_datos_no_del_token() {
        ObjectNode nodoDePrueba = Api.json();
        nodoDePrueba.put("nodeCode", Estado.codigo("ROLTEST"));
        nodoDePrueba.put("title", "Escena de prueba de rol");
        nodoDePrueba.put("sceneText", "Nodo usado para comprobar que el rol se lee de la base de datos.");
        nodoDePrueba.put("branchCapacity", 5);

        Verificar.estado(403, Api.post("/api/v1/nodes", nodoDePrueba, Estado.tokenDos),
                "Antes de la promocion, ese usuario no puede crear nodos");

        ObjectNode aAdmin = Api.json();
        aAdmin.put("role", "ROLE_ADMIN");
        Api.Res promocion = Api.patch("/api/v1/users/" + Estado.idUsuarioDos + "/role", aAdmin, Estado.tokenAdmin);
        Verificar.estado(200, promocion, "El administrador promueve a otro usuario");
        Verificar.igual("ROLE_ADMIN", promocion.texto("role"), "La respuesta refleja el rol nuevo");
        Verificar.sinCampo(promocion, "password", "El cambio de rol");

        Verificar.estado(201, Api.post("/api/v1/nodes", nodoDePrueba, Estado.tokenDos),
                "Con el MISMO token de antes, el usuario promovido ya puede crear nodos",
                PISTA_ROL_BD);

        // La auto-degradacion se prueba sobre este administrador temporal, nunca sobre el
        // real: si al equipo le falta la validacion, degradar al admin del .env dejaria su
        // base de datos sin ningun administrador de forma permanente.
        ObjectNode aUser = Api.json();
        aUser.put("role", "ROLE_USER");
        Verificar.estado(400, Api.patch("/api/v1/users/" + Estado.idUsuarioDos + "/role", aUser, Estado.tokenDos),
                "Un administrador no puede cambiar su propio rol",
                "Si se degrada a si mismo puedes quedarte sin ningun administrador. "
                        + "Compara el id del objetivo con el del usuario del token.");

        // Se devuelve a ROLE_USER desde el administrador real, para que el resto de
        // estrellas siga probando el aislamiento entre usuarios normales.
        Verificar.estado(200, Api.patch("/api/v1/users/" + Estado.idUsuarioDos + "/role", aUser, Estado.tokenAdmin),
                "El administrador real devuelve al usuario a ROLE_USER");
    }
}
