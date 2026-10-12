package com.echanneling.controller;

import com.echanneling.service.DddVivaService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/ddd-viva")
public class DddVivaController {
    private final DddVivaService service;
    public DddVivaController(DddVivaService service) { this.service = service; }
    @GetMapping
    public String page(@RequestParam(defaultValue="IT25101985") String student,
                       @RequestParam(defaultValue="1") int query, Model model) {
        try {
            var member = service.member(student);
            model.addAttribute("members", service.members());
            model.addAttribute("member", member);
            model.addAttribute("query", query);
            model.addAttribute("sql", service.sql(member, query));
            try {
                model.addAttribute("connection", service.connection());
                model.addAttribute("result", service.execute(member, query));
            } catch (DataAccessException ex) {
                model.addAttribute("connectionError", "Cannot load the assignment database. Check SQL Server and run viva files 01, 02 and 03 in SSMS, then refresh.");
            }
            return "admin/ddd-viva";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
    @GetMapping("/eer")
    public ResponseEntity<ClassPathResource> eer() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=EER_Original.pdf")
            .body(new ClassPathResource("viva/EER_Original.pdf"));
    }
    @GetMapping("/sql/{student}")
    public ResponseEntity<ClassPathResource> sql(@PathVariable String student) {
        try {
            var member = service.member(student);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=" + member.file())
                .body(new ClassPathResource("viva/" + member.file()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
