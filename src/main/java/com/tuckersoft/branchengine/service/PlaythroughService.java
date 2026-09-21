package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.PathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import com.tuckersoft.branchengine.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public PlaythroughResponse create(PlaythroughRequest request, User owner) {
        StoryNode node = storyNodeRepository.findByNodeCode(request.getStartNodeCode())
                .orElseThrow(() -> ApiException.notFound(
                        "No existe un nodo con nodeCode " + request.getStartNodeCode()));

        if (playthroughRepository.existsByPlayerTag(request.getPlayerTag())) {
            throw ApiException.conflict("Ya existe una partida con ese playerTag.");
        }

        if (node.getCurrentBranches() >= node.getBranchCapacity()) {
            throw ApiException.badRequest("El nodo " + node.getNodeCode() + " esta lleno.");
        }

        Playthrough playthrough = new Playthrough();
        playthrough.setPlayerTag(request.getPlayerTag());
        playthrough.setUser(owner);
        playthrough.setStartNodeCode(node.getNodeCode());
        playthrough.setCurrentNode(node);
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus("ACTIVA");
        playthrough.setEndingCode(null);
        Instant now = Instant.now();
        playthrough.setCreatedAt(now);
        playthrough.setUpdatedAt(now);

        node.setCurrentBranches(node.getCurrentBranches() + 1);
        storyNodeRepository.save(node);

        playthroughRepository.save(playthrough);
        return toResponse(playthrough);
    }

    public List<PlaythroughResponse> listForCurrentUser(User current) {
        List<Playthrough> playthroughs = currentUserProvider.isAdmin(current)
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findAllByUser_IdOrderByCreatedAtDesc(current.getId());

        return playthroughs.stream().map(this::toResponse).toList();
    }

    public PlaythroughResponse getById(Long id, User current) {
        Playthrough playthrough = findOwnedOrAdmin(id, current);
        return toResponse(playthrough);
    }

    public PathResponse getPath(Long id, User current) {
        Playthrough playthrough = findOwnedOrAdmin(id, current);

        List<Decision> decisions = decisionRepository
                .findAllByPlaythrough_IdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(playthrough.getId());

        List<PathResponse.Step> steps = new java.util.ArrayList<>();
        int order = 1;
        for (Decision d : decisions) {
            steps.add(new PathResponse.Step(order++, d.getId(), d.getNode().getNodeCode(),
                    d.getResolvedNodeCode(), d.getBranchType(), d.getImpactLevel(), d.getCreatedAt()));
        }

        return new PathResponse(playthrough.getId(), playthrough.getPlayerTag(), playthrough.getStatus(),
                playthrough.getEndingCode(), playthrough.getStartNodeCode(),
                playthrough.getCurrentNode().getNodeCode(), steps);
    }

    /** Carga la partida y valida propiedad: dueno o admin, 403 para el resto. */
    Playthrough findOwnedOrAdmin(Long id, User current) {
        Playthrough playthrough = playthroughRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe una partida con id " + id));

        boolean esDueno = playthrough.getUser().getId().equals(current.getId());
        if (!esDueno && !currentUserProvider.isAdmin(current)) {
            throw ApiException.forbidden("No puedes acceder a una partida que no es tuya.");
        }
        return playthrough;
    }

    private PlaythroughResponse toResponse(Playthrough p) {
        return new PlaythroughResponse(p.getId(), p.getPlayerTag(), p.getUser().getEmail(),
                p.getStartNodeCode(), p.getCurrentNode().getNodeCode(), p.getLucidity(), p.getControlLevel(),
                p.getStatus(), p.getEndingCode(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
