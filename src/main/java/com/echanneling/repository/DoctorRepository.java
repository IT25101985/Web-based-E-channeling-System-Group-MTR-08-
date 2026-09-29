package com.echanneling.repository;

import com.echanneling.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from Doctor d where d.id = :id")
    java.util.Optional<Doctor> findForBooking(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<Doctor> findBySpecialization(String specialization);
    java.util.Optional<Doctor> findByUser(com.echanneling.entity.User user);
}
