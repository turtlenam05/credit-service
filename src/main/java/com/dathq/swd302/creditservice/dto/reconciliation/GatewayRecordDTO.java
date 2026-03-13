package com.dathq.swd302.creditservice.dto.reconciliation;

import lombok.Data;

import java.math.BigDecimal;

// Used when admin submits gateway records per-transaction for matching.
// POST /reconciliation/{month}/{year}/audit/match
@Data
public class GatewayRecordDTO {
    /** referenceId of the CreditTransaction to match against. */
    public String transactionReferenceId;
    /** Amount the gateway recorded (null = no gateway record → UNMATCHED). */
    public BigDecimal gatewayAmount;
    public String description;
}
