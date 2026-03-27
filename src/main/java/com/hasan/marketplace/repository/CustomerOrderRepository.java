package com.hasan.marketplace.repository;

import com.hasan.marketplace.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"buyer", "items", "items.product", "items.product.seller", "items.product.category"})
    List<CustomerOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"buyer", "items", "items.product", "items.product.seller", "items.product.category"})
    Optional<CustomerOrder> findById(Long id);

    @EntityGraph(attributePaths = {"buyer", "items", "items.product", "items.product.seller", "items.product.category"})
    List<CustomerOrder> findByBuyerIdOrderByOrderDateDesc(Long buyerId);

    @EntityGraph(attributePaths = {"buyer", "items", "items.product", "items.product.seller", "items.product.category"})
    List<CustomerOrder> findDistinctByItemsProductSellerIdOrderByOrderDateDesc(Long sellerId);

    long countDistinctByItemsProductSellerId(Long sellerId);
}
