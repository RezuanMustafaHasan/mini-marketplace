package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.service.ProductService;
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

    // TODO: Replace this temporary value with the logged-in seller id from Spring Security.
    private static final Long TEMP_SELLER_ID = 1L;

    private final ProductService productService;

    @GetMapping("/products")
    public String showAllProducts(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("products", productService.searchProducts(keyword));
        model.addAttribute("keyword", keyword);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        return "product-details";
    }

    @GetMapping("/seller/products")
    public String showSellerProducts(Model model) {
        model.addAttribute("products", productService.getProductsBySeller(TEMP_SELLER_ID));
        return "seller-products";
    }

    @GetMapping("/seller/products/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        model.addAttribute("formTitle", "Add Product");
        model.addAttribute("formAction", "/seller/products");
        return "product-form";
    }

    @PostMapping("/seller/products")
    public String createProduct(@Valid @ModelAttribute("product") ProductRequest productRequest,
                                BindingResult bindingResult,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formTitle", "Add Product");
            model.addAttribute("formAction", "/seller/products");
            return "product-form";
        }

        productService.createProduct(productRequest, TEMP_SELLER_ID);
        return "redirect:/seller/products";
    }

    @GetMapping("/seller/products/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductResponse productResponse = productService.getProductById(id);

        ProductRequest productRequest = new ProductRequest();
        productRequest.setName(productResponse.getName());
        productRequest.setDescription(productResponse.getDescription());
        productRequest.setPrice(productResponse.getPrice());
        productRequest.setStock(productResponse.getStock());
        productRequest.setImageUrl(productResponse.getImageUrl());

        model.addAttribute("product", productRequest);
        model.addAttribute("productId", id);
        model.addAttribute("formTitle", "Edit Product");
        model.addAttribute("formAction", "/seller/products/update/" + id);
        return "product-form";
    }

    @PostMapping("/seller/products/update/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") ProductRequest productRequest,
                                BindingResult bindingResult,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("formTitle", "Edit Product");
            model.addAttribute("formAction", "/seller/products/update/" + id);
            return "product-form";
        }

        productService.updateProduct(id, productRequest, TEMP_SELLER_ID);
        return "redirect:/seller/products";
    }

    @PostMapping("/seller/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id, TEMP_SELLER_ID);
        return "redirect:/seller/products";
    }
}

