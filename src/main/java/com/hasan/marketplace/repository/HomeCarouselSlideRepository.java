package com.hasan.marketplace.repository;

import com.hasan.marketplace.entity.HomeCarouselSlide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeCarouselSlideRepository extends JpaRepository<HomeCarouselSlide, Long> {

    List<HomeCarouselSlide> findAllByOrderByDisplayOrderAscIdAsc();
}
