package com.echanneling.repository;
import com.echanneling.entity.AttendeeLock;
public interface AttendeeLockRepository extends org.springframework.data.jpa.repository.JpaRepository<AttendeeLock,String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from AttendeeLock l where l.id = :id")
    java.util.Optional<AttendeeLock> lock(@org.springframework.data.repository.query.Param("id") String id);
}
