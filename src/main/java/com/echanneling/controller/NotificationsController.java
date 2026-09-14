package com.echanneling.controller;

import com.echanneling.entity.Notification;
import com.echanneling.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/admin/notifications")
public class NotificationsController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String notifications(Model model) {
        model.addAttribute("notifications", notificationService.getAllNotifications());
        model.addAttribute("newNotification", new Notification());
        model.addAttribute("pageTitle", "Notification Management");
        return "admin/notifications";
    }

    @PostMapping("/add")
    public String createNotification(@RequestParam String recipient,
                                     @RequestParam String message,
                                     @RequestParam Notification.NotificationType type,
                                     @RequestParam(required = false) Notification.NotificationStatus status) {
        try {
            if (status == null) {
                status = Notification.NotificationStatus.PENDING;
            }
            Notification notification = new Notification(recipient, message, type, status);
            notificationService.saveNotification(notification);
            return "redirect:/admin/notifications?created";
        } catch (Exception e) {
            return "redirect:/admin/notifications?error";
        }
    }

    @PostMapping("/{id}/update")
    public String updateNotification(@PathVariable Long id,
                                     @RequestParam String recipient,
                                     @RequestParam String message,
                                     @RequestParam Notification.NotificationType type,
                                     @RequestParam Notification.NotificationStatus status) {
        try {
            Optional<Notification> opt = notificationService.getNotificationById(id);
            if (opt.isPresent()) {
                Notification n = opt.get();
                n.setRecipient(recipient);
                n.setMessage(message);
                n.setType(type);
                n.setStatus(status);
                notificationService.saveNotification(n);
            }
            return "redirect:/admin/notifications?updated";
        } catch (Exception e) {
            return "redirect:/admin/notifications?error";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Notification.NotificationStatus status) {
        notificationService.updateNotificationStatus(id, status);
        return "redirect:/admin/notifications?updated";
    }

    @PostMapping("/{id}/delete")
    public String deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return "redirect:/admin/notifications?deleted";
    }
}

