package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.CreditLockResult;
import com.dathq.swd302.creditservice.entity.*;
import com.dathq.swd302.creditservice.repository.CreditReservationRepository;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import com.dathq.swd302.creditservice.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CreditService implements ICreditService {
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CreditReservationRepository reservationRepository;
    private final IKafkaProducerService kafkaProducerService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String DAILY_MSG_KEY = "daily_msg:";

    @Transactional
    @Override
    public UserWallet rechargeBalance(UUID userId, Double amountVnd) {
        // 1. Quy đổi: 1000 VNĐ = 1 Credit
        long creditsToAdd = (long) (amountVnd / 1000);
        BigDecimal creditsBigDecimal = BigDecimal.valueOf(creditsToAdd);

        // 2. Tìm hoặc tạo ví (Lưu ý: UserWallet của bạn cần có constructor phù hợp hoặc dùng Builder)
        UserWallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(userId);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setTotalSpent(BigDecimal.ZERO);
                    newWallet.setCreatedAt(LocalDateTime.now());
                    return walletRepository.save(newWallet);
                });

        // 3. Cập nhật số dư
        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amountVnd)));
        wallet.setUpdatedAt(LocalDateTime.now());

        // 4. Tạo lịch sử giao dịch bằng CreditTransaction
        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet) // Gán trực tiếp đối tượng wallet vào
                .amount(creditsBigDecimal)
                .type(TransactionType.PURCHASE) // Nạp tiền dùng PURCHASE hoặc bạn thêm RECHARGE vào enum
                .referenceType("PAYOS_RECHARGE")
                .notes("Nạp tiền từ PayOS: " + amountVnd + " VNĐ")
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        UserWallet saved = walletRepository.save(wallet);

        kafkaProducerService.publishCreditPurchased(userId, (int) creditsToAdd);

        return saved;
    }

    // Lấy thông tin ví
    @Override
    public UserWallet getWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet không tồn tại!"));
    }


    // Xem lịch sử giao dịch
    @Override
    public List<CreditTransaction> getTransactionHistory(UUID userId) {
        // Sử dụng hàm findByWallet_UserId chúng ta đã sửa lúc nãy
        return transactionRepository.findByWallet_UserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public boolean deductCredit(UUID userId, int credits) {
        UserWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet không tồn tại!"));

        if (wallet.getBalance().compareTo(BigDecimal.valueOf(credits)) < 0) {
            return false;
        }

        wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(credits)));
        wallet.setTotalSpent(wallet.getTotalSpent().add(BigDecimal.valueOf(credits)));
        wallet.setUpdatedAt(LocalDateTime.now());

        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(BigDecimal.valueOf(credits))
                .type(TransactionType.AI_CHAT)
                .referenceType("AI_CHAT")
                .status(TransactionStatus.SUCCESS)
                .notes("Trừ credit cho AI chat")
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        walletRepository.save(wallet);

        kafkaProducerService.publishCreditDeducted(userId, credits, "AI_CHAT");

        return true;
    }

    @Override
    @Transactional
    public CreditLockResult lockCredit(UUID userId, int credits, String referenceId) {

        if (reservationRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Post already has a reservation: " + referenceId);
        }
        UserWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet không tồn tại!"));

        boolean isFirstPost = !transactionRepository.existsByWallet_UserIdAndType(userId, TransactionType.POST_CHARGE)
                && !transactionRepository.existsByWallet_UserIdAndReferenceType(userId, "FIRST_POST");


        if (isFirstPost) {
            // Record the free post so next time isFirstPost = false
            CreditTransaction freeTransaction = CreditTransaction.builder()
                    .wallet(wallet)
                    .amount(BigDecimal.ZERO)
                    .type(TransactionType.POST_CHARGE)
                    .referenceType("FIRST_POST")
                    .referenceId(referenceId)
                    .status(TransactionStatus.SUCCESS)
                    .notes("Bài đăng đầu tiên miễn phí")
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(freeTransaction);

            // Still create a reservation with 0 amount so resolve flow works uniformly
            CreditReservation reservation = CreditReservation.builder()
                    .wallet(wallet)
                    .amount(BigDecimal.ZERO)
                    .referenceId(referenceId)
                    .listingId(referenceId)
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            reservationRepository.save(reservation);

            kafkaProducerService.publishCreditLocked(userId, 0, referenceId);
            return CreditLockResult.free(referenceId);
        }

        if (wallet.getBalance().compareTo(BigDecimal.valueOf(credits)) < 0) {
            return CreditLockResult.insufficient();
        }
        BigDecimal negativeAmount = BigDecimal.valueOf(credits).negate();
        wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(credits)));
        wallet.setUpdatedAt(LocalDateTime.now());

        CreditReservation reservation = CreditReservation.builder()
                .wallet(wallet)
                .amount(BigDecimal.valueOf(credits))
                .referenceId(referenceId)
                .listingId(referenceId)
                .status(ReservationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);

        // ✅ Missing transaction record — now added with negative amount
        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(negativeAmount) // -credits to indicate deduction
                .type(TransactionType.POST_CHARGE)
                .referenceType("POST_LOCK")
                .referenceId(referenceId)
                .status(TransactionStatus.SUCCESS)
                .notes("Khóa credit cho bài đăng: " + referenceId)
                .createdAt(LocalDateTime.now())
                .build();

        walletRepository.save(wallet);

        kafkaProducerService.publishCreditLocked(userId, credits, referenceId);

        return CreditLockResult.paid(referenceId, credits);
    }

    @Override
    @Transactional
    public boolean unlockAndDeductCredit(UUID userId, String referenceId) {
        CreditReservation reservation = reservationRepository.findByReferenceIdAndStatus(referenceId, ReservationStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Reservation không tồn tại hoặc đã xử lý: " + referenceId));

        UserWallet wallet = reservation.getWallet();

        reservation.setStatus(ReservationStatus.SUCCESS);


        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(reservation.getAmount())
                .type(TransactionType.POST_CHARGE)
                .referenceType("POST_APPROVED")
                .referenceId(referenceId)
                .status(TransactionStatus.SUCCESS)
                .notes("Trừ credit đăng bài - bài được duyệt: " + referenceId)
                .createdAt(LocalDateTime.now())
                .build();

        wallet.setTotalSpent(wallet.getTotalSpent().add(reservation.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
        reservationRepository.save(reservation);
        walletRepository.save(wallet);

        kafkaProducerService.publishCreditDeducted(userId, reservation.getAmount().intValue(), "POST_APPROVED");

        return true;
    }

    @Override
    @Transactional
    public boolean unlockAndRefundCredit(UUID userId, String referenceId) {
        CreditReservation reservation = reservationRepository.findByReferenceIdAndStatus(referenceId, ReservationStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Reservation không tồn tại hoặc đã xử lý: " + referenceId));

        UserWallet wallet = reservation.getWallet();

        wallet.setBalance(wallet.getBalance().add(reservation.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());

        reservation.setStatus(ReservationStatus.FAILED);


        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(reservation.getAmount())
                .type(TransactionType.REFUND)
                .referenceType("POST_REJECTED")
                .referenceId(referenceId)
                .status(TransactionStatus.SUCCESS)
                .notes("Hoàn credit do bài bị từ chối: " + referenceId)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        reservationRepository.save(reservation);
        walletRepository.save(wallet);

        kafkaProducerService.publishCreditRefunded(userId, reservation.getAmount().intValue(), referenceId);

        return true;
    }

    @Override
    public int getDailyMessageCount(UUID userId) {
        String key = DAILY_MSG_KEY + userId;
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

    @Override
    public void incrementDailyMessageCount(UUID userId) {
        String key = DAILY_MSG_KEY + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // Set TTL đến 00:00 GMT+7 hôm sau
            redisTemplate.expire(key, getSecondsUntilMidnightGMT7(), TimeUnit.SECONDS);
        }

    }
    private long getSecondsUntilMidnightGMT7() {
        java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
        java.time.ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId);
        return java.time.Duration.between(now, midnight).getSeconds();
    }
}