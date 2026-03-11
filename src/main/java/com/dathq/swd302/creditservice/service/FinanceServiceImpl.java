package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.FinanceSummaryDTO;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements IFinanceService {

    private final TransactionRepository transactionRepository;

    @Override
    public FinanceSummaryDTO getSummary(int month, int year) {

        BigDecimal revenue = transactionRepository.getRevenue(month, year);
        BigDecimal refunds = transactionRepository.getRefunds(month, year);

        if(revenue == null) revenue = BigDecimal.ZERO;
        if(refunds == null) refunds = BigDecimal.ZERO;

        FinanceSummaryDTO dto = new FinanceSummaryDTO();

        dto.setMonth(month + "/" + year);
        dto.setTotalRevenue(revenue);
        dto.setTotalRefunds(refunds);
        dto.setNetRevenue(revenue.subtract(refunds));

        return dto;
    }
}
