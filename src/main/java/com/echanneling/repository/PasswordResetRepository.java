package com.echanneling.repository;
import com.echanneling.entity.PasswordReset;
public interface PasswordResetRepository extends org.springframework.data.jpa.repository.JpaRepository<PasswordReset,Long> {
    java.util.Optional<PasswordReset> findByTokenHash(String hash);
}
