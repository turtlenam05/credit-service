package com.dathq.swd302.creditservice.service;

import java.util.UUID;

public interface IKafkaProducerService {
    void publishCreditPurchased(UUID userId, int credits);
    void publishCreditDeducted(UUID userId, int credits, String reason);
    void publishCreditRefunded(UUID userId, int credits, String refundRequestId);
    void publishCreditLocked(UUID userId, int credits, String postReferenceId);
}
