package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.service.CategoryService;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
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
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping("/products")
    public String showAllProducts(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long category,
                                  Model model) {
        model.addAttribute("products", productService.searchProducts(keyword, category));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", category);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        return "product-details";
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

    private Long getCurrentUserId() {
        return userService.getAuthenticatedUser().getId();
    }
}
