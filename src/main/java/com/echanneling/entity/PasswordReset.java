package com.echanneling.entity;
import java.time.LocalDateTime;
@jakarta.persistence.Entity
public class PasswordReset {
    @jakarta.persistence.Id private Long userId;
    @jakarta.persistence.Column(length=64, unique=true) private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime requestedAt;
    private LocalDateTime usedAt;
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId=v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { tokenHash=v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { expiresAt=v; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime v) { requestedAt=v; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime v) { usedAt=v; }
}
