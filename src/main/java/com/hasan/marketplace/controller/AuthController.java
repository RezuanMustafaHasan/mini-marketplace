package com.hasan.marketplace.controller;

import com.hasan.marketplace.dto.UserRegistrationRequest;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserRegistrationRequest());
        model.addAttribute("availableRoles", List.of(RoleName.BUYER, RoleName.SELLER));
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationRequest request,
                               BindingResult bindingResult,
                               Model model) {
        if (request.getRole() == RoleName.ADMIN) {
            bindingResult.rejectValue("role", "forbidden", "ADMIN registration is not allowed");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("availableRoles", List.of(RoleName.BUYER, RoleName.SELLER));
            return "register";
        }

        try {
            userService.registerUser(request);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            model.addAttribute("availableRoles", List.of(RoleName.BUYER, RoleName.SELLER));
            return "register";
        }

        return "redirect:/login?registered";
    }
}
