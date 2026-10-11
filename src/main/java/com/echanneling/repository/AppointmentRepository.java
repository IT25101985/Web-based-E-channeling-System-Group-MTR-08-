package com.echanneling.repository;

import com.echanneling.entity.Appointment;
import com.echanneling.entity.Doctor;
import com.echanneling.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.id = :id")
    java.util.Optional<Appointment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Appointment> findByAppointmentDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Appointment> findByStatusAndAppointmentDateBetween(String status, java.time.LocalDateTime start, java.time.LocalDateTime end);
    boolean existsByHospitalBranchId(Long branchId);
    List<Appointment> findByPatient(User patient);
    @org.springframework.data.jpa.repository.Query("""
        select a from Appointment a where a.status <> 'CANCELLED'
        and a.appointmentDate > :start and a.appointmentDate < :end
        and ((:nic is not null and (upper(a.recipientNic) = :nic or (a.recipientNic is null and (a.bookingFor is null or a.bookingFor = 'self') and upper(treat(a.patient as Patient).nic) = :nic)))
          or (a.patient.id = :owner and (a.bookingFor is null or a.bookingFor = 'self') and :self = true))
        """)
    List<Appointment> attendeeConflicts(@org.springframework.data.repository.query.Param("nic") String nic,
        @org.springframework.data.repository.query.Param("owner") Long owner,
        @org.springframework.data.repository.query.Param("self") boolean self,
        @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.patient = :patient and (a.hiddenByPatient is null or a.hiddenByPatient = false) order by a.appointmentDate desc")
    org.springframework.data.domain.Page<Appointment> findByPatientAndHiddenByPatientFalseOrderByAppointmentDateDesc(User patient, org.springframework.data.domain.Pageable page);
    org.springframework.data.domain.Page<Appointment> findByDoctorOrderByAppointmentDateDesc(Doctor doctor, org.springframework.data.domain.Pageable page);
    org.springframework.data.domain.Page<Appointment> findByDoctorUserUsernameOrderByAppointmentDateDesc(String username, org.springframework.data.domain.Pageable page);
    java.util.Optional<Appointment> findFirstByDoctorAndStatusOrderByAppointmentDateAsc(Doctor doctor, String status);
    long countByPatientAndStatus(User patient, String status);
    long countByStatus(String status);
    long countByDoctorAndStatus(Doctor doctor,String status);
    long countByDoctorAndAppointmentDateBetween(Doctor doctor,java.time.LocalDateTime start,java.time.LocalDateTime end);
    @org.springframework.data.jpa.repository.Query("select hour(a.appointmentDate), count(a) from Appointment a group by hour(a.appointmentDate)")
    List<Object[]> hourlyCounts();
    long countByAppointmentDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByDoctorOrderByIdDesc(Doctor doctor);
    List<Appointment> findByDoctorAndAppointmentDateBetween(Doctor doctor, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
