package com.genc.healthins.controller;

import com.genc.healthins.model.*;
import com.genc.healthins.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    private final UserService userService;
    private final PolicyService policyService;
    private final ClaimService claimService;
    private final SupportService supportService;

    public AdminController(UserService userService, PolicyService policyService, 
                           ClaimService claimService, SupportService supportService) {
        this.userService = userService;
        this.policyService = policyService;
        this.claimService = claimService;
        this.supportService = supportService;
    }

    // --- DASHBOARD ---
    @GetMapping({"/admin/dashboard", "/admin/dashboard.html"})
    public String dashboard(Model model) {
        var users = userService.findAll();
        var policies = policyService.findAll();
        List<Claim> allClaims = claimService.findAll();

        model.addAttribute("usersCount", users.size());
        model.addAttribute("activePolicies", policies.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getPolicyStatus())).count());
        model.addAttribute("openClaims", allClaims.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getClaimStatus())).count());
        model.addAttribute("ticketsCount", supportService.findAll().size());

        // Recent users logic
        var recentUsers = users.stream().limit(5).map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("policyCount", policyService.findByUser(u).size());
            return m;
        }).collect(Collectors.toList());
        model.addAttribute("recentUsers", recentUsers);

        return "admin/dashboard";
    }

    // --- USER MANAGEMENT ---
    @GetMapping({"/admin/users", "/admin/users.html"})
    public String listUsers(Model model) {
        List<User> allUsers = userService.findAll();
        // Fetch all agents for the dropdown in the UI
        List<User> agents = allUsers.stream()
                .filter(u -> "AGENT".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());

        model.addAttribute("users", allUsers);
        model.addAttribute("agents", agents);
        return "admin/users";
    }

    @PostMapping("/admin/users/assign-agent")
public String assignAgentToUser(@RequestParam int userId, @RequestParam Integer agentId, RedirectAttributes ra) {
    // Convert int userId to Long if your service strictly uses Long, 
    // or change service to use int
    userService.findById((long)userId).ifPresent(user -> {
        user.setAssignedAgentId(agentId); 
        userService.save(user);
        ra.addFlashAttribute("success", "Agent assigned successfully!");
    });
    return "redirect:/admin/users";
}

    @PostMapping("/admin/users/add")
    public String addUser(@ModelAttribute User user, RedirectAttributes ra) {
        user.setJoinDate(java.time.LocalDateTime.now());
        userService.save(user);
        ra.addFlashAttribute("success", "New account created!");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/edit")
    public String editUser(@RequestParam Long id, @RequestParam String username, 
                           @RequestParam String phone, @RequestParam String role, RedirectAttributes ra) {
        userService.findById(id).ifPresent(u -> {
            u.setUsername(username);
            u.setPhone(phone);
            u.setRole(role);
            userService.save(u);
        });
        ra.addFlashAttribute("success", "User updated!");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.deleteById(id);
        ra.addFlashAttribute("success", "User deleted");
        return "redirect:/admin/users";
    }

    @GetMapping({"/admin/users/{id}", "/admin/users/{id}.html"})
    public String userDetails(@PathVariable Long id, Model model) {
        userService.findById(id).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("policies", policyService.findByUser(u));
        });
        return "admin/user-details";
    }

    // --- POLICY MANAGEMENT (Master Templates) ---
    @GetMapping({"/admin/policies", "/admin/policies.html"})
    public String adminPolicies(Model model) {
        // Only fetch policies where user is NULL (Master Plans)
        List<Policy> templatesOnly = policyService.findAll().stream()
                .filter(p -> p.getUser() == null)
                .collect(Collectors.toList());
        model.addAttribute("policies", templatesOnly);
        return "admin/policies";
    }

    @PostMapping("/admin/policies")
    public String createPolicy(@RequestParam String policyName, @RequestParam java.math.BigDecimal premium,
                               @RequestParam java.math.BigDecimal coverage, @RequestParam String startDate,
                               @RequestParam String endDate, RedirectAttributes ra) {
        Policy p = new Policy();
        p.setPolicyNumber("POL-" + System.currentTimeMillis());
        p.setCoverageType(policyName);
        p.setCoverageAmount(coverage);
        p.setPremiumAmount(premium);
        p.setPolicyStatus("ACTIVE");
        p.setStartDate(java.time.LocalDate.parse(startDate).atStartOfDay());
        p.setEndDate(java.time.LocalDate.parse(endDate).atStartOfDay());
        policyService.save(p);
        ra.addFlashAttribute("success", "New Plan published!");
        return "redirect:/admin/policies";
    }

    @PostMapping("/admin/policies/edit")
    public String editPolicy(@RequestParam Long id, @RequestParam String policyName,
                             @RequestParam java.math.BigDecimal premium, @RequestParam java.math.BigDecimal coverage,
                             @RequestParam String startDate, @RequestParam String endDate, RedirectAttributes ra) {
        policyService.findById(id).ifPresent(p -> {
            p.setCoverageType(policyName);
            p.setPremiumAmount(premium);
            p.setCoverageAmount(coverage);
            p.setStartDate(java.time.LocalDate.parse(startDate).atStartOfDay());
            p.setEndDate(java.time.LocalDate.parse(endDate).atStartOfDay());
            policyService.save(p);
        });
        ra.addFlashAttribute("success", "Plan updated!");
        return "redirect:/admin/policies";
    }

    @PostMapping("/admin/policies/delete/{id}")
    public String deletePolicy(@PathVariable Long id, RedirectAttributes ra) {
        policyService.deleteById(id);
        ra.addFlashAttribute("success", "Plan removed from marketplace");
        return "redirect:/admin/policies";
    }

    // --- CLAIM PROCESSING ---
@GetMapping({"/admin/claims", "/admin/claims.html"})
public String claims(Model model) {
    List<Claim> allClaims = claimService.findAll();
    
    // Filter out any claims that have 'broken' policy links before sending to UI
    List<Claim> validClaims = allClaims.stream()
            .filter(c -> {
                try {
                    return c.getPolicy() != null && c.getPolicy().getPolicyNumber() != null;
                } catch (Exception e) {
                    return false; // Skips the claim if policy is missing from DB
                }
            })
            .collect(Collectors.toList());

    model.addAttribute("claims", validClaims);
    
    // KPI logic remains the same
    model.addAttribute("pendingCount", validClaims.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getClaimStatus())).count());
    model.addAttribute("approvedCount", validClaims.stream().filter(c -> "APPROVED".equalsIgnoreCase(c.getClaimStatus())).count());
    model.addAttribute("rejectedCount", validClaims.stream().filter(c -> "REJECTED".equalsIgnoreCase(c.getClaimStatus())).count());
    
    return "admin/claims";
}

    @PostMapping("/admin/claims/process")
    public String processClaim(@RequestParam Long claimId, @RequestParam String status, 
                               @RequestParam(required = false) java.math.BigDecimal approvedAmount, RedirectAttributes ra) {
        claimService.findById(claimId).ifPresent(c -> {
            c.setClaimStatus(status);
            if (approvedAmount != null) c.setClaimAmount(approvedAmount);
            claimService.save(c);
        });
        ra.addFlashAttribute("success", "Claim " + status);
        return "redirect:/admin/claims";
    }
}