package com.genc.healthins.controller;

import com.genc.healthins.model.SupportResponse;
import com.genc.healthins.model.SupportTicket;
import com.genc.healthins.model.User;
import com.genc.healthins.service.SupportResponseService;
import com.genc.healthins.service.SupportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class SupportController {

    private final SupportService ticketService;
    private final SupportResponseService responseService;

    public SupportController(SupportService ticketService, SupportResponseService responseService) {
        this.ticketService = ticketService;
        this.responseService = responseService;
    }

    @GetMapping({"/user/support","/user/support.html"})
    public String userSupport(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("loggedInUser") : null;
        model.addAttribute("tickets", user != null ? ticketService.findByUser(user) : java.util.Collections.emptyList());
        return "user/support";
    }

    @PostMapping("/user/support/create")
    public String createTicket(@RequestParam String issue_description, HttpServletRequest request, RedirectAttributes ra) {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("loggedInUser") : null;
        if (user == null) {
            ra.addFlashAttribute("error", "Please login to create a ticket");
            return "redirect:/login";
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setIssueDescription(issue_description);
        ticket.setCreatedDate(LocalDateTime.now());
        ticket.setTicketStatus("OPEN");
        ticket.setUser(user);
        ticketService.save(ticket);
        ra.addFlashAttribute("success", "Ticket created successfully");
        return "redirect:/user/support";
    }

    @GetMapping({"/admin/support","/admin/support.html"})
    public String adminSupport(Model model) {
        model.addAttribute("tickets", ticketService.findAll());
        return "admin/support";
    }

    @GetMapping({"/support/view/{id}", "/support/view/{id}.html"})
    public String viewTicket(@PathVariable Long id, Model model) {
        var opt = ticketService.findById(id);
        if (opt.isEmpty()) return "not-found";
        SupportTicket ticket = opt.get();
        model.addAttribute("ticket", ticket);
        model.addAttribute("responses", responseService.findByTicket(ticket));
        return "support/view"; // templates/support/view.html
    }

    @PostMapping("/support/{id}/response")
    public String addResponse(@PathVariable Long id, @RequestParam String message, HttpServletRequest request, RedirectAttributes ra) {
        var opt = ticketService.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error","Ticket not found"); return "redirect:/admin/support"; }
        SupportTicket ticket = opt.get();
        HttpSession session = request.getSession(false);
        User responder = session != null ? (User) session.getAttribute("loggedInUser") : null;
        if (responder == null) { ra.addFlashAttribute("error","Please login to respond"); return "redirect:/login"; }

        SupportResponse r = new SupportResponse();
        r.setSupportTicket(ticket);
        r.setResponder(responder);
        r.setMessage(message);
        r.setCreatedAt(LocalDateTime.now());
        responseService.save(r);

        // Update ticket status when admin/agent responds
        if ("ADMIN".equals(responder.getRole()) || "AGENT".equals(responder.getRole())) {
            ticket.setTicketStatus("RESPONDED");
            ticketService.save(ticket);
        }

        ra.addFlashAttribute("success","Response added");
        return "redirect:/support/view/" + id;
    }
}
