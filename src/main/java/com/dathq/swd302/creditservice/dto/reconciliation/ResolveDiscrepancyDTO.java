package com.dathq.swd302.creditservice.dto.reconciliation;

import lombok.Data;

// POST /reconciliation/{id}/resolve
@Data
public class ResolveDiscrepancyDTO {
    public String notes;
}
