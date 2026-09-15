package com.echanneling.controller;

import com.echanneling.entity.HospitalBranch;
import com.echanneling.service.HospitalBranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/admin/branches")
public class HospitalBranchController {

    @Autowired
    private HospitalBranchService hospitalBranchService;

    @GetMapping
    public String listBranches(Model model) {
        model.addAttribute("branches", hospitalBranchService.getAllBranches());
        model.addAttribute("newBranch", new HospitalBranch());
        return "admin/hospital-management";
    }

    @PostMapping("/add")
    public String addBranch(@RequestParam String branchName,
                            @RequestParam String area,
                            @RequestParam String address,
                            @RequestParam(required = false) String contactNumber) {
        try {
            if (branchName == null || branchName.trim().isEmpty() ||
                area == null || area.trim().isEmpty() ||
                address == null || address.trim().isEmpty()) {
                return "redirect:/admin/branches?error=missing_fields";
            }

            HospitalBranch branch = new HospitalBranch();
            branch.setBranchName(branchName.trim());
            branch.setArea(area.trim());
            branch.setAddress(address.trim());
            branch.setContactNumber(contactNumber != null ? contactNumber.trim() : "");

            hospitalBranchService.save(branch);
            return "redirect:/admin/branches?success";
        } catch (Exception e) {
            System.err.println("Failed to save branch: " + e.getMessage());
            return "redirect:/admin/branches?error";
        }
    }

    @PostMapping("/{id}/update")
    public String updateBranch(@PathVariable Long id,
                               @RequestParam String branchName,
                               @RequestParam String area,
                               @RequestParam String address,
                               @RequestParam(required = false) String contactNumber) {
        try {
            if (branchName == null || branchName.trim().isEmpty() ||
                area == null || area.trim().isEmpty() ||
                address == null || address.trim().isEmpty()) {
                return "redirect:/admin/branches?error=missing_fields";
            }

            Optional<HospitalBranch> opt = hospitalBranchService.getBranchById(id);
            if (opt.isPresent()) {
                HospitalBranch branch = opt.get();
                branch.setBranchName(branchName.trim());
                branch.setArea(area.trim());
                branch.setAddress(address.trim());
                branch.setContactNumber(contactNumber != null ? contactNumber.trim() : "");
                hospitalBranchService.save(branch);
            }
            return "redirect:/admin/branches?updated";
        } catch (Exception e) {
            System.err.println("Failed to update branch: " + e.getMessage());
            return "redirect:/admin/branches?error";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteBranch(@PathVariable Long id) {
        try {
            hospitalBranchService.deleteBranch(id);
            return "redirect:/admin/branches?deleted";
        } catch (Exception e) {
            System.err.println("Failed to delete branch: " + e.getMessage());
            return "redirect:/admin/branches?error";
        }
    }
}

