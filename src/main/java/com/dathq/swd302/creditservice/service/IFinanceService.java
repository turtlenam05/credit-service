package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.FinanceSummaryDTO;

public interface IFinanceService {
    FinanceSummaryDTO getSummary(int month, int year);
}
