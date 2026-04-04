package com.hasan.marketplace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeCarouselSlideRequest {

    @NotBlank(message = "Image URL is required")
    @URL(message = "Please provide a valid image URL")
    @Size(max = 2000, message = "Image URL must be at most 2000 characters")
    private String imageUrl;

    @NotBlank(message = "Alt text is required")
    @Size(max = 120, message = "Alt text must be at most 120 characters")
    private String altText;

    @Min(value = 1, message = "Display order must be at least 1")
    private Integer displayOrder;
}
