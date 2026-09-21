package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.RoleUpdateRequest;
import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.security.CurrentUserProvider;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    public UserResponse me() {
        return userService.me(currentUserProvider.getCurrentUser());
    }

    @GetMapping
    public List<UserResponse> listAll() {
        return userService.listAll();
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return userService.updateRole(id, request, currentUserProvider.getCurrentUser());
    }
}
