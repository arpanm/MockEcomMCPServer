package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Ticket;
import com.mock.ecom.mcpserver.entity.TicketComment;
import com.mock.ecom.mcpserver.repository.TicketCommentRepository;
import com.mock.ecom.mcpserver.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketTools {

    private final TicketService ticketService;
    private final TicketCommentRepository ticketCommentRepository;
    private final ToolResponseHelper helper;

    @Tool(description = "Get paginated list of all customer support tickets for the authenticated user. Returns ticket list with ID, subject, type, priority, status, and dates. Ticket types: DELIVERY_ISSUE, RETURN_REQUEST, PAYMENT_ISSUE, PRODUCT_QUALITY, CANCELLATION, OTHER. Requires valid sessionId.")
    public String getTickets(String sessionId, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] getTickets session={}", sessionId);
            int p = page != null ? page : 0;
            int s = pageSize != null ? pageSize : 10;
            Page<Ticket> tickets = ticketService.getTickets(sessionId, p, s);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("page", p);
            result.put("totalElements", tickets.getTotalElements());
            result.put("tickets", tickets.getContent().stream().map(this::ticketToMap).toList());
            return helper.toJson(result);
        } catch (Exception e) {
            log.error("[Tool] getTickets error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get complete details of a specific support ticket including full description, all comments and responses from support agents, current status, and resolution. Provide ticketId (from getTickets) and sessionId.")
    public String getTicketDetails(String ticketId, String sessionId) {
        try {
            log.info("[Tool] getTicketDetails ticketId={} session={}", ticketId, sessionId);
            Ticket ticket = ticketService.getTicketDetails(ticketId, sessionId);
            List<TicketComment> comments = ticketCommentRepository.findByTicketOrderByCreatedAtAsc(ticket);
            Map<String, Object> m = ticketToMap(ticket);
            m.put("description", ticket.getDescription());
            m.put("resolution", ticket.getResolution());
            m.put("comments", comments.stream().map(c -> {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("commentId", c.getId().toString());
                cm.put("authorType", c.getAuthorType().name());
                cm.put("content", c.getContent());
                cm.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                return cm;
            }).toList());
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] getTicketDetails error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Add a comment or reply to an existing support ticket. The comment appears as a customer message. An auto-response from the support agent will also be generated. Requires ticketId (from getTickets), comment text, and sessionId.")
    public String addTicketComment(String ticketId, String comment, String sessionId) {
        try {
            log.info("[Tool] addTicketComment ticketId={} session={}", ticketId, sessionId);
            TicketComment result = ticketService.addComment(ticketId, comment, sessionId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("commentId", result.getId().toString());
            m.put("content", result.getContent());
            m.put("authorType", result.getAuthorType().name());
            m.put("createdAt", result.getCreatedAt() != null ? result.getCreatedAt().toString() : null);
            m.put("message", "Comment added. Support team has been notified.");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] addTicketComment error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Create a new customer support ticket. Provide subject, description, ticket type (DELIVERY_ISSUE/RETURN_REQUEST/PAYMENT_ISSUE/PRODUCT_QUALITY/CANCELLATION/OTHER), and optionally orderId or orderItemId for context. Priority is auto-assigned based on type. Requires valid sessionId. Returns created ticket with ID and initial support response.")
    public String createTicket(String subject, String description, String type, String orderId, String orderItemId, String sessionId) {
        try {
            log.info("[Tool] createTicket type={} session={}", type, sessionId);
            Ticket ticket = ticketService.createTicket(subject, description, type, orderId, orderItemId, sessionId);
            Map<String, Object> m = ticketToMap(ticket);
            m.put("description", ticket.getDescription());
            m.put("message", "Ticket created successfully. Our team will respond within 24 hours.");
            return helper.toJson(m);
        } catch (Exception e) {
            log.error("[Tool] createTicket error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    private Map<String, Object> ticketToMap(Ticket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ticketId", t.getId().toString());
        m.put("subject", t.getSubject());
        m.put("type", t.getType() != null ? t.getType().name() : null);
        m.put("priority", t.getPriority() != null ? t.getPriority().name() : null);
        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        m.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
        if (t.getOrder() != null) m.put("orderId", t.getOrder().getId().toString());
        return m;
    }
}
