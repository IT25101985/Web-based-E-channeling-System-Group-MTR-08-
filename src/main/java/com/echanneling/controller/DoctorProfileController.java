package com.echanneling.controller;

import com.echanneling.entity.*;
import com.echanneling.service.*;
import com.echanneling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/doctor")
public class DoctorProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    // 1. Doctor Dashboard - Profile Image & Reviews
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("username", username);
            
            Optional<User> optUser = userService.findByUsername(username);
            if (optUser.isPresent()) {
                User user = optUser.get();
                model.addAttribute("profileImage", user.getProfileImage());
                
                // Provide defaults to prevent Thymeleaf 500 errors
                model.addAttribute("reviews", java.util.Collections.emptyList());
                model.addAttribute("averageRating", "0.0");
                model.addAttribute("totalReviews", 0);

                // Fetch Doctor details
                Optional<Doctor> optDoctor = doctorService.getDoctorByUser(user);
                if (optDoctor.isPresent()) {
                    Doctor doctor = optDoctor.get();
                    model.addAttribute("doctor", doctor);
                    
                    // Fetch Reviews
                    List<Feedback> reviews = feedbackService.getFeedbackByDoctor(doctor).stream().filter(f -> "APPROVED".equals(f.getModerationStatus())).toList();
                    model.addAttribute("reviews", reviews);
                    
                    // Calculate Average Rating
                    double avg = 0.0;
                    if (!reviews.isEmpty()) {
                        avg = reviews.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
                    }
                    model.addAttribute("averageRating", String.format("%.1f", avg));
                    model.addAttribute("totalReviews", reviews.size());

                    // Fetch Recent Appointments (Top 5)
                    List<Appointment> allDoctorAppointments = appointmentService.getAppointmentsForDoctor(doctor);
                    List<Appointment> recentAppointments = appointmentService.getRecentAppointmentsForDoctor(doctor)
                            .stream().limit(5).collect(java.util.stream.Collectors.toList());
                    model.addAttribute("recentAppointments", recentAppointments);

                    java.time.LocalDate today = java.time.LocalDate.now();
                    long todayCount = allDoctorAppointments.stream()
                            .filter(a -> a.getAppointmentDate() != null && a.getAppointmentDate().toLocalDate().equals(today))
                            .count();
                    long pendingCount = allDoctorAppointments.stream()
                            .filter(a -> "SCHEDULED".equalsIgnoreCase(a.getStatus()))
                            .count();

                    model.addAttribute("todayPatientsCount", todayCount);
                    model.addAttribute("pendingReportsCount", pendingCount);
                }
            }
        }
        return "doctor/dashboard";
    }

    @PostMapping("/update-picture")
    public String updatePicture(@RequestParam("imageUrl") String imageUrl, Principal principal) {
        if (principal != null) {
            Optional<User> optUser = userService.findByUsername(principal.getName());
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setProfileImage(imageUrl);
                userRepository.save(user); // Directly save without re-encoding password
            }
        }
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/update-fee")
    public String updateFee(@RequestParam("fee") Double fee, Principal principal) {
        if (principal != null) {
            Optional<User> optUser = userService.findByUsername(principal.getName());
            if (optUser.isPresent()) {
                Optional<Doctor> optDoctor = doctorService.getDoctorByUser(optUser.get());
                if (optDoctor.isPresent()) {
                    Doctor doctor = optDoctor.get();
                    doctor.setConsultationFee(fee);
                    doctorService.saveDoctor(doctor);
                }
            }
        }
        return "redirect:/doctor/dashboard?fee_updated";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("specialization") String specialization,
                                @RequestParam("experience") String experience,
                                @RequestParam("clinicHours") String clinicHours,
                                Principal principal) {
        if (principal != null) {
            Optional<User> optUser = userService.findByUsername(principal.getName());
            if (optUser.isPresent()) {
                Optional<Doctor> optDoctor = doctorService.getDoctorByUser(optUser.get());
                if (optDoctor.isPresent()) {
                    Doctor doctor = optDoctor.get();
                    doctor.setSpecialization(specialization);
                    doctor.setExperience(experience);
                    doctor.setClinicHours(clinicHours);
                    doctorService.saveDoctor(doctor);
                }
            }
        }
        return "redirect:/doctor/dashboard?profile_updated";
    }
}