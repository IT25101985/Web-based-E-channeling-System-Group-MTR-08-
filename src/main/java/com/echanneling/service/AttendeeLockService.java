package com.echanneling.service;
import com.echanneling.entity.AttendeeLock;
import com.echanneling.repository.AttendeeLockRepository;
import org.springframework.transaction.support.TransactionTemplate;
@org.springframework.stereotype.Service
public class AttendeeLockService {
    private final AttendeeLockRepository locks;
    private final TransactionTemplate create;
    public AttendeeLockService(AttendeeLockRepository locks, org.springframework.transaction.PlatformTransactionManager tx) {
        this.locks = locks; create = new TransactionTemplate(tx);
        create.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    // Creation commits separately; losing a concurrent insert must not poison the booking transaction.
    public void acquire(String identity) {
        String key;
        try { key = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        try { create.executeWithoutResult(s -> { if (!locks.existsById(key)) locks.saveAndFlush(new AttendeeLock(key)); }); }
        catch (org.springframework.dao.DataIntegrityViolationException concurrentInsert) { /* another instance created this key */ }
        locks.lock(key).orElseThrow();
    }
}
