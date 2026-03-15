package com.dathq.swd302.creditservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCreditSettingRequest {
    @NotNull
    @Min(value = 0, message = "Value must be >= 0")
    private Integer value;

    private String description;
}
