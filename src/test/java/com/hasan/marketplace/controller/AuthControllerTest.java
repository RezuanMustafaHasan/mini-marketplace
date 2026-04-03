package com.hasan.marketplace.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.hasan.marketplace.dto.UserRegistrationRequest;
import com.hasan.marketplace.entity.RoleName;
import com.hasan.marketplace.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void loginPageReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPageExposesFormBackingObjectAndAllowedRoles() throws Exception {
        MvcResult result = mockMvc.perform(get("/register").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user", "availableRoles"))
                .andReturn();

        UserRegistrationRequest form = (UserRegistrationRequest) result.getModelAndView().getModel().get("user");
        @SuppressWarnings("unchecked")
        List<RoleName> availableRoles = (List<RoleName>) result.getModelAndView().getModel().get("availableRoles");

        assertTrue(form != null);
        assertTrue(availableRoles.contains(RoleName.BUYER));
        assertTrue(availableRoles.contains(RoleName.SELLER));
        assertTrue(!availableRoles.contains(RoleName.ADMIN));
    }

    @Test
    void registerUserRejectsAdminRoleBeforeCallingService() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Admin User")
                        .param("email", "admin@example.com")
                        .param("password", "secret123")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "role"))
                .andExpect(model().attributeExists("availableRoles"));

        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUserReturnsFormWhenValidationFails() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "")
                        .param("email", "not-an-email")
                        .param("password", "123")
                        .param("role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "fullName", "email", "password"))
                .andExpect(model().attributeExists("availableRoles"));

        verify(userService, never()).registerUser(any(UserRegistrationRequest.class));
    }

    @Test
    void registerUserShowsDuplicateEmailError() throws Exception {
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Buyer User")
                        .param("email", "buyer@example.com")
                        .param("password", "secret123")
                        .param("role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "email"))
                .andExpect(model().attributeExists("availableRoles"));
    }

    @Test
    void registerUserRedirectsToLoginWhenSuccessful() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Seller User")
                        .param("email", "seller@example.com")
                        .param("password", "secret123")
                        .param("role", "SELLER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).registerUser(any(UserRegistrationRequest.class));
    }
}
