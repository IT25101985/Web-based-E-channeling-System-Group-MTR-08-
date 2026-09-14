package com.echanneling.service;

import com.echanneling.entity.User;
import com.echanneling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public User saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$") && !user.getPassword().startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
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
        Long userId = user.getId();
        // Manually cascade deletes to avoid foreign key constraint violations
        jdbcTemplate.update("DELETE FROM prescription WHERE medical_record_id IN (SELECT id FROM medical_record WHERE patient_id = ?)", userId);
        jdbcTemplate.update("DELETE FROM invoice WHERE appointment_id IN (SELECT id FROM appointment WHERE patient_id = ? OR doctor_id = ?)", userId, userId);
        jdbcTemplate.update("DELETE FROM medical_record WHERE patient_id = ? OR appointment_id IN (SELECT id FROM appointment WHERE doctor_id = ?)", userId, userId);
        jdbcTemplate.update("DELETE FROM appointment WHERE patient_id = ? OR doctor_id = ?", userId, userId);
        
        userRepository.delete(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        jdbcTemplate.update("DELETE FROM prescription WHERE medical_record_id IN (SELECT id FROM medical_record WHERE patient_id = ?)", id);
        jdbcTemplate.update("DELETE FROM invoice WHERE appointment_id IN (SELECT id FROM appointment WHERE patient_id = ? OR doctor_id = ?)", id, id);
        jdbcTemplate.update("DELETE FROM medical_record WHERE patient_id = ? OR appointment_id IN (SELECT id FROM appointment WHERE doctor_id = ?)", id, id);
        jdbcTemplate.update("DELETE FROM appointment WHERE patient_id = ? OR doctor_id = ?", id, id);
        
        userRepository.deleteById(id);
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
