package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.TransactionAuditEntry;
import com.dathq.swd302.creditservice.entity.TransactionMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAuditEntry, Long> {

    // Tất cả các bút toán kiểm toán cho một kỳ đối chiếu, được sắp xếp theo ngày giao dịch giảm dần.
    List<TransactionAuditEntry> findByReconciliation_IdOrderByCreatedAtDesc(Long reconciliationId);

    // Lọc theo trạng thái khớp (đối với các huy hiệu "3 chưa khớp", "3 một phần" trong giao diện người dùng).
    List<TransactionAuditEntry> findByReconciliation_IdAndMatchStatus(Long reconciliationId, TransactionMatchStatus matchStatus);

    long countByReconciliation_IdAndMatchStatus(Long reconciliationId, TransactionMatchStatus matchStatus);

    Optional<TransactionAuditEntry> findByReconciliation_IdAndTransaction_TransactionId(Long reconciliationId, Long transactionId);
}
