package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByIdAndActiveTrueAndExpiresAtAfter(UUID id, LocalDateTime now);
    List<Session> findByCustomerAndActiveTrue(Customer customer);
}
