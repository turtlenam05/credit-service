package com.dathq.swd302.creditservice.service;


import com.dathq.swd302.creditservice.dto.CreditLockResult;
import com.dathq.swd302.creditservice.entity.TransactionType;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostCreditServiceImpl implements  IPostCreditService  {

    private final ICreditService creditService;
    private final TransactionRepository transactionRepository;

    private static final int POST_COST = 10000;

    @Override
    public boolean isFirstPost(UUID userId) {
        // Kiểm tra toàn bộ lịch sử: user có bài đăng nào chưa (kể cả free lần đầu)
        return !transactionRepository.existsByWallet_UserIdAndType(userId, TransactionType.POST_CHARGE)
                && !transactionRepository.existsByWallet_UserIdAndReferenceType(userId, "FIRST_POST");

    }

    @Override
    public CreditLockResult lockCreditForPost(UUID userId, String postReferenceId) {
        return creditService.lockCredit(userId, POST_COST, postReferenceId);

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
