package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Checkout;
import com.mock.ecom.mcpserver.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByCheckout(Checkout checkout);
    Optional<Payment> findByTransactionId(String transactionId);
}
