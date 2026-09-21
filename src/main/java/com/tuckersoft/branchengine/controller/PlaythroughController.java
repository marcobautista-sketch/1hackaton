package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.PathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.security.CurrentUserProvider;
import com.tuckersoft.branchengine.service.PlaythroughService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaythroughResponse create(@Valid @RequestBody PlaythroughRequest request) {
        return playthroughService.create(request, currentUserProvider.getCurrentUser());
    }

    @GetMapping
    public List<PlaythroughResponse> list() {
        return playthroughService.listForCurrentUser(currentUserProvider.getCurrentUser());
    }

    @GetMapping("/{id}")
    public PlaythroughResponse getById(@PathVariable Long id) {
        return playthroughService.getById(id, currentUserProvider.getCurrentUser());
    }

    @GetMapping("/{id}/path")
    public PathResponse getPath(@PathVariable Long id) {
        return playthroughService.getPath(id, currentUserProvider.getCurrentUser());
    }
}
