package com.example.examseating.security;

import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MustChangePasswordInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public MustChangePasswordInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String path = uri.substring(contextPath.length());

            // Allowed paths when must_change_password is true
            if (path.startsWith("/change-password") ||
                path.startsWith("/logout") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/favicon.ico")) {
                return true;
            }

            // Check if user must change password
            boolean mustChange = false;
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                // Refresh from DB or check userDetails
                User user = userRepository.findByUsernameIgnoreCase(userDetails.getUsername()).orElse(null);
                if (user != null && user.isMustChangePassword()) {
                    mustChange = true;
                }
            } else {
                User user = userRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
                if (user != null && user.isMustChangePassword()) {
                    mustChange = true;
                }
            }

            if (mustChange) {
                response.sendRedirect(contextPath + "/change-password");
                return false;
            }
        }

        return true;
    }
}
