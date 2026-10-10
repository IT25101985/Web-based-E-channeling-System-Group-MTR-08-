package com.echanneling.controller;
import com.echanneling.repository.*;
import com.echanneling.service.ClinicSessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;

@Controller
@RequestMapping("/admin/sessions")
public class ClinicSessionController {
    private final ClinicSessionService sessions;
    private final DoctorRepository doctors;
    private final HospitalBranchRepository branches;
    public ClinicSessionController(ClinicSessionService sessions,DoctorRepository doctors,HospitalBranchRepository branches) {
        this.sessions=sessions; this.doctors=doctors; this.branches=branches;
    }
    @GetMapping public String list(Model model) {
        model.addAttribute("sessions",sessions.all()); model.addAttribute("doctors",doctors.findAll());
        model.addAttribute("branches",branches.findAll()); return "admin/sessions";
    }
    @PostMapping("/save") public String save(@RequestParam(required=false) Long id,@RequestParam Long doctorId,
            @RequestParam Long branchId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime end,RedirectAttributes flash) {
        try { sessions.save(id,doctorId,branchId,date,start,end); flash.addFlashAttribute("successMessage","Clinic session saved."); }
        catch(IllegalArgumentException e) { flash.addFlashAttribute("errorMessage",e.getMessage()); }
        return "redirect:/admin/sessions";
    }
    @PostMapping("/{id}/delete") public String delete(@PathVariable Long id,RedirectAttributes flash) {
        try { sessions.delete(id); flash.addFlashAttribute("successMessage","Clinic session removed."); }
        catch(IllegalArgumentException e) { flash.addFlashAttribute("errorMessage",e.getMessage()); }
        return "redirect:/admin/sessions";
    }
}
