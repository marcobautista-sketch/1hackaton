package com.tuckersoft.branchengine.security;

import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resuelve el User autenticado a partir del SecurityContext. */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "No hay usuario autenticado.");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "El usuario del token ya no existe."));
    }

    public boolean isAdmin(User user) {
        return "ROLE_ADMIN".equals(user.getRole());
    }
}
