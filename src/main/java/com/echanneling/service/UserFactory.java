package com.echanneling.service;

import com.echanneling.entity.*;
import org.springframework.stereotype.Component;
import java.util.Locale;

/** Simple Factory centralizes the supported account creation rules. */
@Component
public class UserFactory {
    public User createUser(String role) {
        if (role == null) throw new IllegalArgumentException("Choose an account role.");
        String value = role.toUpperCase(Locale.ROOT).replace("ROLE_", "");
        User user = switch (value) {
            case "PATIENT" -> new Patient();
            case "ADMIN" -> new Admin();
            // Doctor is a separate profile entity; staff accounts use User.
            case "DOCTOR", "RECEPTIONIST" -> new User();
            default -> throw new IllegalArgumentException("Unsupported account role.");
        };
        user.setRole("ROLE_" + value);
        return user;
    }
}
