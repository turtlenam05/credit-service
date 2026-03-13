package com.dathq.swd302.creditservice.entity;

public enum TransactionMatchStatus {
    MATCHED,        // System amount == Gateway amount
    UNMATCHED,      // Gateway record missing entirely
    PARTIAL         // Amounts differ (e.g. rounding, partial receipt)
}
