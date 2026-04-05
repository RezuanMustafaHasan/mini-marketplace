package com.hasan.marketplace.controller.api;

import com.hasan.marketplace.dto.ProductRequest;
import com.hasan.marketplace.dto.ProductResponse;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.exception.UnauthorizedActionException;
import com.hasan.marketplace.service.ProductService;
import com.hasan.marketplace.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final UserService userService;

    @GetMapping
    public List<ProductResponse> getProducts(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long categoryId) {
        return productService.searchProducts(keyword, categoryId);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request, getSellerOrAdminId());
        URI location = URI.create("/api/products/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request, getSellerOrAdminId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id, getSellerOrAdminId());
    }

    private Long getSellerOrAdminId() {
        User currentUser = userService.getAuthenticatedUser();
        boolean canManageProducts = userService.hasRole(currentUser, RoleName.SELLER)
                || userService.hasRole(currentUser, RoleName.ADMIN);

        if (!canManageProducts) {
            throw new UnauthorizedActionException("Only sellers or admins can manage products.");
        }

        return currentUser.getId();
    }
}
