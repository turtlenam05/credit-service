package com.dathq.swd302.creditservice.service;


import com.dathq.swd302.creditservice.dto.CreditLockResult;
import com.dathq.swd302.creditservice.entity.TransactionType;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.dathq.swd302.creditservice.entity.CreditSetting.SettingKey.POST_COST_BASIC;
import static com.dathq.swd302.creditservice.entity.CreditSetting.SettingKey.POST_COST_PREMIUM_ADD;

@Service
@RequiredArgsConstructor
public class PostCreditServiceImpl implements  IPostCreditService  {

    private final ICreditService creditService;
    private final TransactionRepository transactionRepository;
    private final ICreditSettingService settingService;

    @Override
    public boolean isFirstPost(UUID userId) {
        // Kiểm tra toàn bộ lịch sử: user có bài đăng nào chưa (kể cả free lần đầu)
        return !transactionRepository.existsByWallet_UserIdAndType(userId, TransactionType.POST_CHARGE)
                && !transactionRepository.existsByWallet_UserIdAndReferenceType(userId, "FIRST_POST");

    }

    @Override
    public CreditLockResult lockCreditForPost(UUID userId, String postReferenceId, int type) {
        int cost = settingService.getValue(POST_COST_BASIC);
        if(type == 2){
            cost += settingService.getValue(POST_COST_PREMIUM_ADD);
        }
        return creditService.lockCredit(userId, cost, postReferenceId);

    }

    @Override
    public boolean confirmPostApproved(UUID userId, String postReferenceId) {
        return creditService.unlockAndDeductCredit(userId, postReferenceId);
    }

    @Override
    public boolean confirmPostRejected(UUID userId, String postReferenceId) {
        return creditService.unlockAndRefundCredit(userId, postReferenceId);
    }
}
