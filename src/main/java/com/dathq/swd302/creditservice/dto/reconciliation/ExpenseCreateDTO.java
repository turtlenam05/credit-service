package com.dathq.swd302.creditservice.dto.reconciliation;

import lombok.Data;

import java.math.BigDecimal;

// POST /reconciliation/expenses
@Data
public class ExpenseCreateDTO {
    public int month;
    public int year;
    public String category;
    public BigDecimal amount;
    public String description;
}
