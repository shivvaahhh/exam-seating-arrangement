package com.example.examseating.controller;

import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import com.example.examseating.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                    return "redirect:/admin/dashboard";
                } else if ("ROLE_STUDENT".equals(auth.getAuthority())) {
                    return "redirect:/student/dashboard";
                } else if ("ROLE_INVIGILATOR".equals(auth.getAuthority())) {
                    return "redirect:/invigilator/dashboard";
                }
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Authentication authentication,
                        Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        if (error != null) {
            if ("true".equalsIgnoreCase(error) || error.trim().isEmpty()) {
                model.addAttribute("errorMessage", "Invalid username or password");
            } else {
                model.addAttribute("errorMessage", error);
            }
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = getUserFromAuth(authentication);
        model.addAttribute("mustChangePassword", user != null && user.isMustChangePassword());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@RequestParam("currentPassword") String currentPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = getUserFromAuth(authentication);
        if (user == null) {
            return "redirect:/login";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("errorMessage", "Current password is incorrect.");
            model.addAttribute("mustChangePassword", user.isMustChangePassword());
            return "change-password";
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            model.addAttribute("errorMessage", "New password must be at least 6 characters long.");
            model.addAttribute("mustChangePassword", user.isMustChangePassword());
            return "change-password";
        }

        if (newPassword.equals(currentPassword)) {
            model.addAttribute("errorMessage", "New password cannot be the same as your current password.");
            model.addAttribute("mustChangePassword", user.isMustChangePassword());
            return "change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "New password and confirm password do not match.");
            model.addAttribute("mustChangePassword", user.isMustChangePassword());
            return "change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");

        if ("ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        } else if ("ROLE_STUDENT".equals(user.getRole())) {
            return "redirect:/student/dashboard";
        } else if ("ROLE_INVIGILATOR".equals(user.getRole())) {
            return "redirect:/invigilator/dashboard";
        }
        return "redirect:/";
    }

    private User getUserFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findByUsernameIgnoreCase(userDetails.getUsername()).orElse(null);
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
    }
}
