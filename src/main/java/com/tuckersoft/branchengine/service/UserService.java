package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.RoleUpdateRequest;
import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> ROLES_VALIDOS = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;

    public UserResponse me(User current) {
        return toResponse(current);
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, RoleUpdateRequest request, User current) {
        if (!ROLES_VALIDOS.contains(request.getRole())) {
            throw ApiException.badRequest("role debe ser ROLE_USER o ROLE_ADMIN.");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe un usuario con id " + id));

        if (target.getId().equals(current.getId())) {
            throw ApiException.badRequest("Un administrador no puede cambiar su propio rol.");
        }

        target.setRole(request.getRole());
        userRepository.save(target);
        return toResponse(target);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getCreatedAt());
    }
}
