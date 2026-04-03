package com.hasan.marketplace.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundPopulatesErrorPageModel() {
        Model model = new ConcurrentModel();

        String viewName = handler.handleResourceNotFound(
                new ResourceNotFoundException("Product not found with id: 9"),
                model
        );

        assertErrorPage(model, viewName, "Resource Not Found", "Product not found with id: 9");
    }

    @Test
    void handleUnauthorizedActionPopulatesErrorPageModel() {
        Model model = new ConcurrentModel();

        String viewName = handler.handleUnauthorizedAction(
                new UnauthorizedActionException("You are not allowed to modify this product"),
                model
        );

        assertErrorPage(model, viewName, "Unauthorized Action", "You are not allowed to modify this product");
    }

    @Test
    void handleInsufficientStockPopulatesErrorPageModel() {
        Model model = new ConcurrentModel();

        String viewName = handler.handleInsufficientStock(
                new InsufficientStockException("Insufficient stock for product: Monitor"),
                model
        );

        assertErrorPage(model, viewName, "Insufficient Stock", "Insufficient stock for product: Monitor");
    }

    @Test
    void handleIllegalArgumentPopulatesErrorPageModel() {
        Model model = new ConcurrentModel();

        String viewName = handler.handleIllegalArgument(
                new IllegalArgumentException("Email is already registered"),
                model
        );

        assertErrorPage(model, viewName, "Invalid Request", "Email is already registered");
    }

    @Test
    void handleGenericExceptionPopulatesErrorPageModel() {
        Model model = new ConcurrentModel();

        String viewName = handler.handleGenericException(
                new RuntimeException("Unexpected failure"),
                model
        );

        assertErrorPage(model, viewName, "Unexpected Error", "Unexpected failure");
    }

    private void assertErrorPage(Model model, String viewName, String expectedTitle, String expectedMessage) {
        assertEquals("error-page", viewName);
        assertEquals(expectedTitle, model.getAttribute("errorTitle"));
        assertEquals(expectedMessage, model.getAttribute("errorMessage"));
    }
}
