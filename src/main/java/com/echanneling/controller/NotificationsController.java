package com.echanneling.controller;
import com.echanneling.entity.Notification;
import com.echanneling.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/notifications")
public class NotificationsController {
    private final NotificationService service;
    private final NotificationDeliveryService delivery;
    public NotificationsController(NotificationService service,NotificationDeliveryService delivery) {this.service=service;this.delivery=delivery;}
    @GetMapping public String list(Model model) {
        model.addAttribute("notifications",service.getAllNotifications());
        model.addAttribute("mailEnabled",delivery.isEnabled());
        return "admin/notifications";
    }
    @PostMapping({"/add","/{id}/update"}) public String save(@PathVariable(required=false) Long id,
            @RequestParam String recipient,@RequestParam String subject,@RequestParam String message,
            @RequestParam Notification.NotificationType type,RedirectAttributes flash) {
        try {service.saveManual(id,recipient,subject,message,type);flash.addFlashAttribute("successMessage","Message queued for delivery.");}
        catch(IllegalArgumentException e){flash.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/admin/notifications";
    }
    @PostMapping("/{id}/retry") public String retry(@PathVariable Long id,RedirectAttributes flash) {
        try {service.retry(id);flash.addFlashAttribute("successMessage","Message queued for retry.");}
        catch(IllegalArgumentException e){flash.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/admin/notifications";
    }
    @PostMapping("/{id}/delete") public String delete(@PathVariable Long id) {
        service.deleteNotification(id);return "redirect:/admin/notifications?deleted";
    }
    @GetMapping("/templates") public String templates(Model model) {
        model.addAttribute("templates",service.allTemplates()); return "admin/notification-templates";
    }
    @PostMapping("/templates/{type}") public String template(@PathVariable Notification.NotificationType type,
            @RequestParam String subject,@RequestParam String body,RedirectAttributes flash) {
        try {service.saveTemplate(type,subject,body);flash.addFlashAttribute("successMessage","Template saved. New notifications will use these words.");}
        catch(IllegalArgumentException e){flash.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/admin/notifications/templates";
    }
    @PostMapping("/templates/{type}/reset") public String reset(@PathVariable Notification.NotificationType type) {
        service.resetTemplate(type);return "redirect:/admin/notifications/templates?updated";
    }
}
