package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BrandRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống")
    private String name;

    private String logoUrl;

    private String description;
}
