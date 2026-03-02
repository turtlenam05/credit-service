package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.RefundRequestDTO;
import com.dathq.swd302.creditservice.entity.*;
import com.dathq.swd302.creditservice.repository.RefundRequestRepository;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import com.dathq.swd302.creditservice.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundRequestServiceImpl implements IRefundRequestService {

    private final RefundRequestRepository refundRequestRepository;
    private final TransactionRepository transactionRepository;
    private final UserWalletRepository userWalletRepository;
    private final IKafkaProducerService kafkaProducerService;


    @Override
    @Transactional
    public RefundRequest submitRefundRequest(UUID userId, RefundRequestDTO dto) {
        // Validate bắt buộc
        if (dto.getReason() == null || dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new RuntimeException("Vui lòng cung cấp đầy đủ thông tin: lý do và mô tả chi tiết.");
        }

        // Mỗi giao dịch chỉ được 1 refund request
        boolean alreadySubmitted = refundRequestRepository.existsByTransaction_transactionId(Long.parseLong(dto.getTransactionId()));
        if (alreadySubmitted) {
            throw new RuntimeException("Giao dịch này đã có Refund Request. Mỗi giao dịch chỉ được gửi tối đa 1 lần.");
        }

        CreditTransaction transaction = transactionRepository.findById(Long.parseLong(dto.getTransactionId()))
                .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại: " + dto.getTransactionId()));

        RefundRequest refundRequest = RefundRequest.builder()
                .userId(userId)
                .transaction(transaction)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .evidenceUrl(dto.getEvidenceUrl())
                .status(RefundStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return refundRequestRepository.save(refundRequest);
    }

    @Override
    @Transactional
    public RefundRequest approveRefundRequest(Long requestId, UUID adminId) {
        RefundRequest request = refundRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Refund Request không tồn tại: " + requestId));

        if (request.getStatus() != RefundStatus.PENDING) {
            throw new RuntimeException("Refund Request này đã được xử lý.");
        }

        // Cộng credit lại vào ví
        UserWallet wallet = userWalletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet không tồn tại!"));

        BigDecimal refundAmount = request.getTransaction().getAmount();
        wallet.setBalance(wallet.getBalance().add(refundAmount));
        wallet.setUpdatedAt(LocalDateTime.now());

        // Ghi transaction hoàn tiền
        CreditTransaction refundTx = CreditTransaction.builder()
                .wallet(wallet)
                .amount(refundAmount)
                .type(TransactionType.REFUND)
                .referenceType("REFUND_REQUEST")
                .referenceId(String.valueOf(requestId))
                .status(TransactionStatus.SUCCESS)
                .notes("Hoàn credit theo Refund Request #" + requestId + " - Admin: " + adminId)
                .createdAt(LocalDateTime.now())
                .build();

        request.setStatus(RefundStatus.APPROVED);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());

        transactionRepository.save(refundTx);
        userWalletRepository.save(wallet);
        RefundRequest saved = refundRequestRepository.save(request);

        kafkaProducerService.publishCreditRefunded(request.getUserId(), refundAmount.intValue(), String.valueOf(requestId));

        return saved;
    }

    @Override
    public RefundRequest rejectRefundRequest(Long requestId, UUID adminId, String reason) {
        RefundRequest request = refundRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Refund Request không tồn tại: " + requestId));

        if (request.getStatus() != RefundStatus.PENDING) {
            throw new RuntimeException("Refund Request này đã được xử lý.");
        }

        request.setStatus(RefundStatus.REJECTED);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNote(reason);

        return refundRequestRepository.save(request);
    }

    @Override
    public List<RefundRequest> getRefundRequestsByUser(UUID userId) {
        return refundRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<RefundRequest> getAllPendingRequests() {
        return refundRequestRepository.findByStatus(RefundStatus.PENDING);
    }
}
