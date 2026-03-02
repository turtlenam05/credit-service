package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.RefundRequest;
import com.dathq.swd302.creditservice.entity.RefundStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends CrudRepository<RefundRequest, Long> {
    boolean existsByTransaction_transactionId(Long transactionId);

    List<RefundRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<RefundRequest> findByStatus(RefundStatus status);
}
