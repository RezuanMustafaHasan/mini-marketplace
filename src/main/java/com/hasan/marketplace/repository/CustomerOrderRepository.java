package com.hasan.marketplace.repository;

import com.hasan.marketplace.entity.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByBuyerId(Long buyerId);

    List<CustomerOrder> findByBuyerEmail(String email);
}

