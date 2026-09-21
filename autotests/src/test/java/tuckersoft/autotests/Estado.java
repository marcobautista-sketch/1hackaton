package tuckersoft.autotests;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado compartido entre estrellas: lo que una crea, la siguiente lo usa.
 *
 * Todo lo que se crea lleva el sufijo de la corrida, asi que los autotests se pueden
 * ejecutar veinte veces seguidas contra la misma base de datos sin chocar entre si
 * ni con los datos que el equipo haya metido a mano.
 */
final class Estado {

    static final String RUN = Long.toString(System.currentTimeMillis(), 36).toUpperCase();

    /** Credenciales del admin. Estan fijadas en el enunciado y en el .env del equipo. */
    static final String ADMIN_EMAIL = "colin@tuckersoft.co.uk";
    static final String ADMIN_PASSWORD = "colin1984";

    static String tokenAdmin;
    static String tokenUno;
    static String tokenDos;
    static String emailUno;
    static String emailDos;
    static Integer idUsuarioDos;
    static String tokenDosAntiguo;

    static String nodoCereal;
    static String nodoBus;
    static String nodoEspejo;
    static String nodoFinal;
    static String nodoRotoLink;
    static String nodoBucle;
    static String nodoOrigen;
    static String nodoFantasma;
    static Integer idNodoCereal;

    static Integer partidaFinalizada;
    static Integer partidaDeUno;
    static Integer partidaDeDos;

    static String codigo(String sufijo) {
        return "QA-" + RUN + "-" + sufijo;
    }

    // ----------------------------------------------------------- seguimiento

    record Marcador(int numero, String nombre) {
    }

    static final Map<Integer, String> NOMBRES = new LinkedHashMap<>();
    static final Map<Integer, Integer> COMPROBACIONES = new LinkedHashMap<>();
    static final Map<Integer, Boolean> APROBADOS = new LinkedHashMap<>();
    static final Map<Integer, String> PRIMER_FALLO = new LinkedHashMap<>();

    private static int actual = 0;
    private static boolean appCaida = false;

    static void iniciar(int numero, String nombre) {
        actual = numero;
        NOMBRES.putIfAbsent(numero, nombre);
        COMPROBACIONES.putIfAbsent(numero, 0);
    }

    static void sumarComprobacion() {
        COMPROBACIONES.merge(actual, 1, Integer::sum);
    }

    static void marcarFallo(int numero, String test, Throwable causa) {
        APROBADOS.put(numero, false);
        // El mensaje va primero: el marcador muestra solo la primera linea, y debe ser
        // "que se probaba", no el nombre del metodo.
        String detalle = resumen(causa);
        PRIMER_FALLO.putIfAbsent(numero, detalle.isBlank() ? test : detalle);
    }

    static void cerrar(int numero) {
        APROBADOS.putIfAbsent(numero, true);
    }

    static boolean aprobado(int numero) {
        return Boolean.TRUE.equals(APROBADOS.get(numero));
    }

    static void appCaida(String mensaje) {
        appCaida = true;
        PRIMER_FALLO.putIfAbsent(0, mensaje);
    }

    static boolean estaCaida() {
        return appCaida;
    }

    static int estrellas() {
        int n = 0;
        for (int i = 1; i <= 5; i++) {
            if (aprobado(i)) n++;
            else break;
        }
        return n;
    }

    private static String resumen(Throwable causa) {
        if (causa == null) return "";
        String mensaje = causa.getMessage();
        return mensaje == null ? "" : mensaje;
    }

    private Estado() {
    }
}
