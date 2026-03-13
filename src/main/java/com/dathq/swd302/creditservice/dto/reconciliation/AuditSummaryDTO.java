package com.dathq.swd302.creditservice.dto.reconciliation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Wraps the full audit list + badge counts.
@Data
@Builder
public class AuditSummaryDTO {
    public long totalCount;
    public long matchedCount;
    public long unmatchedCount;
    public long partialCount;
    public List<TransactionAuditDTO> entries;
}
