package com.echanneling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class NotificationsController {

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("pageTitle", "Notification Management");
        return "admin/notifications";
    }
}
