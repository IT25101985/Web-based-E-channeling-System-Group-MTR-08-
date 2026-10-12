package com.echanneling.service;

import com.echanneling.entity.User;
import com.echanneling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired private org.springframework.security.core.session.SessionRegistry sessions;
    @Autowired private com.echanneling.repository.PasswordResetRepository resets;


    public User saveUser(User user) {
        if (user.getId() == null && userRepository.findByUsername(user.getUsername()).isPresent()) throw new IllegalArgumentException("Username already exists");
        if (user.getId() != null) throw new IllegalArgumentException("Use the explicit password change operation for an existing account.");
        PasswordPolicy.requireValid(user.getPassword());
        user.setEmail(user.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) throw new IllegalArgumentException("Email already exists");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    /** Raw user input is always encoded, even when it resembles a BCrypt hash. */
    @Transactional
    public User saveWithNewPassword(User user, String rawPassword) {
        PasswordPolicy.requireValid(rawPassword);
        User target=userRepository.findLockedById(user.getId()).orElseThrow();
        target.setPassword(passwordEncoder.encode(rawPassword));
        User saved=userRepository.save(target);
        resets.findById(user.getId()).ifPresent(reset -> reset.setUsedAt(java.time.LocalDateTime.now()));
        // Expire sessions only after the password transaction successfully commits.
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                public void afterCommit() {
                    for(Object principal:sessions.getAllPrincipals()) {
                        if(principal instanceof org.springframework.security.core.userdetails.UserDetails details
                                && user.getUsername().equals(details.getUsername()))
                            sessions.getAllSessions(principal,false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
                    }
                }
            });
        return saved;
    }

    public boolean emailExists(String email) { return userRepository.existsByEmailIgnoreCase(email); }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void setAccountActive(Long id, boolean active) {
        User user = userRepository.findById(id).orElseThrow();
        if ("ROLE_ADMIN".equals(user.getRole())) throw new IllegalArgumentException("Administrator accounts are protected.");
        user.setActive(active);
        userRepository.save(user);
    }

    public User updateUserProfile(User currentUser, User updatedInfo) {
        currentUser.setFullName(updatedInfo.getFullName());
        currentUser.setAddress(updatedInfo.getAddress());
        currentUser.setPhoneNo(updatedInfo.getPhoneNo());
        currentUser.setProfileImage(updatedInfo.getProfileImage());
        return userRepository.save(currentUser);
    }
    @Transactional
    public void deleteUser(User user) {
        if (user == null || user.getId() == null) return;
        deleteUserById(user.getId());
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (id != null) setAccountActive(id, false);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
}
