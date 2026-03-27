package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.*;
import com.mock.ecom.mcpserver.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final AuthService authService;
    private final ProductService productService;

    @Transactional
    public WishlistItem addToWishlist(String productId, String sessionId) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        Product product = productService.getProductById(UUID.fromString(productId));
        return wishlistItemRepository.findByCustomerAndProduct(customer, product)
            .orElseGet(() -> wishlistItemRepository.save(WishlistItem.builder()
                .customer(customer).product(product).addedAt(LocalDateTime.now()).build()));
    }

    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Page<WishlistItem> getWishlist(String sessionId, int page, int pageSize) {
        Customer customer = authService.getCustomerFromSession(sessionId);
        return wishlistItemRepository.findByCustomerOrderByAddedAtDesc(customer, PageRequest.of(page, pageSize));
    }
}
