package com.dathq.swd302.creditservice.service;
// IPostCreditService.java  (called by Kafka Consumer)

import com.dathq.swd302.creditservice.dto.CreditLockResult;

import java.util.UUID;

public interface IPostCreditService {
    boolean isFirstPost(UUID userId);
    CreditLockResult lockCreditForPost(UUID userId, String postReferenceId);
    boolean confirmPostApproved(UUID userId, String postReferenceId);
    boolean confirmPostRejected(UUID userId, String postReferenceId);
}
