package com.dathq.swd302.creditservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAllSettingsRequest {
    private Integer postCostBasic;
    private Integer postCostPremiumAdd;
    private Integer aiChatFreeLimit;
    private Integer aiChatCostPerMsg;
}
