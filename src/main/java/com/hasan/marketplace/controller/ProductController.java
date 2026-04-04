package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductReviewRequest;
import com.hasan.marketplace.dto.ProductReviewResponse;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.service.CategoryService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.ReviewService;
import com.hasan.marketplace.service.UserService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class ProductController {

    private static final Long TEMP_SELLER_ID = 1L;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final ObjectProvider<UserService> userServiceProvider;

    @GetMapping("/products")
    public String showAllProducts(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long category,
                                  Model model) {
        model.addAttribute("products", productService.searchProducts(keyword, category));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", category);
        model.addAttribute("catalogPath", "/products");
        return "products";
    }

    @GetMapping("/products/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        populateProductDetailsModel(id, model, getCurrentUser(), null);
        return "product-details";
    }

    @PostMapping("/products/{id}/reviews")
    public String submitReview(@PathVariable Long id,
                               @Valid @ModelAttribute("reviewForm") ProductReviewRequest reviewForm,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        if (currentUser == null || currentUser.getId() == null) {
            throw new UnauthorizedActionException("Please login to submit a review.");
        }

        if (bindingResult.hasErrors()) {
            populateProductDetailsModel(id, model, currentUser, reviewForm);
            return "product-details";
        }

        try {
            reviewService.createOrUpdateReview(id, reviewForm, currentUser.getId());
        } catch (IllegalArgumentException | UnauthorizedActionException ex) {
            bindingResult.reject("review", ex.getMessage());
            populateProductDetailsModel(id, model, currentUser, reviewForm);
            return "product-details";
        }

        redirectAttributes.addFlashAttribute("reviewSuccessMessage", "Your review has been saved.");
        return "redirect:/products/" + id;
    }

    @GetMapping("/seller/products")
    public String showSellerProducts(Model model) {
        model.addAttribute("products", productService.getProductsBySeller(getCurrentUserId()));
        return "seller-products";
    }

    @GetMapping("/seller/products/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        populateProductForm(model, "Add Product", "/seller/products");
        return "product-form";
    }

    @PostMapping("/seller/products")
    public String createProduct(@Valid @ModelAttribute("product") ProductRequest productRequest,
                                BindingResult bindingResult,
                                Model model) {
        if (bindingResult.hasErrors()) {
            populateProductForm(model, "Add Product", "/seller/products");
            return "product-form";
        }

        productService.createProduct(productRequest, getCurrentUserId());
        return "redirect:/seller/products";
    }

    @GetMapping("/seller/products/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductResponse productResponse = productService.getProductByIdForSeller(id, getCurrentUserId());

        ProductRequest productRequest = new ProductRequest();
        productRequest.setName(productResponse.getName());
        productRequest.setDescription(productResponse.getDescription());
        productRequest.setPrice(productResponse.getPrice());
        productRequest.setStock(productResponse.getStock());
        productRequest.setCategoryId(productResponse.getCategoryId());
        productRequest.setImageUrl(productResponse.getImageUrl());

        model.addAttribute("product", productRequest);
        model.addAttribute("productId", id);
        populateProductForm(model, "Edit Product", "/seller/products/update/" + id);
        return "product-form";
    }

    @PostMapping("/seller/products/update/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") ProductRequest productRequest,
                                BindingResult bindingResult,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            populateProductForm(model, "Edit Product", "/seller/products/update/" + id);
            return "product-form";
        }

        productService.updateProduct(id, productRequest, getCurrentUserId());
        return "redirect:/seller/products";
    }

    @PostMapping("/seller/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id, getCurrentUserId());
        return "redirect:/seller/products";
    }

    private void populateProductForm(Model model, String formTitle, String formAction) {
        model.addAttribute("formTitle", formTitle);
        model.addAttribute("formAction", formAction);
        model.addAttribute("availableCategories", categoryService.getAllCategories());
    }

    private void populateProductDetailsModel(Long productId,
                                             Model model,
                                             User currentUser,
                                             ProductReviewRequest reviewFormOverride) {
        ProductResponse product = productService.getProductById(productId);
        List<ProductReviewResponse> reviews = reviewService.getReviewsForProduct(productId);

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);

        boolean canReview = currentUser != null
                && currentUser.getId() != null
                && reviewService.canUserReviewProduct(productId, currentUser.getId());
        model.addAttribute("canReview", canReview);

        if (reviewFormOverride != null) {
            model.addAttribute("reviewForm", reviewFormOverride);
            return;
        }

        if (canReview) {
            ProductReviewRequest reviewForm = reviewService.getReviewForProductByReviewer(productId, currentUser.getId())
                    .map(existingReview -> ProductReviewRequest.builder()
                            .rating(existingReview.getRating())
                            .comment(existingReview.getComment())
                            .build())
                    .orElseGet(ProductReviewRequest::new);
            model.addAttribute("reviewForm", reviewForm);
            return;
        }

        model.addAttribute("reviewForm", new ProductReviewRequest());
    }

    private Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null && user.getId() != null ? user.getId() : TEMP_SELLER_ID;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserService userService = userServiceProvider.getIfAvailable();

        if (authentication == null || userService == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        if (email == null || "anonymousUser".equals(email)) {
            return null;
        }

        try {
            return userService.findByEmail(email);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
