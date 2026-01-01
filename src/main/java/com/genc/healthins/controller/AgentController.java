package com.genc.healthins.controller;

import com.genc.healthins.model.*;
import com.genc.healthins.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AgentController {

    private final PolicyService policyService;
    private final SupportService supportService;
    private final ClaimService claimService;
    private final NotificationService notificationService;
    private final UserService userService;

    public AgentController(PolicyService policyService, 
                           SupportService supportService, 
                           ClaimService claimService, 
                           NotificationService notificationService, 
                           UserService userService) {
        this.policyService = policyService;
        this.supportService = supportService;
        this.claimService = claimService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * 1. AGENT DASHBOARD
     * Strictly shows stats related to policies of customers assigned to this agent.
     */
    @GetMapping({"/agent/dashboard", "/agent/dashboard.html"})
    public String dashboard(Model model, HttpServletRequest request) {
        User agent = (User) request.getSession().getAttribute("loggedInUser");
        if (agent == null) return "redirect:/login";

        // Fetch data based on Agent -> Customer -> Policy hierarchy
        List<Policy> policies = policyService.findPoliciesByAssignedAgent(agent.getId());
        List<SupportTicket> tickets = supportService.findAll();

        // Count unique customers assigned to this agent
        Set<User> customers = policies.stream()
                .map(Policy::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Claim> pendingClaims = new ArrayList<>();
        int approvedToday = 0;
        int rejectedToday = 0;
        LocalDate today = LocalDate.now();

        for (Policy p : policies) {
            List<Claim> claims = claimService.findByPolicy(p);
            for (Claim c : claims) {
                String status = c.getClaimStatus();
                if ("PENDING".equalsIgnoreCase(status)) pendingClaims.add(c);
                if ("APPROVED".equalsIgnoreCase(status) && c.getClaimDate() != null && c.getClaimDate().toLocalDate().equals(today)) approvedToday++;
                if ("REJECTED".equalsIgnoreCase(status) && c.getClaimDate() != null && c.getClaimDate().toLocalDate().equals(today)) rejectedToday++;
            }
        }

        model.addAttribute("policies", policies);
        model.addAttribute("tickets", tickets);
        model.addAttribute("pendingClaims", pendingClaims);
        model.addAttribute("pendingClaimsCount", pendingClaims.size());
        model.addAttribute("approvedToday", approvedToday);
        model.addAttribute("rejectedToday", rejectedToday);
        model.addAttribute("customersCount", customers.size());

        return "agent/dashboard";
    }

    /**
     * 2. POLICY ASSISTANCE (Consolidated)
     * Shows all policies for customers assigned to this agent.
     */
@GetMapping({"/agent/policies", "/agent/policies.html"})
public String policies(Model model, HttpServletRequest request) {
    User currentAgent = (User) request.getSession().getAttribute("loggedInUser");
    if (currentAgent == null) return "redirect:/login";

    // Consistently use agent.getId()
    List<Policy> assistedPolicies = policyService.findPoliciesByAssignedAgent(currentAgent.getId());

    model.addAttribute("policies", assistedPolicies);
    model.addAttribute("activeCount", assistedPolicies.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getPolicyStatus())).count());
    model.addAttribute("expiredCount", assistedPolicies.stream().filter(p -> "EXPIRED".equalsIgnoreCase(p.getPolicyStatus())).count());
    model.addAttribute("inactiveCount", assistedPolicies.stream().filter(p -> "INACTIVE".equalsIgnoreCase(p.getPolicyStatus())).count());
    
    return "agent/policies";
}

    /**
     * 3. CUSTOMER ASSISTANCE ACTION
     * Triggers a notification from agent to customer.
     */
    @PostMapping("/agent/policies/assist")
    public String assistCustomer(@RequestParam Long policyId, @RequestParam String actionType, RedirectAttributes ra) {
        Optional<Policy> opt = policyService.findById(policyId);
        if (opt.isPresent()) {
            Policy policy = opt.get();
            User customer = policy.getUser();

            if (customer != null) {
                Notification n = new Notification();
                n.setUser(customer);
                n.setType(actionType);
                n.setMessage("Your Agent has initiated " + actionType.toLowerCase() + " assistance for Policy " + policy.getPolicyNumber() + ".");
                notificationService.save(n); 
                ra.addFlashAttribute("success", "Assistance alert sent to " + customer.getUsername());
            }
        }
        return "redirect:/agent/policies";
    }

    /**
     * 4. SEND REMINDER (AJAX)
     */
    @PostMapping("/agent/policies/send-reminder")
    @ResponseBody
    public ResponseEntity<?> sendReminder(@RequestParam Long policyId, @RequestParam String type) {
        Optional<Policy> opt = policyService.findById(policyId);
        if (opt.isEmpty()) return ResponseEntity.status(404).build();
        
        Policy p = opt.get();
        String customerName = (p.getUser() != null) ? p.getUser().getUsername() : "Customer";
        
        // Log simulation of external service (Email/SMS)
        System.out.println("Reminder sent to " + customerName + " for policy " + p.getPolicyNumber() + " Type: " + type);
        
        return ResponseEntity.ok(Map.of("message", type + " reminder sent successfully!"));
    }

    /**
     * 5. CLAIMS MANAGEMENT
     */
    @GetMapping({"/agent/claims", "/agent/claims.html"})
    public String claims(Model model, HttpServletRequest request) {
        User agent = (User) request.getSession().getAttribute("loggedInUser");
        if (agent == null) return "redirect:/login";

        List<Policy> policies = policyService.findPoliciesByAssignedAgent(agent.getId());
        List<Claim> allClaims = new ArrayList<>();
        for (Policy p : policies) {
            allClaims.addAll(claimService.findByPolicy(p));
        }
        
        model.addAttribute("claims", allClaims);
        model.addAttribute("statPending", (int) allClaims.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getClaimStatus())).count());
        model.addAttribute("statApproved", (int) allClaims.stream().filter(c -> "APPROVED".equalsIgnoreCase(c.getClaimStatus())).count());
        model.addAttribute("statRejected", (int) allClaims.stream().filter(c -> "REJECTED".equalsIgnoreCase(c.getClaimStatus())).count());
        return "agent/claims";
    }

    @PostMapping("/agent/claims/{id}/action")
    @ResponseBody
    public ResponseEntity<?> processClaim(@PathVariable Long id, @RequestParam String action) {
        Optional<Claim> opt = claimService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Claim not found"));
        
        Claim claim = opt.get();
        LocalDateTime now = LocalDateTime.now();
        
        if ("approve".equalsIgnoreCase(action)) {
            claim.setClaimStatus("APPROVED");
            claim.setClaimDate(now);
            claimService.save(claim);
            return ResponseEntity.ok(Map.of("status","approved"));
        } else if ("reject".equalsIgnoreCase(action)) {
            claim.setClaimStatus("REJECTED");
            claim.setClaimDate(now);
            claimService.save(claim);
            return ResponseEntity.ok(Map.of("status","rejected"));
        }
        return ResponseEntity.badRequest().body(Map.of("error","Unknown action"));
    }

    @DeleteMapping("/agent/claims/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteClaim(@PathVariable Long id) {
        claimService.deleteById(id);
        return ResponseEntity.ok(Map.of("status","deleted"));
    }

    /**
     * 6. CUSTOMER LIST
     * Strictly shows unique users assigned to this agent.
     */
    @GetMapping({"/agent/customers", "/agent/customers.html"})
    public String customers(Model model, HttpServletRequest request) {
        User agent = (User) request.getSession().getAttribute("loggedInUser");
        if (agent == null) return "redirect:/login";

        List<User> assignedUsers = userService.findAll().stream()
                .filter(u -> "USER".equalsIgnoreCase(u.getRole()) && 
        u.getAssignedAgentId() != null && 
        u.getAssignedAgentId().equals(agent.getId()))
                .collect(Collectors.toList());

        model.addAttribute("customers", assignedUsers);
        model.addAttribute("customersCount", assignedUsers.size());
        return "agent/customers";
    }

    @GetMapping({"/agent/customers/{id}", "/agent/customers/{id}.html"})
    public String customerDetails(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Customer not found");
            return "redirect:/agent/customers";
        }
        User user = opt.get();
        model.addAttribute("customer", user);
        model.addAttribute("policies", policyService.findByUser(user));
        return "agent/customer-details";
    }

    /**
     * 7. PROFILE
     */
    @GetMapping({"/agent/profile", "/agent/profile.html"})
    public String profile(HttpServletRequest request, Model model) {
        User user = (User) request.getSession().getAttribute("loggedInUser");
        model.addAttribute("agent", user);
        return "agent/profile";
    }
}