package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.dto.PageResponse;
import com.tuckersoft.branchengine.dto.RealityLogResponse;
import com.tuckersoft.branchengine.security.CurrentUserProvider;
import com.tuckersoft.branchengine.service.DecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private static final String SIMULATE_HEADER = "X-Bandersnatch-Simulate";
    private static final String SIMULATE_MAIL_FAILURE = "MAIL_FAILURE";

    private final DecisionService decisionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionResponse create(@Valid @RequestBody DecisionRequest request,
                                    @RequestHeader(value = SIMULATE_HEADER, required = false) String simulateHeader) {
        boolean simulateFailure = SIMULATE_MAIL_FAILURE.equals(simulateHeader);
        return decisionService.create(request, currentUserProvider.getCurrentUser(), simulateFailure);
    }

    @GetMapping
    public PageResponse<DecisionResponse> search(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return decisionService.search(branchType, impactLevel, status, playthroughId, page, size,
                currentUserProvider.getCurrentUser());
    }

    @GetMapping("/{id}")
    public DecisionResponse getById(@PathVariable Long id) {
        return decisionService.getById(id, currentUserProvider.getCurrentUser());
    }

    @GetMapping("/{id}/reality-logs")
    public List<RealityLogResponse> getRealityLogs(@PathVariable Long id) {
        return decisionService.getRealityLogs(id, currentUserProvider.getCurrentUser());
    }
}
