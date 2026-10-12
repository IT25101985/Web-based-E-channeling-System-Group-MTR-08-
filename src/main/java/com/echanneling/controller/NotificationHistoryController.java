package com.echanneling.controller;
import com.echanneling.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
@Controller
public class NotificationHistoryController {
    private final NotificationService notifications;
    public NotificationHistoryController(NotificationService notifications) {this.notifications=notifications;}
    @GetMapping({"/patient/notifications","/doctor/notifications"})
    public String history(Principal principal,Model model,HttpServletRequest request) {
        model.addAttribute("notifications",notifications.history(principal.getName()));
        model.addAttribute("portal",request.getServletPath().startsWith("/doctor/") ? "doctor" : "patient");
        return "notifications";
    }
    @PostMapping({"/patient/notifications/{id}/read","/doctor/notifications/{id}/read"})
    public String markRead(@PathVariable Long id,Principal principal,HttpServletRequest request) {
        notifications.markRead(id,principal.getName());
        return "redirect:"+(request.getServletPath().startsWith("/doctor/") ? "/doctor/notifications" : "/patient/notifications");
    }
}
