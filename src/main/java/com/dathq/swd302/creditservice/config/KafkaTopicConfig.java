package com.dathq.swd302.creditservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String TOPIC_CREDIT_PURCHASED = "credit.purchased";
    public static final String TOPIC_CREDIT_DEDUCTED  = "credit.deducted";
    public static final String TOPIC_CREDIT_REFUNDED  = "credit.refunded";
    public static final String TOPIC_CREDIT_LOCKED    = "credit.locked";
    public static final String TOPIC_LISTING_APPROVED = "listing.approved";
    public static final String TOPIC_LISTING_REJECTED = "listing.rejected";
    public static final String TOPIC_AI_CHAT_MESSAGE  = "ai.chat.message";

    @Bean
    public NewTopic creditPurchasedTopic() { return build(TOPIC_CREDIT_PURCHASED); }
    @Bean public NewTopic creditDeductedTopic()  { return build(TOPIC_CREDIT_DEDUCTED);  }
    @Bean public NewTopic creditRefundedTopic()  { return build(TOPIC_CREDIT_REFUNDED);  }
    @Bean public NewTopic creditLockedTopic()    { return build(TOPIC_CREDIT_LOCKED);    }
    @Bean public NewTopic listingApprovedTopic() { return build(TOPIC_LISTING_APPROVED); }
    @Bean public NewTopic listingRejectedTopic() { return build(TOPIC_LISTING_REJECTED); }
    @Bean public NewTopic aiChatMessageTopic()   { return build(TOPIC_AI_CHAT_MESSAGE);  }

    private NewTopic build(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
