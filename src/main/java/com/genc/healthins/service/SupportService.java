package com.genc.healthins.service;

import com.genc.healthins.model.SupportTicket;
import com.genc.healthins.model.User;

import java.util.List;
import java.util.Optional;

public interface SupportService {
    List<SupportTicket> findByUser(User user);
    List<SupportTicket> findAll();
    SupportTicket save(SupportTicket ticket);
    Optional<SupportTicket> findById(Long id);
    void delete(SupportTicket ticket);
}
