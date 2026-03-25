package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthService authService;

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Page<Ticket> getTickets(String sessionId, int page, int pageSize) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return ticketRepository.findByCustomerOrderByCreatedAtDesc(customer, PageRequest.of(page, pageSize));
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Ticket getTicketDetails(String ticketId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return ticketRepository.findById(UUID.fromString(ticketId))
            .filter(t -> t.getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    @Transactional
    public TicketComment addComment(String ticketId, String comment, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Ticket ticket = ticketRepository.findById(UUID.fromString(ticketId))
            .filter(t -> t.getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        TicketComment userComment = ticketCommentRepository.save(TicketComment.builder()
            .ticket(ticket).authorType(TicketComment.AuthorType.CUSTOMER).content(comment).build());
        ticketCommentRepository.save(TicketComment.builder()
            .ticket(ticket).authorType(TicketComment.AuthorType.SUPPORT_AGENT)
            .content(generateAutoResponse(ticket.getType())).build());
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
        return userComment;
    }

    @Transactional
    public Ticket createTicket(String subject, String description, String type, String orderId, String orderItemId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Ticket.TicketType ticketType;
        try { ticketType = Ticket.TicketType.valueOf(type.toUpperCase()); }
        catch (Exception e) { ticketType = Ticket.TicketType.OTHER; }
        Order order = (orderId != null && !orderId.isBlank()) ? orderRepository.findById(UUID.fromString(orderId)).orElse(null) : null;
        OrderItem orderItem = (orderItemId != null && !orderItemId.isBlank()) ? orderItemRepository.findById(UUID.fromString(orderItemId)).orElse(null) : null;
        Ticket ticket = ticketRepository.save(Ticket.builder()
            .customer(customer).type(ticketType)
            .priority(assignPriority(ticketType))
            .status(Ticket.TicketStatus.OPEN)
            .subject(subject).description(description)
            .order(order).orderItem(orderItem).build());
        ticketCommentRepository.save(TicketComment.builder()
            .ticket(ticket).authorType(TicketComment.AuthorType.SYSTEM)
            .content("Ticket created. Our team will respond within 24 hours. Ticket ID: " + ticket.getId()).build());
        return ticket;
    }

    private Ticket.TicketPriority assignPriority(Ticket.TicketType type) {
        return switch (type) {
            case PAYMENT_ISSUE -> Ticket.TicketPriority.HIGH;
            case DELIVERY_ISSUE, RETURN_REQUEST -> Ticket.TicketPriority.MEDIUM;
            default -> Ticket.TicketPriority.LOW;
        };
    }

    private String generateAutoResponse(Ticket.TicketType type) {
        return switch (type) {
            case DELIVERY_ISSUE -> "Thank you for reaching out. We have escalated your delivery issue to our logistics team. You will receive an update within 24 hours.";
            case RETURN_REQUEST -> "We have received your return request. Our team will schedule a pickup within 2-3 business days.";
            case PAYMENT_ISSUE -> "We understand your concern regarding the payment. Our finance team is investigating and will resolve within 24-48 hours.";
            case PRODUCT_QUALITY -> "We apologize for the quality issue. Please share photos if possible. We will arrange a replacement or refund.";
            default -> "Thank you for contacting us. Our support team is reviewing your query and will respond shortly.";
        };
    }
}
