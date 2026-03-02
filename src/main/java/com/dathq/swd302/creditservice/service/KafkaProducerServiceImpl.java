package com.dathq.swd302.creditservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.dathq.swd302.creditservice.config.KafkaTopicConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements IKafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Override
    public void publishCreditPurchased(UUID userId, int credits) {
        send(TOPIC_CREDIT_PURCHASED, userId, Map.of(
                "userId", userId,
                "credits", credits,
                "event", "CREDIT_PURCHASED",
                "timestamp", Instant.now().toString()
        ));
    }

    @Override
    public void publishCreditDeducted(UUID userId, int credits, String reason) {
        send(TOPIC_CREDIT_DEDUCTED, userId, Map.of(
                "userId", userId,
                "credits", credits,
                "reason", reason,
                "event", "CREDIT_DEDUCTED",
                "timestamp", Instant.now().toString()
        ));
    }

    @Override
    public void publishCreditRefunded(UUID userId, int credits, String refundRequestId) {
        send(TOPIC_CREDIT_REFUNDED, userId, Map.of(
                "userId", userId,
                "credits", credits,
                "refundRequestId", refundRequestId,
                "event", "CREDIT_REFUNDED",
                "timestamp", Instant.now().toString()
        ));
    }

    @Override
    public void publishCreditLocked(UUID userId, int credits, String postReferenceId) {
        send(TOPIC_CREDIT_LOCKED, userId, Map.of(
                "userId", userId,
                "credits", credits,
                "postReferenceId", postReferenceId,
                "event", "CREDIT_LOCKED",
                "timestamp", Instant.now().toString()
        ));
    }

    private void send(String topic, UUID userId, Map<String, Object> payload) {
        kafkaTemplate.send(topic, String.valueOf(userId), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish to topic={} userId={} event={}",
                                topic, userId, payload.get("event"), ex);
                    } else {
                        log.info("[Kafka] Published to topic={} userId={} event={} partition={} offset={}",
                                topic, userId, payload.get("event"),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
