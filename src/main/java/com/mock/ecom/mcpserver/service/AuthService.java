package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.config.AppProperties;
import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.entity.Session;
import com.mock.ecom.mcpserver.repository.CustomerRepository;
import com.mock.ecom.mcpserver.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final CustomerRepository customerRepository;
    private final SessionRepository sessionRepository;
    private final AppProperties appProperties;

    @Transactional
    public Session serverToServerLogin(String phoneNumber, String platform, String secret) {
        if (!appProperties.getMock().getPlatformSecret().equals(secret)) {
            throw new IllegalArgumentException("Invalid platform secret");
        }
        Customer customer = customerRepository.findByPhoneNumber(phoneNumber)
            .orElseGet(() -> customerRepository.save(Customer.builder()
                .phoneNumber(phoneNumber)
                .name(generateName(phoneNumber))
                .build()));
        Session session = Session.builder()
            .customer(customer)
            .phoneNumber(phoneNumber)
            .platform(platform)
            .active(true)
            .expiresAt(LocalDateTime.now().plusHours(appProperties.getSession().getTtlHours()))
            .build();
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Session validateSession(String sessionId) {
        UUID sid;
        try { sid = UUID.fromString(sessionId); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid session ID format"); }
        return sessionRepository.findByIdAndActiveTrueAndExpiresAtAfter(sid, LocalDateTime.now())
            .orElseThrow(() -> new IllegalArgumentException("Session not found or expired. Please login again."));
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Customer getCustomerFromSession(String sessionId) {
        return validateSession(sessionId).getCustomer();
    }

    private String generateName(String phone) {
        String[] names = {"Rahul","Priya","Amit","Neha","Vijay","Sunita","Ravi","Anjali","Suresh","Kavita"};
        return names[Math.abs(phone.hashCode()) % names.length] + " " +
               new String[]{"Sharma","Patel","Singh","Kumar","Gupta","Yadav","Mehta","Shah","Joshi","Nair"}[Math.abs(phone.hashCode() / 7) % 10];
    }
}
