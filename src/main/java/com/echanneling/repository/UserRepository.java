package com.echanneling.repository;

import com.echanneling.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findAllByEmailIgnoreCase(String email);
    /** A shared legacy address must never select an arbitrary account. */
    default Optional<User> findByEmailIgnoreCase(String email) {
        List<User> matches = findAllByEmailIgnoreCase(email);
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<User> findByRole(String role);
    long countByRole(String role);
}
