package com.echanneling.controller;

import com.echanneling.entity.Appointment;
import com.echanneling.entity.Doctor;
import com.echanneling.entity.User;
import com.echanneling.entity.Feedback;
import com.echanneling.service.AppointmentService;
import com.echanneling.service.DoctorService;
import com.echanneling.service.UserService;
import com.echanneling.service.MedicalRecordService;
import com.echanneling.service.FeedbackService;
import com.echanneling.repository.UserRepository;
import com.echanneling.service.HospitalBranchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.echanneling.entity.HospitalBranch;
import com.echanneling.entity.AvailabilitySlot;
import com.echanneling.repository.AvailabilitySlotRepository;
import com.echanneling.repository.HospitalBranchRepository;

@Controller
@org.springframework.transaction.annotation.Transactional
@RequestMapping("/admin")
public class AdminController {

    @Autowired private com.echanneling.service.InvoiceService invoiceService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserService userService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalBranchService hospitalBranchService;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private com.echanneling.service.UserFactory userFactory;

    private Appointment findAppointmentById(Long id) {
        return appointmentService.getAppointmentById(id).orElse(null);
    }

    @GetMapping("/export")
    public void exportData(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"users_export.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("ID,Username,Full Name,Role,Email,Phone");

        for (User u : userService.getAllUsers()) {
            String email = (u.getEmail() != null) ? u.getEmail() : "";
            String phone = (u.getPhoneNo() != null) ? u.getPhoneNo() : "";

            writer.printf("%d,%s,%s,%s,%s,%s\n",
                    u.getId(), com.echanneling.service.CsvExport.cell(u.getUsername()), com.echanneling.service.CsvExport.cell(u.getFullName()), com.echanneling.service.CsvExport.cell(u.getRole()), com.echanneling.service.CsvExport.cell(email), com.echanneling.service.CsvExport.cell(phone));
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long total=appointmentService.count();
        long cancelled=appointmentService.statusCount("CANCELLED");
        long completed=appointmentService.statusCount("COMPLETED");
        long scheduled=appointmentService.statusCount("SCHEDULED");
        long todayCount=appointmentService.todayCount();
        java.math.BigDecimal revenue=invoiceService.paidRevenue();
        Map<Integer,Long> hourlyStats=appointmentService.hourlyCounts();
        long patientsCount=userRepository.countByRole("ROLE_PATIENT");
        List<Doctor> allDoctors=doctorService.getAllDoctors();
        long doctorsCount=allDoctors.size();
        List<Appointment> recentAppointments=appointmentService.recent(5);

        model.addAttribute("totalAppointments", total);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("completedCount", completed);
        model.addAttribute("scheduledCount", scheduled);
        model.addAttribute("todayCount", todayCount);
        model.addAttribute("estimatedRevenue", revenue.setScale(2).toPlainString());
        model.addAttribute("patientsCount", patientsCount);
        model.addAttribute("doctorsCount", doctorsCount);
        model.addAttribute("hourlyStats", hourlyStats);
        model.addAttribute("recentAppointments", recentAppointments);
        model.addAttribute("allDoctors", allDoctors);

        return "admin/dashboard";
    }

    @GetMapping("/doctors")
    public String doctorManagement(Model model) {
        List<Doctor> allDoctors = doctorService.getAllDoctors();
        List<HospitalBranch> allBranches = hospitalBranchService.getAllBranches();
        model.addAttribute("doctors", allDoctors);
        model.addAttribute("branches", allBranches);
        model.addAttribute("newDoctor", new Doctor());
        model.addAttribute("newUser", new User());

        // Map doctor ID to assigned hospital branch names for badge display
        Map<Long, List<String>> doctorBranches = new java.util.HashMap<>();
        for (Doctor doc : allDoctors) {
            List<String> bNames = availabilitySlotRepository.findByDoctor(doc).stream()
                .filter(s -> s.getHospitalBranch() != null)
                .map(s -> s.getHospitalBranch().getBranchName())
                .distinct()
                .collect(Collectors.toList());
            doctorBranches.put(doc.getId(), bNames);
        }
        model.addAttribute("doctorBranches", doctorBranches);
        return "admin/doctor-management";
    }

    @PostMapping("/doctors/add")
    public String addDoctor(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String username,
                            @RequestParam String password,
                            @RequestParam(required = false) Long branchId,
                            @RequestParam(required = false) String phoneNo,
                            @RequestParam(required = false) String address,
                            @RequestParam(required = false) String specialization,
                            @RequestParam(required = false) String experience,
                            @RequestParam(required = false) String clinicHours,
                            @RequestParam(required = false) java.math.BigDecimal consultationFee,
                            @RequestParam(required = false) String profileImage) {
        try {
            if (!com.echanneling.service.PasswordPolicy.valid(password)) return "redirect:/admin/users?error=weak_password";
            if (username == null || username.trim().length() < 4) {
                return "redirect:/admin/doctors?error=short_username";
            }
            if (userService.findByUsername(username.trim()).isPresent()) {
                return "redirect:/admin/doctors?error=duplicate_username";
            }

            // Using Factory Pattern (UserFactory) to create User object for Doctor
            User userObj = userFactory.createUser("DOCTOR");
            userObj.setUsername(username.trim());
            userObj.setPassword(password);
            userObj.setFullName(fullName.trim());
            userObj.setEmail(email != null ? email.trim() : "");
            userObj.setPhoneNo(phoneNo != null && !phoneNo.trim().isEmpty() ? phoneNo.trim() : null);
            userObj.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
            if (profileImage != null && !profileImage.isEmpty()) {
                userObj.setProfileImage(profileImage);
            }

            User savedUser = userService.saveUser(userObj);

            Doctor doctor = new Doctor();
            doctor.setUser(savedUser);
            doctor.setName(savedUser.getFullName());
            doctor.setSpecialization(specialization != null && !specialization.trim().isEmpty() ? specialization.trim() : "General Physician");
            doctor.setExperience(experience != null && !experience.trim().isEmpty() ? experience.trim() : "1 Year");
            doctor.setClinicHours(clinicHours != null && !clinicHours.trim().isEmpty() ? clinicHours.trim() : "09:00 AM - 05:00 PM");
            doctor.setConsultationFee(consultationFee != null ? consultationFee : new java.math.BigDecimal("1000.00"));

            Doctor savedDoctor = doctorService.saveDoctor(doctor);

            // Automatically create active clinic sessions for the next 30 days if a branch is assigned
            if (branchId != null) {
                HospitalBranch branch = hospitalBranchRepository.findById(branchId).orElse(null);
                if (branch != null) {
                    seedDoctorSessionsForBranch(savedDoctor, branch);
                }
            }

            return "redirect:/admin/doctors?add_success";
        } catch (Exception e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.err.println("Doctor registration failed: " + e.getMessage());
            return "redirect:/admin/doctors?error";
        }
    }

    @PostMapping("/doctors/{id}/delete")
    public String deleteDoctor(@PathVariable Long id) {
        Optional<Doctor> d = doctorService.getDoctorById(id);

        if (d.isPresent()) {
            Doctor doc = d.get();
            User u = doc.getUser();

            // Clean up doctor record first
            if (u == null) doctorService.deleteDoctor(id);

            // Clean up user account and all cascading records
            if (u != null) {
                userService.deleteUser(u);
            }
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/{id}/update")
    public String updateDoctor(@PathVariable Long id,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String phoneNo,
                               @RequestParam(required = false) Long branchId,
                               @RequestParam(required = false) String specialization,
                               @RequestParam(required = false) String experience,
                               @RequestParam(required = false) String clinicHours,
                               @RequestParam(required = false) java.math.BigDecimal consultationFee,
                               @RequestParam(required = false) String profileImage) {
        try {
            Optional<Doctor> optDoc = doctorService.getDoctorById(id);
            if (optDoc.isPresent()) {
                Doctor doc = optDoc.get();

                if (name != null && !name.trim().isEmpty()) {
                    doc.setName(name.trim());
                }
                if (specialization != null && !specialization.trim().isEmpty()) {
                    doc.setSpecialization(specialization.trim());
                }
                if (experience != null && !experience.trim().isEmpty()) {
                    doc.setExperience(experience.trim());
                }
                if (clinicHours != null && !clinicHours.trim().isEmpty()) {
                    doc.setClinicHours(clinicHours.trim());
                }
                if (consultationFee != null) {
                    doc.setConsultationFee(consultationFee);
                }

                if (doc.getUser() != null) {
                    User u = doc.getUser();
                    if (phoneNo != null && !phoneNo.trim().isEmpty()) u.setPhoneNo(phoneNo.trim());
                    if (email != null && !email.trim().isEmpty()) u.setEmail(email.trim());
                    if (name != null && !name.trim().isEmpty()) u.setFullName(name.trim());
                    if (profileImage != null && !profileImage.isEmpty()) u.setProfileImage(profileImage);
                    userRepository.save(u);
                }
                doctorService.saveDoctor(doc);

                // If branchId is selected, ensure doctor has active clinic sessions at that branch
                if (branchId != null) {
                    HospitalBranch branch = hospitalBranchRepository.findById(branchId).orElse(null);
                    if (branch != null) {
                        seedDoctorSessionsForBranch(doc, branch);
                    }
                }
            }
            return "redirect:/admin/doctors?update_success";
        } catch (Exception e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "redirect:/admin/doctors?error";
        }
    }

    @Autowired private com.echanneling.service.ClinicSessionService clinicSessions;

    private void seedDoctorSessionsForBranch(Doctor doctor, HospitalBranch branch) {
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= 30; day++) {
            LocalDate date = today.plusDays(day);
            if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) continue;
            boolean exists = availabilitySlotRepository.findByDoctorAndSlotDateAndIsActive(doctor, date, true).stream()
                .anyMatch(s -> s.getHospitalBranch() != null && s.getHospitalBranch().getId().equals(branch.getId()));
            if (!exists) {
                var format = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
                String[] times = doctor.getClinicHours().toUpperCase(java.util.Locale.ROOT).split(" - ");
                clinicSessions.save(null, doctor.getId(), branch.getId(), date,
                    java.time.LocalTime.parse(times[0], format), java.time.LocalTime.parse(times[1], format));
            }
        }
    }

    @PostMapping("/doctors/{id}/reset-password")
    public String resetDoctorPassword(@PathVariable Long id, @RequestParam String newPassword) {
        Optional<Doctor> optDoc = doctorService.getDoctorById(id);
        if (optDoc.isPresent() && optDoc.get().getUser() != null) {
            User u = optDoc.get().getUser();
            userService.saveWithNewPassword(u, newPassword);
        }
        return "redirect:/admin/doctors?reset_success";
    }

    @GetMapping("/appointments")
    public String appointments(Model model, @RequestParam(defaultValue="0") int page) {
        var history=appointmentService.allHistory(page);
        model.addAttribute("appointments", history.getContent());
        model.addAttribute("clinicToday", com.echanneling.patterns.ClinicClock.getInstance().today());
        model.addAttribute("page",history.getNumber());model.addAttribute("hasNext",history.hasNext());
        model.addAttribute("doctors", doctorService.getAllDoctors());
        return "admin/appointments";
    }

    @PostMapping("/appointments/{id}/checkin")
    public String checkInAppointment(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            appointmentService.checkIn(id);
            flash.addFlashAttribute("successMessage", "Patient checked in. The assigned doctor can now write the prescription.");
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("errorMessage", error.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id) {
        // Reusing our custom OOP helper method to locate the appointment smoothly
        Appointment foundAppointment = findAppointmentById(id);

        if (foundAppointment != null && "SCHEDULED".equals(foundAppointment.getStatus())) {
            appointmentService.cancel(foundAppointment.getId());
        }
        return "redirect:/admin/appointments";
    }

    @GetMapping("/users")
    public String userManagement(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("newUser", new User());
        return "admin/users";
    }

    @PostMapping("/users/add")
    public String addUser(@RequestParam String fullName,
                          @RequestParam String username,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam(required = false) String phoneNo,
                          @RequestParam(required = false) String address,
                          @RequestParam(required = false, defaultValue = "ROLE_PATIENT") String role,
                          @RequestParam(required = false) String nic) {
        try {
            if (!com.echanneling.service.PasswordPolicy.valid(password)) return "redirect:/admin/users?error=weak_password";
            if (username == null || username.trim().length() < 4) {
                return "redirect:/admin/users?error=short_username";
            }
            if (userService.findByUsername(username.trim()).isPresent()) {
                return "redirect:/admin/users?error=duplicate_username";
            }

            if (!java.util.Set.of("ROLE_PATIENT", "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_RECEPTIONIST").contains(role)) return "redirect:/admin/users?error=invalid_role";
            String targetRole = role;
            User newUser = userFactory.createUser(targetRole);
            if (newUser instanceof com.echanneling.entity.Patient patient)
                patient.setNic(nic != null ? nic.trim() : null);
            if (newUser instanceof com.echanneling.entity.Admin admin)
                admin.setDepartment("System Administration");

            newUser.setFullName(fullName.trim());
            newUser.setUsername(username.trim());
            newUser.setEmail(email != null ? email.trim() : "");
            newUser.setPassword(password);
            newUser.setPhoneNo(phoneNo != null && !phoneNo.trim().isEmpty() ? phoneNo.trim() : null);
            newUser.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
            newUser.setRole(targetRole);

            User savedUser = userService.saveUser(newUser);

            if ("ROLE_DOCTOR".equalsIgnoreCase(targetRole)) {
                Doctor doctor = new Doctor();
                doctor.setUser(savedUser);
                doctor.setName(savedUser.getFullName());
                doctor.setSpecialization("General Physician");
                doctor.setExperience("1 Year");
                doctor.setClinicHours("09:00 AM - 05:00 PM");
                doctor.setConsultationFee(1000.0);
                doctorService.saveDoctor(doctor);
            }

            return "redirect:/admin/users?user_added";
        } catch (Exception e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.err.println("Failed to add user: " + e.getMessage());
            return "redirect:/admin/users?error";
        }
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam(required = false) String phoneNo,
                             @RequestParam(required = false) String address,
                             @RequestParam(required = false) String role) {
        try {
            Optional<User> opt = userService.getUserById(id);
            if (opt.isPresent()) {
                User user = opt.get();
                if (fullName != null && !fullName.trim().isEmpty()) user.setFullName(fullName.trim());
                if (email != null && !email.trim().isEmpty()) user.setEmail(email.trim());
                if (phoneNo != null) user.setPhoneNo(phoneNo.trim());
                if (address != null) user.setAddress(address.trim());
                if (role != null && !role.trim().isEmpty()) {
                    String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                    if (!formattedRole.equals(user.getRole())) return "redirect:/admin/users?error=role_change_requires_new_account";
                }
                userRepository.save(user);

                Optional<Doctor> optDoc = doctorService.getDoctorByUser(user);
                if (optDoc.isPresent()) {
                    Doctor doc = optDoc.get();
                    doc.setName(user.getFullName());
                    doctorService.saveDoctor(doc);
                }
            }
            return "redirect:/admin/users?user_updated";
        } catch (Exception e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "redirect:/admin/users?error";
        }
    }

    // ============================================================
    // REVIEW / FEEDBACK MANAGEMENT (Day 3 - Feedback Module)
    // ============================================================

    @GetMapping("/reviews")
    public String reviewManagement(Model model) {
        model.addAttribute("feedbacks", feedbackService.getAllFeedback());
        return "admin/review-management";
    }

    @PostMapping("/reviews/{id}/approve")
    public String approveFeedback(@PathVariable Long id) {
        feedbackService.approveFeedback(id);
        return "redirect:/admin/reviews?approved";
    }

    @PostMapping("/reviews/{id}/reject")
    public String rejectFeedback(@PathVariable Long id) {
        feedbackService.rejectFeedback(id);
        return "redirect:/admin/reviews?rejected";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return "redirect:/admin/reviews?deleted";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, java.security.Principal principal) {
        if (userService.getUserById(id).map(u -> u.getUsername().equals(principal.getName()) || "ROLE_ADMIN".equals(u.getRole())).orElse(false)) return "redirect:/admin/users?error=protected_admin";
        Optional<User> optUser = userService.getUserById(id);
        if (optUser.isPresent()) {
            User user = optUser.get();

            userService.deleteUser(user);
        }
        return "redirect:/admin/users";
    }
    @PostMapping("/users/{id}/active")
    public String setAccountActive(@PathVariable Long id, @RequestParam boolean active) {
        userService.setAccountActive(id, active);
        return "redirect:/admin/users?updated";
    }
    @PostMapping("/users/{id}/reset-password")
    public String resetUserPassword(@PathVariable Long id, @RequestParam String newPassword) {
        if (!com.echanneling.service.PasswordPolicy.valid(newPassword)) return "redirect:/admin/users?error=weak_password";
        User user = userService.getUserById(id).orElseThrow();
        userService.saveWithNewPassword(user, newPassword);
        return "redirect:/admin/users?reset_success";
    }
}
