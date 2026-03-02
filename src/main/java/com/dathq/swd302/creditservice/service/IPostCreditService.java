package com.dathq.swd302.creditservice.service;
// IPostCreditService.java  (called by Kafka Consumer)

import java.util.UUID;

public interface IPostCreditService {
    boolean isFirstPost(UUID userId);
    boolean lockCreditForPost(UUID userId, String postReferenceId);
    boolean confirmPostApproved(UUID userId, String postReferenceId);
    boolean confirmPostRejected(UUID userId, String postReferenceId);
}
