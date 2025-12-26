package com.genc.healthins.controller;

import com.genc.healthins.service.PolicyService;
import com.genc.healthins.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// import java.util.List;

@Controller
public class AdminController {

    private final UserService userService;
    private final PolicyService policyService;
    private final com.genc.healthins.service.ClaimService claimService;
    private final com.genc.healthins.service.SupportService supportService;

    public AdminController(UserService userService, PolicyService policyService, com.genc.healthins.service.ClaimService claimService, com.genc.healthins.service.SupportService supportService) {
        this.userService = userService;
        this.policyService = policyService;
        this.claimService = claimService;
        this.supportService = supportService;
    }

    @GetMapping({"/admin/dashboard", "/admin/dashboard.html"})
    public String dashboard(Model model) {
        var users = userService.findAll();
        var policies = policyService.findAll();

        // Aggregate claims across policies
        java.util.List<com.genc.healthins.model.Claim> allClaims = new java.util.ArrayList<>();
        for (var p : policies) {
            allClaims.addAll(claimService.findByPolicy(p));
        }

        // compute KPI counts
        long activePolicies = policies.stream().filter(p -> "Active".equalsIgnoreCase(p.getPolicyStatus())).count();
        long openClaims = allClaims.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getClaimStatus())).count();
        long tickets = supportService.findAll().size();

        // recent claims (sorted by date desc, take 3)
        java.util.List<com.genc.healthins.model.Claim> recentClaims = allClaims.stream()
                .sorted(java.util.Comparator.comparing((com.genc.healthins.model.Claim c) -> c.getClaimDate(),
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed())
                .limit(3)
                .toList();

        model.addAttribute("policies", policies);
        model.addAttribute("usersCount", users.size());
        model.addAttribute("users", users);
        model.addAttribute("activePolicies", activePolicies);
        model.addAttribute("openClaims", openClaims);
        model.addAttribute("ticketsCount", tickets);
        model.addAttribute("recentClaims", recentClaims);

        // build a small list of recent users with policy counts
        var recentUsers = users.stream().limit(5).map(u -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("policyCount", policyService.findByUser(u).size());
            return m;
        }).toList();
        model.addAttribute("recentUsers", recentUsers);

        return "admin/dashboard"; // templates/admin/dashboard.html
    }

    @GetMapping({"/admin/users", "/admin/users.html"})
    public String users(Model model) {
        var users = userService.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping({"/admin/policies", "/admin/policies.html"})
    public String policies(Model model) {
        model.addAttribute("policies", policyService.findAll());
        model.addAttribute("users", userService.findAll());
        return "admin/policies";
    }

    @PostMapping({"/admin/policies", "/admin/policies.html"})
    public String createPolicy(@org.springframework.web.bind.annotation.RequestParam String policyName,
                               @org.springframework.web.bind.annotation.RequestParam(required = false) String holderEmail,
                               @org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal premium,
                               @org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal coverage,
                               @org.springframework.web.bind.annotation.RequestParam(required = false) String startDate,
                               RedirectAttributes ra) {
        try {
            com.genc.healthins.model.Policy p = new com.genc.healthins.model.Policy();
            p.setPolicyNumber("PLCY-" + System.currentTimeMillis());
            p.setCoverageType(policyName);
            if (coverage != null) p.setCoverageAmount(coverage);
            if (premium != null) p.setPremiumAmount(premium);
            if (startDate != null && !startDate.isBlank()) {
                java.time.LocalDate ld = java.time.LocalDate.parse(startDate);
                p.setStartDate(ld.atStartOfDay());
            }
            p.setPolicyStatus("Active");
            if (holderEmail != null && !holderEmail.isBlank()) {
                userService.findByEmail(holderEmail).ifPresent(p::setUser);
            }
            policyService.save(p);
            ra.addFlashAttribute("success", "Policy created");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Could not create policy: " + ex.getMessage());
        }
        return "redirect:/admin/policies";
    }

    @PostMapping({"/admin/policies/delete/{id}", "/admin/policies/delete/{id}.html"})
    public String deletePolicy(@PathVariable Long id, RedirectAttributes ra) {
        try {
            policyService.deleteById(id);
            ra.addFlashAttribute("success", "Policy deleted");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Could not delete policy: " + ex.getMessage());
        }
        return "redirect:/admin/policies";
    }

    @GetMapping({"/admin/policies/{id}", "/admin/policies/{id}.html"})
    public String policyDetails(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var opt = policyService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Policy not found");
            return "redirect:/admin/policies";
        }
        model.addAttribute("policy", opt.get());
        return "admin/policy-details";
    }

    @PostMapping({"/admin/users/delete/{id}", "/admin/users/delete/{id}.html"})
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteById(id);
            ra.addFlashAttribute("success", "User deleted");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Could not delete user: " + ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping({"/admin/users/{id}", "/admin/users/{id}.html"})
    public String userDetails(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        var user = opt.get();
        model.addAttribute("user", user);
        model.addAttribute("policies", policyService.findByUser(user));
        return "admin/user-details";
    }
}
