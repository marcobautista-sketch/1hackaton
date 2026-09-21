package com.tuckersoft.branchengine.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Todo lo que el listener necesita para enviar el correo y armar el log,
 * porque corre en otro hilo despues del commit: ahi ya no hay usuario
 * autenticado ni contexto de la peticion original.
 */
@Getter
@AllArgsConstructor
public class DecisionCommittedEvent {
    private final Long decisionId;
    private final String recipientEmail;
    private final String recipientDisplayName;
    private final String playerTag;
    private final String branchType;
    private final String impactLevel;
    private final String handlerUnit;
    private final String outcomeCode;
    private final String sourceNodeCode;
    private final String resolvedNodeCode;
    private final String playthroughStatus;
    private final Integer lucidity;
    private final Integer controlLevel;
    private final String endingCode;
    private final String rawInput;
    private final Instant createdAt;
    /** true si el request trajo X-Bandersnatch-Simulate: MAIL_FAILURE */
    private final boolean simulateMailFailure;
}
