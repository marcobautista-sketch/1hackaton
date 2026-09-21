package com.tuckersoft.branchengine.service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Reglas de clasificacion de decisiones. Sin IA ni servicios externos: el
 * resultado es siempre el mismo para la misma entrada.
 *
 * El orden de las reglas importa y esta verificado contra los autotests:
 * 1 ENTRADA_CORRUPTA, 2 RUPTURA_CUARTA_PARED, 3 SOSPECHA, 4 REBELDIA, 5 OBEDIENCIA.
 */
public final class DecisionClassifier {

    private static final Pattern SIN_LETRAS = Pattern.compile("[a-z]");

    private DecisionClassifier() {
    }

    /** Minusculas y sin tildes, para que "CÁMARA", "cámara" y "camara" comparen igual. */
    public static String normalize(String rawInput) {
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    public static String classify(String rawInput) {
        String texto = normalize(rawInput);

        if (!SIN_LETRAS.matcher(texto).find()) {
            return "ENTRADA_CORRUPTA";
        }
        if (contieneAlguna(texto, "netflix", "camara", "espectador", "videojuego")) {
            return "RUPTURA_CUARTA_PARED";
        }
        if (contieneAlguna(texto, "vigilan", "simbolo", "conspiracion")) {
            return "SOSPECHA";
        }
        if (contieneAlguna(texto, "rechaza", "destruye", "desobedece", "renuncia")) {
            return "REBELDIA";
        }
        return "OBEDIENCIA";
    }

    private static boolean contieneAlguna(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    public static String handlerUnitFor(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "Mesa de Guion";
            case "REBELDIA" -> "Control de Continuidad";
            case "SOSPECHA" -> "Oficina de Seguridad";
            case "RUPTURA_CUARTA_PARED" -> "Departamento Netflix";
            case "ENTRADA_CORRUPTA" -> "Archivo de Errores";
            default -> throw new IllegalStateException("branchType desconocido: " + branchType);
        };
    }

    public static String outcomeCodeFor(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "ADVANCE_MAIN_PATH";
            case "REBELDIA" -> "FORK_TIMELINE";
            case "SOSPECHA" -> "INJECT_WHITE_BEAR_SYMBOL";
            case "RUPTURA_CUARTA_PARED" -> "BREAK_FOURTH_WALL";
            case "ENTRADA_CORRUPTA" -> "DISCARD_INPUT";
            default -> throw new IllegalStateException("branchType desconocido: " + branchType);
        };
    }
}
