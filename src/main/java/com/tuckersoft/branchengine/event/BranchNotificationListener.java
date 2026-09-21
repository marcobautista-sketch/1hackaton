package com.tuckersoft.branchengine.event;

import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Envia el Informe de Realidad DESPUES de que PostgreSQL confirmo la
 * transaccion que guardo la Decision (AFTER_COMMIT). Corre en su propio
 * hilo (branch-worker-N) y en su propia transaccion (REQUIRES_NEW), porque
 * un @Transactional normal sobre un @TransactionalEventListener no arranca.
 *
 * Aqui ya NO hay usuario autenticado: todo lo que hace falta viaja dentro
 * del evento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BranchNotificationListener {

    private final JavaMailSender mailSender;
    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;

    @Value("${spring.mail.username:tuckersoft@bandersnatch.test}")
    private String remitente;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.getDecisionId()).orElse(null);
        if (decision == null) {
            log.error("[BRANCH-LOG] No se encontro la decision {} para notificar", evento.getDecisionId());
            return;
        }

        decision.setStatus("PROCESANDO");
        decisionRepository.save(decision);

        String subject = "[TUCKERSOFT] " + evento.getBranchType() + " en " + evento.getPlayerTag()
                + " | Impacto " + evento.getImpactLevel();
        String body = construirCuerpo(evento);

        RealityLog realityLog = new RealityLog();
        realityLog.setDecision(decision);
        realityLog.setRecipientEmail(evento.getRecipientEmail());
        realityLog.setSubject(subject);
        realityLog.setCreatedAt(Instant.now());

        try {
            if (evento.isSimulateMailFailure()) {
                throw new IllegalStateException("Fallo simulado por X-Bandersnatch-Simulate: MAIL_FAILURE");
            }

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(evento.getRecipientEmail());
            mensaje.setSubject(subject);
            mensaje.setText(body);
            mailSender.send(mensaje);

            realityLog.setLogStatus("SENT");
            realityLog.setErrorMessage(null);
            realityLog.setSentAt(Instant.now());
            decision.setStatus("ESTABILIZADA");
        } catch (Exception e) {
            realityLog.setLogStatus("FAILED");
            realityLog.setErrorMessage(e.getMessage());
            realityLog.setSentAt(null);
            decision.setStatus("ERROR");
            log.error("[BRANCH-LOG] Fallo el envio del Informe de Realidad para la decision {}: {}",
                    decision.getId(), e.getMessage());
        }

        decisionRepository.save(decision);
        realityLogRepository.save(realityLog);

        log.info("[BRANCH-LOG] Decision ID: {} | Player: {} | Branch: {} | Impact: {} | Unit: {} | "
                        + "Node: {} -> {} | Thread: {} | Status: {}",
                decision.getId(), evento.getPlayerTag(), evento.getBranchType(), evento.getImpactLevel(),
                evento.getHandlerUnit(), evento.getSourceNodeCode(), evento.getResolvedNodeCode(),
                Thread.currentThread().getName(), decision.getStatus());
    }

    private String construirCuerpo(DecisionCommittedEvent evento) {
        String finalCode = evento.getEndingCode() == null ? "-" : evento.getEndingCode();
        String fecha = DateTimeFormatter.ISO_INSTANT.format(evento.getCreatedAt());

        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                Decisión original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                evento.getRecipientDisplayName(),
                evento.getDecisionId(),
                evento.getPlayerTag(),
                evento.getBranchType(),
                evento.getImpactLevel(),
                evento.getHandlerUnit(),
                evento.getOutcomeCode(),
                evento.getSourceNodeCode(),
                evento.getResolvedNodeCode() == null ? "-" : evento.getResolvedNodeCode(),
                evento.getPlaythroughStatus(),
                evento.getLucidity(),
                evento.getControlLevel(),
                finalCode,
                fecha,
                evento.getRawInput());
    }
}
