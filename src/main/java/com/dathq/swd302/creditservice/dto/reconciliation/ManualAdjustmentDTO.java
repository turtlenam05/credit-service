package com.dathq.swd302.creditservice.dto.reconciliation;

import lombok.Data;

import java.util.UUID;

// POST /reconciliation/adjust
@Data
public class ManualAdjustmentDTO {
    public UUID userId;
    /** Positive = add credits, negative = deduct credits. */
    public int creditAmount;
    public String reason;
    public String referenceId;
}
