package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.dto.PageResponse;
import com.tuckersoft.branchengine.dto.RealityLogResponse;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.RealityLog;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import com.tuckersoft.branchengine.security.CurrentUserProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private static final Set<String> IMPACTOS_VALIDOS = Set.of("LEVE", "MODERADO", "GRAVE", "CRITICO");

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final RealityLogRepository realityLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionResponse create(DecisionRequest request, User current, boolean simulateMailFailure) {
        // 1. Cargar la partida y validar propiedad: SOLO el dueno, admin incluido.
        Playthrough playthrough = playthroughRepository.findById(request.getPlaythroughId())
                .orElseThrow(() -> ApiException.notFound(
                        "No existe una partida con id " + request.getPlaythroughId()));

        if (!playthrough.getUser().getId().equals(current.getId())) {
            throw ApiException.forbidden("Solo el dueno de la partida puede decidir sobre ella.");
        }

        // 2. Validar el request y que la partida este ACTIVA.
        if (!IMPACTOS_VALIDOS.contains(request.getImpactLevel())) {
            throw ApiException.badRequest("impactLevel debe ser LEVE, MODERADO, GRAVE o CRITICO.");
        }
        if (!"ACTIVA".equals(playthrough.getStatus())) {
            throw ApiException.conflict("La partida ya esta FINALIZADA y no acepta mas decisiones.");
        }

        StoryNode sourceNode = playthrough.getCurrentNode();

        // 3. Clasificar el rawInput y derivar handlerUnit / outcomeCode.
        String branchType = DecisionClassifier.classify(request.getRawInput());
        String handlerUnit = DecisionClassifier.handlerUnitFor(branchType);
        String outcomeCode = DecisionClassifier.outcomeCodeFor(branchType);

        Instant now = Instant.now();
        Decision decision = new Decision();
        decision.setPlaythrough(playthrough);
        decision.setNode(sourceNode);
        decision.setRawInput(request.getRawInput());
        decision.setBranchType(branchType);
        decision.setImpactLevel(request.getImpactLevel());
        decision.setHandlerUnit(handlerUnit);
        decision.setOutcomeCode(outcomeCode);
        decision.setCreatedAt(now);
        decision.setUpdatedAt(now);

        // 4. ENTRADA_CORRUPTA: se guarda, pero no toca la partida ni publica evento.
        if ("ENTRADA_CORRUPTA".equals(branchType)) {
            decision.setResolvedNodeCode(null);
            decision.setStatus("ERROR");
            decisionRepository.save(decision);

            return toResponse(decision, playthrough, sourceNode.getNodeCode());
        }

        // 5. Aplicar stats, resolver nodo destino y estado de la partida.
        int[] stats = aplicarStats(playthrough.getLucidity(), playthrough.getControlLevel(),
                request.getImpactLevel());
        int nuevaLucidez = stats[0];
        int nuevoControl = stats[1];

        String resolvedNodeCode = resolverNodoDestino(sourceNode, branchType, request.getImpactLevel());

        aplicarEstadoPartida(playthrough, nuevaLucidez, nuevoControl, resolvedNodeCode);
        playthrough.setUpdatedAt(now);

        // 6. Guardar Playthrough.
        playthroughRepository.save(playthrough);

        // 7. Guardar la Decision con status = REGISTRADA.
        decision.setResolvedNodeCode(resolvedNodeCode);
        decision.setStatus("REGISTRADA");
        decisionRepository.save(decision);

        // 8. Publicar DecisionCommittedEvent (solo AFTER_COMMIT lo procesa).
        eventPublisher.publishEvent(new DecisionCommittedEvent(
                decision.getId(),
                playthrough.getUser().getEmail(),
                playthrough.getUser().getDisplayName(),
                playthrough.getPlayerTag(),
                branchType,
                request.getImpactLevel(),
                handlerUnit,
                outcomeCode,
                sourceNode.getNodeCode(),
                resolvedNodeCode,
                playthrough.getStatus(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getEndingCode(),
                request.getRawInput(),
                now,
                simulateMailFailure));

        // 9. Retornar 201.
        return toResponse(decision, playthrough, sourceNode.getNodeCode());
    }

    /** Paso 1: stats segun impacto, topados entre 0 y 100. */
    private int[] aplicarStats(int lucidezActual, int controlActual, String impactLevel) {
        int deltaLucidez;
        int deltaControl;
        switch (impactLevel) {
            case "LEVE" -> {
                deltaLucidez = -5;
                deltaControl = 5;
            }
            case "MODERADO" -> {
                deltaLucidez = -15;
                deltaControl = 10;
            }
            case "GRAVE" -> {
                deltaLucidez = -30;
                deltaControl = 20;
            }
            case "CRITICO" -> {
                deltaLucidez = -40;
                deltaControl = 45;
            }
            default -> throw new IllegalStateException("impactLevel desconocido: " + impactLevel);
        }
        int nuevaLucidez = Math.max(0, Math.min(100, lucidezActual + deltaLucidez));
        int nuevoControl = Math.max(0, Math.min(100, controlActual + deltaControl));
        return new int[]{nuevaLucidez, nuevoControl};
    }

    /** Paso 2: RUPTURA_CUARTA_PARED o impacto CRITICO usan glitchBranchCode; el resto, primaryBranchCode. */
    private String resolverNodoDestino(StoryNode sourceNode, String branchType, String impactLevel) {
        boolean usaGlitch = "RUPTURA_CUARTA_PARED".equals(branchType) || "CRITICO".equals(impactLevel);
        return usaGlitch ? sourceNode.getGlitchBranchCode() : sourceNode.getPrimaryBranchCode();
    }

    /** Paso 3: evaluar en este orden exacto: controlLevel >= 100, luego lucidity <= 0, luego nodo destino. */
    private void aplicarEstadoPartida(Playthrough playthrough, int nuevaLucidez, int nuevoControl,
                                       String resolvedNodeCode) {
        playthrough.setLucidity(nuevaLucidez);
        playthrough.setControlLevel(nuevoControl);

        if (nuevoControl >= 100) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_PAC_SYMBOL");
            return;
        }
        if (nuevaLucidez <= 0) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_WHITE_BEAR");
            return;
        }

        StoryNode destino = resolvedNodeCode == null ? null
                : storyNodeRepository.findByNodeCode(resolvedNodeCode).orElse(null);

        if (destino == null) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_NETFLIX_CUT");
            return;
        }

        playthrough.setStatus("ACTIVA");
        playthrough.setCurrentNode(destino);
    }

    public DecisionResponse getById(Long id, User current) {
        Decision decision = findOwnedOrAdmin(id, current);
        return toResponse(decision, decision.getPlaythrough(), decision.getNode().getNodeCode());
    }

    public List<RealityLogResponse> getRealityLogs(Long decisionId, User current) {
        Decision decision = findOwnedOrAdmin(decisionId, current);
        return realityLogRepository.findAllByDecision_IdOrderByCreatedAtAsc(decision.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<DecisionResponse> search(String branchType, String impactLevel, String status,
                                                  Long playthroughId, int page, int size, User current) {
        Specification<Decision> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!currentUserProvider.isAdmin(current)) {
                predicates.add(cb.equal(root.get("playthrough").get("user").get("id"), current.getId()));
            }
            if (branchType != null && !branchType.isBlank()) {
                predicates.add(cb.equal(root.get("branchType"), branchType));
            }
            if (impactLevel != null && !impactLevel.isBlank()) {
                predicates.add(cb.equal(root.get("impactLevel"), impactLevel));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (playthroughId != null) {
                predicates.add(cb.equal(root.get("playthrough").get("id"), playthroughId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Decision> resultado = decisionRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        List<DecisionResponse> content = resultado.getContent().stream()
                .map(d -> toResponse(d, d.getPlaythrough(), d.getNode().getNodeCode()))
                .toList();

        return new PageResponse<>(content, resultado.getTotalElements(), resultado.getTotalPages(),
                page, size);
    }

    /** Carga la decision y valida propiedad via la partida: dueno o admin, 403 para el resto. */
    private Decision findOwnedOrAdmin(Long id, User current) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe una decision con id " + id));

        boolean esDueno = decision.getPlaythrough().getUser().getId().equals(current.getId());
        if (!esDueno && !currentUserProvider.isAdmin(current)) {
            throw ApiException.forbidden("No puedes acceder a una decision que no es tuya.");
        }
        return decision;
    }

    private DecisionResponse toResponse(Decision d, Playthrough playthrough, String sourceNodeCode) {
        return new DecisionResponse(d.getId(), playthrough.getId(), playthrough.getPlayerTag(),
                sourceNodeCode, d.getResolvedNodeCode(), d.getRawInput(), d.getBranchType(),
                d.getImpactLevel(), d.getHandlerUnit(), d.getOutcomeCode(), d.getStatus(),
                playthrough.getStatus(), playthrough.getLucidity(), playthrough.getControlLevel(),
                playthrough.getEndingCode(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private RealityLogResponse toResponse(RealityLog log) {
        return new RealityLogResponse(log.getId(), log.getDecision().getId(), log.getRecipientEmail(),
                log.getSubject(), log.getLogStatus(), log.getErrorMessage(), log.getSentAt(),
                log.getCreatedAt());
    }
}
