package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.HomeCarouselSlideRequest;
import com.hasan.marketplace.dto.HomeCarouselSlideResponse;
import com.hasan.marketplace.dto.OrderResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.dto.UserResponse;
import java.util.List;

public interface AdminService {

    List<UserResponse> getAllUsers();

    List<ProductResponse> getAllProducts();

    List<OrderResponse> getAllOrders();

    List<HomeCarouselSlideResponse> getAllHomeSlides();

    HomeCarouselSlideResponse createHomeSlide(HomeCarouselSlideRequest request);

    void deleteHomeSlide(Long slideId);
}
