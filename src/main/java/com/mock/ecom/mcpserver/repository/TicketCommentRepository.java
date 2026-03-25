package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Ticket;
import com.mock.ecom.mcpserver.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {
    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
