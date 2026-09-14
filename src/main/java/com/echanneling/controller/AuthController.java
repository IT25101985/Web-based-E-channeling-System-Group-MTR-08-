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

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

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
            }
        }
        // If the user is neither Admin nor Doctor, send them to Patient dashboard
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        model.addAttribute("doctors", doctorService.getAllDoctors());
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
        System.out.println("Registering user: " + user.getUsername());

        user.setRole("ROLE_PATIENT"); // Default role assigned automatically
        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            user.setAddress("Residential Address");
        }
        if (user.getPhoneNo() == null || user.getPhoneNo().trim().isEmpty()) {
            user.setPhoneNo("0770000000");
        }
        userService.saveUser(user);

        return "redirect:/login?registered";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String username, @RequestParam String newPassword) {
        Optional<User> optUser = userService.findByUsername(username);

        if (optUser.isPresent()) {
            User user = optUser.get();
            user.setPassword(newPassword); // Overwriting the old password
            userService.saveUser(user);
            return "redirect:/forgot-password?success";
        }
        return "redirect:/forgot-password?error";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        // Reusing our custom OOP helper method to cleanly route the user
        return getDashboardRedirectPath(authentication);
    }
}