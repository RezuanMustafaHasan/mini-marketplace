package com.hasan.marketplace.config;

import com.hasan.marketplace.dto.CategoryResponse;
import com.hasan.marketplace.dto.CurrentUserView;
import com.hasan.marketplace.entity.User;
import com.hasan.marketplace.service.CategoryService;
import com.hasan.marketplace.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final CategoryService categoryService;

    @ModelAttribute("currentUser")
    public CurrentUserView currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        User user;
        try {
            user = userService.findByEmail(authentication.getName());
        } catch (RuntimeException exception) {
            return null;
        }

        if (user == null || user.getRoles() == null) {
            return null;
        }

        String primaryRole = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .sorted()
                .findFirst()
                .orElse("USER");

        return CurrentUserView.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .role(primaryRole)
                .build();
    }

    @ModelAttribute("navbarCategories")
    public List<CategoryResponse> navbarCategories() {
        return categoryService.getAllCategories();
    }
}
