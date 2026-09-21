package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.NodeRequest;
import com.tuckersoft.branchengine.dto.NodeResponse;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final StoryNodeRepository storyNodeRepository;

    @Transactional
    public NodeResponse create(NodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.getNodeCode())) {
            throw ApiException.conflict("Ya existe un nodo con ese nodeCode.");
        }

        StoryNode node = new StoryNode();
        node.setNodeCode(request.getNodeCode());
        node.setTitle(request.getTitle());
        node.setSceneText(request.getSceneText());
        node.setBranchCapacity(request.getBranchCapacity());
        node.setCurrentBranches(0);
        node.setPrimaryBranchCode(request.getPrimaryBranchCode());
        node.setGlitchBranchCode(request.getGlitchBranchCode());
        node.setCreatedAt(Instant.now());
        storyNodeRepository.save(node);

        return toResponse(node);
    }

    public List<NodeResponse> listAll() {
        return storyNodeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public NodeResponse getById(Long id) {
        StoryNode node = storyNodeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe un nodo con id " + id));
        return toResponse(node);
    }

    private NodeResponse toResponse(StoryNode node) {
        return new NodeResponse(node.getId(), node.getNodeCode(), node.getTitle(), node.getSceneText(),
                node.getBranchCapacity(), node.getCurrentBranches(), node.getPrimaryBranchCode(),
                node.getGlitchBranchCode(), node.getCreatedAt());
    }
}
