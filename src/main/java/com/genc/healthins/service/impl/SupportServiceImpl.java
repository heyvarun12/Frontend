package com.genc.healthins.service.impl;

import com.genc.healthins.model.SupportTicket;
import com.genc.healthins.model.User;
import com.genc.healthins.repository.SupportTicketRepository;
import com.genc.healthins.service.SupportService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupportServiceImpl implements SupportService {

    private final SupportTicketRepository ticketRepository;

    public SupportServiceImpl(SupportTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public List<SupportTicket> findByUser(User user) { return ticketRepository.findByUser(user); }

    @Override
    public List<SupportTicket> findAll() { return ticketRepository.findAll(); }

    @Override
    public SupportTicket save(SupportTicket ticket) { return ticketRepository.save(ticket); }

    @Override
    public Optional<SupportTicket> findById(Long id) { return ticketRepository.findById(id); }

    @Override
    public void delete(SupportTicket ticket) { ticketRepository.delete(ticket); }
}
