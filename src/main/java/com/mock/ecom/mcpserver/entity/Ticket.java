package com.mock.ecom.mcpserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket")
public class Ticket {

    public enum TicketType {
        DELIVERY_ISSUE, RETURN_REQUEST, PAYMENT_ISSUE, PRODUCT_QUALITY, CANCELLATION, OTHER
    }

    public enum TicketPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum TicketStatus {
        OPEN, IN_PROGRESS, AWAITING_CUSTOMER, RESOLVED, CLOSED
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<TicketComment> comments;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
