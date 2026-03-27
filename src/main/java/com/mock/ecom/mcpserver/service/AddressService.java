package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.Address;
import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final AuthService authService;

    @Transactional
    public List<Address> getAddresses(String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        List<Address> existing = addressRepository.findByCustomer(customer);
        if (existing.isEmpty()) {
            return generateMockAddresses(customer);
        }
        return existing;
    }

    @Transactional
    public List<Address> generateMockAddresses(Customer customer) {
        long seed = Math.abs(customer.getPhoneNumber().hashCode());
        String[][] cities = {
            {"Mumbai","Maharashtra","400001"},{"Delhi","Delhi","110001"},
            {"Bangalore","Karnataka","560001"},{"Chennai","Tamil Nadu","600001"},
            {"Kolkata","West Bengal","700001"},{"Hyderabad","Telangana","500001"},
            {"Pune","Maharashtra","411001"},{"Ahmedabad","Gujarat","380001"}
        };
        String[] city1 = cities[(int)(seed % cities.length)];
        String[] city2 = cities[(int)((seed + 3) % cities.length)];
        Address home = addressRepository.save(Address.builder()
            .customer(customer)
            .recipientName(customer.getName() != null ? customer.getName() : "Customer")
            .phoneNumber(customer.getPhoneNumber())
            .addressLine1((10 + seed % 990) + ", " + new String[]{"Park Street","MG Road","Brigade Road","Linking Road","Anna Salai"}[(int)(seed % 5)])
            .city(city1[0]).state(city1[1]).pincode(city1[2])
            .type(Address.AddressType.HOME).isDefault(true).build());
        Address work = addressRepository.save(Address.builder()
            .customer(customer)
            .recipientName(customer.getName() != null ? customer.getName() : "Customer")
            .phoneNumber(customer.getPhoneNumber())
            .addressLine1("Office No " + (seed % 99 + 1) + ", " + new String[]{"Tech Park","Business Hub","Corporate Tower","Cyber City"}[(int)((seed + 1) % 4)])
            .city(city2[0]).state(city2[1]).pincode(city2[2])
            .type(Address.AddressType.WORK).isDefault(false).build());
        return List.of(home, work);
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Address getAddress(UUID addressId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return addressRepository.findById(addressId)
            .filter(a -> a.getCustomer().getId().equals(customer.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Address not found"));
    }
}
