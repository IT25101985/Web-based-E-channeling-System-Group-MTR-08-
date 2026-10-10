package com.echanneling.config;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired private AvailabilitySlotRepository availabilitySlots;
    @Autowired private org.springframework.core.env.Environment environment;

    @Override
    public void run(String... args) throws Exception {

        // 1. Seed Admin
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            com.echanneling.entity.Admin admin = new com.echanneling.entity.Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setRole("ROLE_ADMIN");
            admin.setEmail("admin@healthcareplus.com");
            admin.setPhoneNo("+94112345678");
            admin.setAddress("HealthcarePlus HQ, Colombo");
            admin.setDepartment("System Administration");

            userRepository.save(admin);
            System.out.println("Default Admin created successfully!");
        }

        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("demo", "test"))
                && userRepository.findByUsername("reception").isEmpty()) {
            User reception = new User("reception", passwordEncoder.encode("Reception@123"), "Reception Team");
            reception.setRole("ROLE_RECEPTIONIST"); reception.setEmail("reception@example.com");
            reception.setAddress("CareLink front desk"); reception.setPhoneNo("0771234567");
            userRepository.save(reception);
        }

        // 2. Seed Hospital Branches
        if (hospitalBranchRepository.count() == 0) {
            createBranch("Colombo Central Hospital", "Colombo 03", "120 Park Road, Colombo 03", "+94 11 234 5678");
            createBranch("Kandy General Hospital Branch", "Kandy", "45 Peradeniya Road, Kandy", "+94 81 223 3445");
            createBranch("Galle Regional Center", "Galle", "12 Matara Road, Galle", "+94 91 334 4556");
            createBranch("Jaffna Healthcare Unit", "Jaffna", "78 Hospital Road, Jaffna", "+94 21 445 5667");
            System.out.println("Hospital branches initialized successfully!");
        }

        // 3. Seed Doctors
        if (doctorRepository.count() == 0) {
            createSampleDoctor("drsmith", "doctor123", "Dr. John Smith", "Cardiology", "10 Years", "09:00 AM - 05:00 PM", 1500.0, "john.smith@echanneling.com", "0771112233", "https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&w=500&q=80");
            createSampleDoctor("drperera", "doctor123", "Dr. Sarah Perera", "General Physician", "7 Years", "09:00 AM - 05:00 PM", 1200.0, "sarah.perera@echanneling.com", "0772223344", "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=500&q=80");
            createSampleDoctor("drshan", "doctor123", "Dr. K. Shanmugam", "Pediatrics", "12 Years", "09:00 AM - 05:00 PM", 1800.0, "k.shanmugam@echanneling.com", "0773334455", "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?auto=format&fit=crop&w=500&q=80");
            createSampleDoctor("drfernando", "doctor123", "Dr. A. Fernando", "Neurology", "15 Years", "09:00 AM - 05:00 PM", 2000.0, "a.fernando@echanneling.com", "0774445566", "https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&w=500&q=80");
            System.out.println("Sample doctors initialized successfully!");
        }

        // Seed clinic sessions for all doctors across all branches so the booking page works out of the box.
        if (availabilitySlots.count() == 0) {
            var demoBranches=hospitalBranchRepository.findAll();
            var demoDoctors=doctorRepository.findAll();
            for (int i=0;i<demoDoctors.size() && !demoBranches.isEmpty();i++) {
                for (int day=1;day<=30;day++) {
                    var date=java.time.LocalDate.now().plusDays(day);
                    if(date.getDayOfWeek()==java.time.DayOfWeek.SUNDAY) continue;
                    AvailabilitySlot session=new AvailabilitySlot();session.setDoctor(demoDoctors.get(i));
                    session.setHospitalBranch(demoBranches.get(i % demoBranches.size()));session.setSlotDate(date);
                    session.setStartTime(java.time.LocalTime.of(9,0));session.setEndTime(java.time.LocalTime.of(17,0));
                    session.setMaxPatients(8);session.setIsActive(true);availabilitySlots.save(session);
                }
            }
            System.out.println("Clinic sessions seeded for all doctors and branches.");
        }

        // 4. Seed Patients
        Patient p1 = createSamplePatient("dhanu", "patient123", "Dhanu Dhanukshan", "199812345678", "dhanu@gmail.com", "0777123456", "Colombo 07", "O+", "120/80", "72 bpm", "0771122334");
        Patient p2 = createSamplePatient("pavi", "patient123", "Pavithiran Kumar", "200012345678", "pavi@gmail.com", "0778234567", "Kandy", "B+", "118/75", "70 bpm", "0772233445");
        Patient p3 = createSamplePatient("kamal", "patient123", "Kamal Perera", "199512345678", "kamal@gmail.com", "0779345678", "Galle", "A+", "125/82", "75 bpm", "0773344556");

        // 5. Seed Appointments
        if (appointmentRepository.count() == 0) {
            Doctor doc1 = doctorRepository.findAll().stream().filter(d -> "Dr. John Smith".equals(d.getName())).findFirst().orElse(null);
            Doctor doc2 = doctorRepository.findAll().stream().filter(d -> "Dr. Sarah Perera".equals(d.getName())).findFirst().orElse(null);
            Doctor doc3 = doctorRepository.findAll().stream().filter(d -> "Dr. K. Shanmugam".equals(d.getName())).findFirst().orElse(null);
            Doctor doc4 = doctorRepository.findAll().stream().filter(d -> "Dr. A. Fernando".equals(d.getName())).findFirst().orElse(null);

            LocalDateTime now = LocalDateTime.now();

            if (doc1 != null && p1 != null) {
                // Scheduled appointment today
                Appointment a1 = new Appointment();
                a1.setDoctor(doc1);
                a1.setPatient(p1);
                a1.setDoctorName(doc1.getName());
                a1.setPatientName(p1.getFullName());
                a1.setContactPhone(p1.getPhoneNo());
                a1.setContactEmail(p1.getEmail());
                a1.setAppointmentDate(now.plusHours(2));
                a1.setStatus("SCHEDULED");
                appointmentRepository.save(a1);

                // Completed appointment in the past
                Appointment a2 = new Appointment();
                a2.setDoctor(doc1);
                a2.setPatient(p1);
                a2.setDoctorName(doc1.getName());
                a2.setPatientName(p1.getFullName());
                a2.setContactPhone(p1.getPhoneNo());
                a2.setContactEmail(p1.getEmail());
                a2.setAppointmentDate(now.minusDays(5).withHour(10).withMinute(0));
                a2.setStatus("COMPLETED");
                a2 = appointmentRepository.save(a2);

                // Invoice for completed
                Invoice inv1 = new Invoice();
                inv1.setAppointment(a2);
                inv1.setAmount(doc1.getConsultationFee() != null ? doc1.getConsultationFee() : 1500.0);
                inv1.setStatus("PAID");
                invoiceRepository.save(inv1);

                // Medical record & prescription
                MedicalRecord mr1 = new MedicalRecord();
                mr1.setPatient(p1);
                mr1.setAppointment(a2);
                mr1.setDiagnosis("Mild hypertension. Advised low sodium diet and regular cardio.");
                mr1.setRecordDate(now.minusDays(5));
                medicalRecordRepository.save(mr1);

                Prescription pr1 = new Prescription();
                pr1.setMedicalRecord(mr1);
                pr1.setMedication("Amlodipine 5mg");
                pr1.setDosage("1 tablet once daily in morning");
                pr1.setDuration("30 Days");
                pr1.setPrice(450.0);
                prescriptionRepository.save(pr1);

                // Feedback
                Feedback fb1 = new Feedback();
                fb1.setDoctor(doc1);
                fb1.setPatient(p1);
                fb1.setRating(5);
                fb1.setComments("Dr. Smith is exceptionally attentive and explained the treatment plan clearly.");
                fb1.setModerationStatus("APPROVED");
                feedbackRepository.save(fb1);
            }

            if (doc2 != null && p2 != null) {
                // Scheduled appointment
                Appointment a3 = new Appointment();
                a3.setDoctor(doc2);
                a3.setPatient(p2);
                a3.setDoctorName(doc2.getName());
                a3.setPatientName(p2.getFullName());
                a3.setContactPhone(p2.getPhoneNo());
                a3.setContactEmail(p2.getEmail());
                a3.setAppointmentDate(now.plusDays(1).withHour(11).withMinute(0));
                a3.setStatus("SCHEDULED");
                appointmentRepository.save(a3);

                // Completed appointment
                Appointment a4 = new Appointment();
                a4.setDoctor(doc2);
                a4.setPatient(p2);
                a4.setDoctorName(doc2.getName());
                a4.setPatientName(p2.getFullName());
                a4.setContactPhone(p2.getPhoneNo());
                a4.setContactEmail(p2.getEmail());
                a4.setAppointmentDate(now.minusDays(2).withHour(14).withMinute(0));
                a4.setStatus("COMPLETED");
                a4 = appointmentRepository.save(a4);

                Invoice inv2 = new Invoice();
                inv2.setAppointment(a4);
                inv2.setAmount(doc2.getConsultationFee() != null ? doc2.getConsultationFee() : 1200.0);
                inv2.setStatus("PAID");
                invoiceRepository.save(inv2);

                MedicalRecord mr2 = new MedicalRecord();
                mr2.setPatient(p2);
                mr2.setAppointment(a4);
                mr2.setDiagnosis("Viral Flu & Upper Respiratory Tract Infection");
                mr2.setRecordDate(now.minusDays(2));
                medicalRecordRepository.save(mr2);

                Prescription pr2 = new Prescription();
                pr2.setMedicalRecord(mr2);
                pr2.setMedication("Paracetamol 500mg & Cetirizine 10mg");
                pr2.setDosage("1 tablet three times daily after meals");
                pr2.setDuration("5 Days");
                pr2.setPrice(320.0);
                prescriptionRepository.save(pr2);

                Feedback fb2 = new Feedback();
                fb2.setDoctor(doc2);
                fb2.setPatient(p2);
                fb2.setRating(5);
                fb2.setComments("Very friendly doctor and fast consultation!");
                fb2.setModerationStatus("APPROVED");
                feedbackRepository.save(fb2);
            }

            if (doc3 != null && p3 != null) {
                Appointment a5 = new Appointment();
                a5.setDoctor(doc3);
                a5.setPatient(p3);
                a5.setDoctorName(doc3.getName());
                a5.setPatientName(p3.getFullName());
                a5.setContactPhone(p3.getPhoneNo());
                a5.setContactEmail(p3.getEmail());
                a5.setAppointmentDate(now.plusDays(2).withHour(9).withMinute(0));
                a5.setStatus("SCHEDULED");
                appointmentRepository.save(a5);

                Feedback fb3 = new Feedback();
                fb3.setDoctor(doc3);
                fb3.setPatient(p3);
                fb3.setRating(4);
                fb3.setComments("Great pediatric care for my child.");
                fb3.setModerationStatus("APPROVED");
                feedbackRepository.save(fb3);
            }

            if (doc4 != null && p1 != null) {
                Appointment a6 = new Appointment();
                a6.setDoctor(doc4);
                a6.setPatient(p1);
                a6.setDoctorName(doc4.getName());
                a6.setPatientName(p1.getFullName());
                a6.setContactPhone(p1.getPhoneNo());
                a6.setContactEmail(p1.getEmail());
                a6.setAppointmentDate(now.minusDays(10).withHour(15).withMinute(0));
                a6.setStatus("CANCELLED");
                appointmentRepository.save(a6);
            }

            System.out.println("Sample appointments, invoices, medical records, and feedbacks initialized successfully!");
        }

        // 6. Seed Notifications
        if (notificationRepository.count() == 0) {
            Notification n1 = new Notification("all-patients@echanneling.com", "Free cardiac health screening camp this Saturday at Colombo Central Branch.", Notification.NotificationType.GENERAL, Notification.NotificationStatus.SENT);
            Notification n2 = new Notification("dhanu@gmail.com", "Reminder: Your appointment with Dr. John Smith is scheduled today.", Notification.NotificationType.REMINDER, Notification.NotificationStatus.SENT);
            Notification n3 = new Notification("admin@healthcareplus.com", "System backup completed successfully.", Notification.NotificationType.SYSTEM, Notification.NotificationStatus.SENT);
            notificationRepository.save(n1);
            notificationRepository.save(n2);
            notificationRepository.save(n3);
            System.out.println("Sample notifications initialized successfully!");
        }
    }

    private void createBranch(String name, String area, String address, String contact) {
        HospitalBranch branch = new HospitalBranch();
        branch.setBranchName(name);
        branch.setArea(area);
        branch.setAddress(address);
        branch.setContactNumber(contact);
        hospitalBranchRepository.save(branch);
    }

    private Patient createSamplePatient(String username, String password, String fullName, String nic, String email, String phone, String address, String bloodGroup, String bp, String hr, String emergencyContact) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            if (existing.get() instanceof Patient) {
                return (Patient) existing.get();
            }
            return patientRepository.findById(existing.get().getId()).orElse(null);
        }

        Patient patient = new Patient();
        patient.setUsername(username);
        patient.setPassword(passwordEncoder.encode(password));
        patient.setFullName(fullName);
        patient.setNic(nic);
        patient.setEmail(email);
        patient.setPhoneNo(phone);
        patient.setAddress(address);
        patient.setBloodGroup(bloodGroup);
        patient.setBloodPressure(bp);
        patient.setHeartRate(hr);
        patient.setEmergencyContact(emergencyContact);
        patient.setRole("ROLE_PATIENT");
        return patientRepository.save(patient);
    }

    private void createSampleDoctor(String username, String password, String fullName, String spec, String exp, String hours, Double fee, String email, String phone, String profileImage) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User docUser = new User();
        docUser.setUsername(username);
        docUser.setPassword(passwordEncoder.encode(password));
        docUser.setFullName(fullName);
        docUser.setRole("ROLE_DOCTOR");
        docUser.setEmail(email);
        docUser.setPhoneNo(phone);
        docUser.setAddress("HealthcarePlus Main Hospital");
        docUser.setProfileImage(profileImage);
        docUser = userRepository.save(docUser);

        Doctor doctor = new Doctor();
        doctor.setUser(docUser);
        doctor.setName(fullName);
        doctor.setSpecialization(spec);
        doctor.setExperience(exp);
        doctor.setClinicHours(hours);
        doctor.setConsultationFee(fee);
        doctorRepository.save(doctor);
    }
}
