package com.example.examseating.security;

import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String errorMessage = "Invalid username or password";

        if (username != null && !username.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username.trim());
            if (userOpt.isPresent() && !userOpt.get().isActive()) {
                errorMessage = "Account disabled, contact admin";
            }
        }

        if (exception instanceof DisabledException) {
            errorMessage = "Account disabled, contact admin";
        }

        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/login?error=" + encodedError);
    }
}
