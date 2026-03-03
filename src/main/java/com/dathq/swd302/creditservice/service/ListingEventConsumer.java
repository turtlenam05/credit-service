package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.CreditLockResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingEventConsumer {

    private final IPostCreditService postCreditService;

    @KafkaListener(topics = "listing.post.approved", groupId = "credit-service")
    public void handlePostApproved(Map<String, Object> payload) {
        try {
            UUID userId = UUID.fromString(payload.get("userId").toString());
            String postReferenceId = payload.get("postId").toString();

            boolean isFirstPost = postCreditService.isFirstPost(userId);
            if (!isFirstPost) {
                postCreditService.confirmPostApproved(userId, postReferenceId);
                log.info(">>> Post approved - Deducted 10 credits for userId: {}, postId: {}", userId, postReferenceId);
            } else {
                log.info(">>> First post approved - No credit deducted for userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error handling listing.post.approved: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "listing.post.rejected", groupId = "credit-service")
    public void handlePostRejected(Map<String, Object> payload) {
        try {
            UUID userId = UUID.fromString(payload.get("userId").toString());
            String postReferenceId = payload.get("postId").toString();

            boolean isFirstPost = postCreditService.isFirstPost(userId);
            if (!isFirstPost) {
                postCreditService.confirmPostRejected(userId, postReferenceId);
                log.info(">>> Post rejected - Refunded 10 credits for userId: {}, postId: {}", userId, postReferenceId);
            }
        } catch (Exception e) {
            log.error("Error handling listing.post.rejected: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "listing.post.submitted", groupId = "credit-service")
    public void handlePostSubmitted(Map<String, Object> payload) {
        try {
            UUID userId = UUID.fromString(payload.get("userId").toString());
            String postReferenceId = payload.get("postId").toString();

            boolean isFirstPost = postCreditService.isFirstPost(userId);
            if (!isFirstPost) {
                CreditLockResult locked = postCreditService.lockCreditForPost(userId, postReferenceId);
//                if (!locked) {
//                    log.warn(">>> Insufficient credits for userId: {} when submitting post: {}", userId, postReferenceId);
//                    // TODO: publish event back to listing service to notify insufficient credit
//                } else {
//                    log.info(">>> Locked 10 credits for userId: {}, postId: {}", userId, postReferenceId);
//                }
            }
        } catch (Exception e) {
            log.error("Error handling listing.post.submitted: {}", e.getMessage(), e);
        }
    }
}
