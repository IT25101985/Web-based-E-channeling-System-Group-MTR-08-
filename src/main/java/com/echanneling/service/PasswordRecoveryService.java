package com.echanneling.service;
import com.echanneling.entity.*;
import com.echanneling.repository.*;
import java.time.LocalDateTime;
@org.springframework.stereotype.Service
public class PasswordRecoveryService {
    private final UserRepository users;
    private final PasswordResetRepository resets;
    private final NotificationRepository notifications;
    private final UserService accounts;
    @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:false}") private boolean mailEnabled;
    @org.springframework.beans.factory.annotation.Value("${app.recovery.base-url:http://localhost:8080}") private String baseUrl;
    public PasswordRecoveryService(UserRepository users,PasswordResetRepository resets,NotificationRepository notifications,UserService accounts) {
        this.users=users;this.resets=resets;this.notifications=notifications;this.accounts=accounts;
    }
    static String hash(String token) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @org.springframework.transaction.annotation.Transactional
    public void request(String email) {
        if (!mailEnabled || email==null || email.length()>254) return;
        User candidate=users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if(candidate==null) return;
        User user=users.findLockedById(candidate.getId()).orElseThrow();
        if(!user.isActive()) return;
        LocalDateTime now=com.echanneling.patterns.ClinicClock.getInstance().now();
        PasswordReset reset=resets.findById(user.getId()).orElse(new PasswordReset());
        if(reset.getRequestedAt()!=null && reset.getRequestedAt().isAfter(now.minusMinutes(5))) return;
        byte[] bytes=new byte[32]; new java.security.SecureRandom().nextBytes(bytes);
        String token=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        reset.setUserId(user.getId());reset.setTokenHash(hash(token));reset.setRequestedAt(now);
        reset.setExpiresAt(now.plusMinutes(15));reset.setUsedAt(null);resets.save(reset);
        String link=baseUrl.replaceAll("/+$","")+"/reset-password?token="+token;
        Notification n=new Notification(user.getEmail(),"Use this link to choose a new password (expires in 15 minutes):\n"+link+
            "\nIf you did not request a reset, ignore this email.",Notification.NotificationType.SYSTEM,Notification.NotificationStatus.PENDING);
        n.setSubject("CareLink password reset"); notifications.save(n);
    }
    @org.springframework.transaction.annotation.Transactional
    public void reset(String token,String newPassword) {
        PasswordPolicy.requireValid(newPassword);
        if(token==null || !token.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Invalid or expired reset link.");
        PasswordReset candidate=resets.findByTokenHash(hash(token)).orElseThrow(()->new IllegalArgumentException("Invalid or expired reset link."));
        User user=users.findLockedById(candidate.getUserId()).orElseThrow();
        if (users.findByEmailIgnoreCase(user.getEmail()).filter(match -> match.getId().equals(user.getId())).isEmpty())
            throw new IllegalArgumentException("Invalid or expired reset link.");
        PasswordReset reset=resets.findById(user.getId()).orElseThrow();
        if(!user.isActive() || !hash(token).equals(reset.getTokenHash()) || reset.getUsedAt()!=null || !reset.getExpiresAt().isAfter(com.echanneling.patterns.ClinicClock.getInstance().now()))
            throw new IllegalArgumentException("Invalid or expired reset link.");
        accounts.saveWithNewPassword(user,newPassword);
        reset.setUsedAt(com.echanneling.patterns.ClinicClock.getInstance().now());
    }
}
