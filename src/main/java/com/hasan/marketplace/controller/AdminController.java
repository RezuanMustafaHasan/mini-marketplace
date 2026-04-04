package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.HomeCarouselSlideRequest;
import com.hasan.marketplace.dto.CategoryRequest;
import com.hasan.marketplace.exception.InvalidCategoryException;
import com.hasan.marketplace.service.AdminService;
import com.hasan.marketplace.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final CategoryService categoryService;

    @GetMapping
    public String showAdminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/users")
    public String showAllUsers(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin-users";
    }

    @GetMapping("/products")
    public String showAllProducts(Model model) {
        model.addAttribute("products", adminService.getAllProducts());
        return "admin-products";
    }

    @GetMapping("/orders")
    public String showAllOrders(Model model) {
        model.addAttribute("orders", adminService.getAllOrders());
        return "admin-orders";
    }

    @GetMapping("/categories")
    public String showAllCategories(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryRequest());
        }
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-categories";
    }

    @GetMapping("/home-slides")
    public String showHomeSlides(Model model) {
        if (!model.containsAttribute("slideForm")) {
            model.addAttribute("slideForm", HomeCarouselSlideRequest.builder().displayOrder(1).build());
        }
        model.addAttribute("slides", adminService.getAllHomeSlides());
        return "admin-home-slides";
    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute("categoryForm") CategoryRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-categories";
        }

        try {
            categoryService.createCategory(request);
        } catch (InvalidCategoryException ex) {
            bindingResult.rejectValue("name", "invalid", ex.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-categories";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Category created successfully.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/home-slides")
    public String createHomeSlide(@Valid @ModelAttribute("slideForm") HomeCarouselSlideRequest request,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("slides", adminService.getAllHomeSlides());
            return "admin-home-slides";
        }

        adminService.createHomeSlide(request);
        redirectAttributes.addFlashAttribute("successMessage", "Homepage slide added successfully.");
        return "redirect:/admin/home-slides";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/home-slides/delete/{id}")
    public String deleteHomeSlide(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteHomeSlide(id);
        redirectAttributes.addFlashAttribute("successMessage", "Homepage slide deleted successfully.");
        return "redirect:/admin/home-slides";
    }
}
