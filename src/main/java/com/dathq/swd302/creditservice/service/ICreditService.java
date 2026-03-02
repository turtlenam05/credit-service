package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.UserWallet;

import java.util.List;
import java.util.UUID;

public interface ICreditService {
    UserWallet rechargeBalance(UUID userId, Double amountVnd);
    UserWallet getWallet(UUID userId);
    List<CreditTransaction> getTransactionHistory(UUID userId);
    boolean deductCredit(UUID userId, int credits);
    boolean lockCredit(UUID userId, int credits, String referenceId);
    boolean unlockAndDeductCredit(UUID userId, String referenceId);
    boolean unlockAndRefundCredit(UUID userId, String referenceId);
    int getDailyMessageCount(UUID userId);
    void incrementDailyMessageCount(UUID userId);
}
