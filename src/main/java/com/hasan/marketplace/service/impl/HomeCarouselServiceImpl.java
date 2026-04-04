package com.hasan.marketplace.service.impl;

import com.hasan.marketplace.dto.HomeCarouselSlideRequest;
import com.hasan.marketplace.dto.HomeCarouselSlideResponse;
import com.hasan.marketplace.entity.HomeCarouselSlide;
import com.hasan.marketplace.exception.ResourceNotFoundException;
import com.hasan.marketplace.repository.HomeCarouselSlideRepository;
import com.hasan.marketplace.service.HomeCarouselService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeCarouselServiceImpl implements HomeCarouselService {

    private final HomeCarouselSlideRepository homeCarouselSlideRepository;

    @Override
    public List<HomeCarouselSlideResponse> getSlidesForHomePage() {
        return getAllSlides();
    }

    @Override
    public List<HomeCarouselSlideResponse> getAllSlides() {
        return homeCarouselSlideRepository.findAllByOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public HomeCarouselSlideResponse createSlide(HomeCarouselSlideRequest request) {
        HomeCarouselSlide slide = HomeCarouselSlide.builder()
                .imageUrl(request.getImageUrl().trim())
                .altText(request.getAltText().trim())
                .displayOrder(request.getDisplayOrder())
                .build();

        return mapToResponse(homeCarouselSlideRepository.save(slide));
    }

    @Override
    @Transactional
    public void deleteSlide(Long slideId) {
        HomeCarouselSlide slide = homeCarouselSlideRepository.findById(slideId)
                .orElseThrow(() -> new ResourceNotFoundException("Slide not found with id: " + slideId));
        homeCarouselSlideRepository.delete(slide);
    }

    private HomeCarouselSlideResponse mapToResponse(HomeCarouselSlide slide) {
        return HomeCarouselSlideResponse.builder()
                .id(slide.getId())
                .imageUrl(slide.getImageUrl())
                .altText(slide.getAltText())
                .displayOrder(slide.getDisplayOrder())
                .build();
    }
}
