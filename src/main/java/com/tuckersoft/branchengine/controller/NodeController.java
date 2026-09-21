package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.NodeRequest;
import com.tuckersoft.branchengine.dto.NodeResponse;
import com.tuckersoft.branchengine.service.NodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NodeResponse create(@Valid @RequestBody NodeRequest request) {
        return nodeService.create(request);
    }

    @GetMapping
    public List<NodeResponse> listAll() {
        return nodeService.listAll();
    }

    @GetMapping("/{id}")
    public NodeResponse getById(@PathVariable Long id) {
        return nodeService.getById(id);
    }
}
