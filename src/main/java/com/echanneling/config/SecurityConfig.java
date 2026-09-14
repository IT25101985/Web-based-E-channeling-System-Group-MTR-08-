package com.echanneling.config;

import com.echanneling.entity.User;
import com.echanneling.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/login", "/forgot-password", "/error", "/css/**", "/js/**", "/images/**", "/api/**", "/invoice/download/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/patient/**", "/pay", "/feedback/add").hasRole("PATIENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("superSecretKey")
                        .alwaysRemember(true)
                        .tokenValiditySeconds(86400 * 30)) // 30 days
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?expired")
                        .maximumSessions(1)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                String role = user.getRole();
                if (role == null || role.trim().isEmpty()) {
                    role = "ROLE_PATIENT"; // Default safety role
                }

                // Ensure the role string passed to .roles() does NOT have ROLE_ prefix
                String roleName = role.replace("ROLE_", "");

                return org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(roleName)
                        .build();
            }
            throw new UsernameNotFoundException("User not found: " + username);
        };
    }
}
