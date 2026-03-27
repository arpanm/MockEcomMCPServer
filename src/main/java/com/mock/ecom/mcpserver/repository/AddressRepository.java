package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Address;
import com.mock.ecom.mcpserver.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByCustomer(Customer customer);
    Optional<Address> findFirstByCustomerAndIsDefaultTrue(Customer customer);
}
