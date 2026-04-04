package com.hasan.marketplace.controller;

import com.hasan.marketplace.service.HomeCarouselService;
import com.hasan.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeCarouselService homeCarouselService;
    private final ProductService productService;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long category,
                       Model model) {
        model.addAttribute("homeSlides", homeCarouselService.getSlidesForHomePage());
        model.addAttribute("products", productService.searchProducts(keyword, category));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", category);
        model.addAttribute("catalogPath", "/");
        return "index";
    }
}
