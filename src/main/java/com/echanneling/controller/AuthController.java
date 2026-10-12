package com.echanneling.controller;

import com.echanneling.entity.User;
import com.echanneling.entity.Patient; // Missing import added
import com.echanneling.service.UserService;
import com.echanneling.service.DoctorService; // Cleaned up fully qualified package name

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@Controller
public class AuthController {
    @InitBinder("user")
    public void registrationFields(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "fullName", "email", "phoneNo", "address", "nic");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private com.echanneling.service.HospitalBranchService hospitalBranchService;

    // --- OOP Helper Method (Encapsulating Login Status Check) ---
    // This reusable method checks if a user is currently logged in or not
    private boolean isUserLoggedIn(Authentication authentication) {
        return authentication != null &&
                authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser");
    }

    // --- OOP Helper Method (Encapsulating Role-Based Redirection Logic) ---
    // This method decides the dashboard path according to user authority
    private String getDashboardRedirectPath(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        // Loop through all roles assigned to this authenticated user
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_DOCTOR".equals(role)) {
                return "redirect:/doctor/dashboard";
            } else if ("ROLE_RECEPTIONIST".equals(role)) {
                return "redirect:/reception/dashboard";
            }
        }
        // If the user is neither Admin nor Doctor, send them to Patient dashboard
        return "redirect:/patient/dashboard";
    }

    @Autowired private com.echanneling.service.BookingService bookingService;

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        model.addAttribute("doctors", doctorService.getAllDoctors().stream().filter(com.echanneling.service.BookingService::isBookable).toList());
        model.addAttribute("todayAvailability", bookingService.todayAvailability());
        model.addAttribute("branches", hospitalBranchService.getAllBranches());
        return "index";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        // Reusing OOP helper method to prevent already logged in users from seeing login page again
        if (isUserLoggedIn(authentication)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new Patient());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") Patient user) {
        if (user.getUsername() == null || user.getUsername().trim().length() < 4) {
            return "redirect:/register?error=short_username";
        }
        String cleanUsername = user.getUsername().trim();
        user.setUsername(cleanUsername);

        if (userService.findByUsername(cleanUsername).isPresent()) {
            return "redirect:/register?error=duplicate_username";
        }

        if (user.getEmail() == null || !user.getEmail().trim().contains("@")) {
            return "redirect:/register?error=invalid_email";
        }
        String cleanEmail = user.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        user.setEmail(cleanEmail);

        if (userService.emailExists(cleanEmail)) {
            return "redirect:/register?error=duplicate_email";
        }

        if (!com.echanneling.service.PasswordPolicy.valid(user.getPassword())) {
            return "redirect:/register?error=weak_password";
        }

        if (user.getNic() != null && !user.getNic().trim().isEmpty()) {
            String cleanNic = user.getNic().replaceAll("\\s+", "");
            user.setNic(cleanNic);
            if (!cleanNic.matches("^([0-9]{9}[xXvV]|[0-9]{12})$")) {
                return "redirect:/register?error=invalid_nic";
            }
        } else {
            return "redirect:/register?error=invalid_nic";
        }

        if (user.getPhoneNo() != null && !user.getPhoneNo().trim().isEmpty()) {
            String cleanPhone = user.getPhoneNo().trim();
            user.setPhoneNo(cleanPhone);
            if (!cleanPhone.matches("^\\+?[0-9\\s\\-()]{9,20}$")) {
                return "redirect:/register?error=invalid_phone";
            }
        } else {
            return "redirect:/register?error=invalid_phone";
        }

        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return "redirect:/register?error=invalid_name";
        } else {
            user.setFullName(user.getFullName().trim());
        }

        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            return "redirect:/register?error=invalid_address";
        } else {
            user.setAddress(user.getAddress().trim());
        }

        user.setRole("ROLE_PATIENT");
        user.setId(null);

        try {
            userService.saveUser(user);
            return "redirect:/login?registered";
        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            return "redirect:/register?error=registration_failed";
        }
    }

    @Autowired private com.echanneling.service.PasswordRecoveryService recovery;

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email) {
        recovery.request(email);
        return "redirect:/forgot-password?requested";
    }
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token",token); return "reset-password";
    }
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,@RequestParam String newPassword,
            org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try { recovery.reset(token,newPassword); return "redirect:/login?password_reset"; }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("errorMessage",ex.getMessage()); return "redirect:/forgot-password?invalid"; }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        // Reusing our custom OOP helper method to cleanly route the user
        return getDashboardRedirectPath(authentication);
    }
}
