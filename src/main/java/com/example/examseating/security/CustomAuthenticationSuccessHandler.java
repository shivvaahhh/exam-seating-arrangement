package com.example.examseating.security;

import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(LoginAttemptService loginAttemptService,
                                              UserRepository userRepository) {
        this.loginAttemptService = loginAttemptService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String username = authentication.getName();
        loginAttemptService.loginSucceeded(username);

        // Update last login timestamp and check must_change_password
        boolean mustChangePassword = false;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userDetails.getUser();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            mustChangePassword = user.isMustChangePassword();
        } else {
            userRepository.findByUsernameIgnoreCase(username).ifPresent(u -> {
                u.setLastLogin(LocalDateTime.now());
                userRepository.save(u);
            });
        }

        if (mustChangePassword) {
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } else if ("ROLE_STUDENT".equals(authority.getAuthority())) {
                response.sendRedirect(request.getContextPath() + "/student/dashboard");
                return;
            } else if ("ROLE_INVIGILATOR".equals(authority.getAuthority())) {
                response.sendRedirect(request.getContextPath() + "/invigilator/dashboard");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/");
    }
}
