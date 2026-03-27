package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Page<Ticket> findByCustomerOrderByCreatedAtDesc(Customer customer, Pageable pageable);
}
