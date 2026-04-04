package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.HomeCarouselSlideRequest;
import com.hasan.marketplace.dto.HomeCarouselSlideResponse;
import java.util.List;

public interface HomeCarouselService {

    List<HomeCarouselSlideResponse> getSlidesForHomePage();

    List<HomeCarouselSlideResponse> getAllSlides();

    HomeCarouselSlideResponse createSlide(HomeCarouselSlideRequest request);

    void deleteSlide(Long slideId);
}
