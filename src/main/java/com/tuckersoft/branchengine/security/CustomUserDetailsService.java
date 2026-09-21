package com.tuckersoft.branchengine.security;

import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busca al usuario por email y construye sus authorities a partir del
 * "role" guardado en la base de datos. Se invoca en cada peticion desde
 * JwtAuthFilter, no solo en el login, para que los cambios de rol se
 * apliquen de inmediato sin reemitir el token.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                .build();
    }
}
