package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.dto.FinanceSummaryDTO;
import com.dathq.swd302.creditservice.service.IFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {
    private final IFinanceService financeService;

    @GetMapping("/summary")
    public FinanceSummaryDTO getSummary(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return financeService.getSummary(month, year);
    }
}
