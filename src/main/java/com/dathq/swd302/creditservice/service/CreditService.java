package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.CreditLockResult;
import com.dathq.swd302.creditservice.entity.*;
import com.dathq.swd302.creditservice.exception.InsufficientBalanceException;
import com.dathq.swd302.creditservice.exception.ReservationAlreadyExistsException;
import com.dathq.swd302.creditservice.exception.ReservationNotFoundException;
import com.dathq.swd302.creditservice.exception.WalletNotFoundException;
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

    // ─── Recharge ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserWallet rechargeBalance(UUID userId, Double amountVnd) {
        long creditsToAdd = (long) (amountVnd / 1000);

        UserWallet wallet = findOrCreateWallet(userId);

        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amountVnd)));
        wallet.setUpdatedAt(LocalDateTime.now());

        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(BigDecimal.valueOf(creditsToAdd))
                .type(TransactionType.PURCHASE)
                .referenceType("PAYOS_RECHARGE")
                .notes("Nạp tiền từ PayOS: " + amountVnd + " VNĐ")
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        UserWallet saved = walletRepository.save(wallet);

        kafkaProducerService.publishCreditPurchased(userId, (int) creditsToAdd);
        return saved;
    }

    // ─── Wallet ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserWallet getWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        UserWallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .reservedBalance(BigDecimal.ZERO)
                                .totalSpent(BigDecimal.ZERO)
                                .status("ACTIVE")
                                .build()
                ));
    }
    @Override
    @Transactional
    public UserWallet createWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(
                        UserWallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .reservedBalance(BigDecimal.ZERO)
                                .totalSpent(BigDecimal.ZERO)
                                .status("ACTIVE")
                                .build()
                ));
    }

    // ─── Transactions ─────────────────────────────────────────────────────────

    @Override
    public List<CreditTransaction> getTransactionHistory(UUID userId) {
        return transactionRepository.findByWallet_UserIdOrderByCreatedAtDesc(userId);
    }

    // ─── Deduct ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean deductCredit(UUID userId, int credits) {
        UserWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        if (wallet.getBalance().compareTo(BigDecimal.valueOf(credits)) < 0) {
            throw new InsufficientBalanceException(userId, credits, wallet.getBalance().doubleValue());
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

    // ─── Lock ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CreditLockResult lockCredit(UUID userId, int credits, String referenceId) {
        if (reservationRepository.existsByReferenceId(referenceId)) {
            throw new ReservationAlreadyExistsException(referenceId);
        }

        UserWallet wallet = getWallet(userId);

        boolean isFirstPost = !transactionRepository.existsByWallet_UserIdAndType(userId, TransactionType.POST_CHARGE)
                && !transactionRepository.existsByWallet_UserIdAndReferenceType(userId, "FIRST_POST");

        if (isFirstPost) {
            return handleFirstPostFree(wallet, referenceId, userId);
        }

        if (wallet.getBalance().compareTo(BigDecimal.valueOf(credits)) < 0) {
            throw new InsufficientBalanceException(userId, credits, wallet.getBalance().doubleValue());
        }

        return handlePaidPost(wallet, userId, credits, referenceId);
    }

    // ─── Unlock ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean unlockAndDeductCredit(UUID userId, String referenceId) {
        CreditReservation reservation = reservationRepository
                .findByReferenceIdAndStatus(referenceId, ReservationStatus.PENDING)
                .orElseThrow(() -> new ReservationNotFoundException(referenceId));

        UserWallet wallet = reservation.getWallet();

        reservation.setStatus(ReservationStatus.SUCCESS);
        wallet.setTotalSpent(wallet.getTotalSpent().add(reservation.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());

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

        transactionRepository.save(transaction);
        reservationRepository.save(reservation);
        walletRepository.save(wallet);

        kafkaProducerService.publishCreditDeducted(userId, reservation.getAmount().intValue(), "POST_APPROVED");
        return true;
    }

    @Override
    @Transactional
    public boolean unlockAndRefundCredit(UUID userId, String referenceId) {
        CreditReservation reservation = reservationRepository
                .findByReferenceIdAndStatus(referenceId, ReservationStatus.PENDING)
                .orElseThrow(() -> new ReservationNotFoundException(referenceId));

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

    // ─── Daily Message Count ──────────────────────────────────────────────────

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
            redisTemplate.expire(key, getSecondsUntilMidnightGMT7(), TimeUnit.SECONDS);
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private UserWallet findOrCreateWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(userId);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setTotalSpent(BigDecimal.ZERO);
                    newWallet.setCreatedAt(LocalDateTime.now());
                    return walletRepository.save(newWallet);
                });
    }

    private CreditLockResult handleFirstPostFree(UserWallet wallet, String referenceId, UUID userId) {
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

    private CreditLockResult handlePaidPost(UserWallet wallet, UUID userId, int credits, String referenceId) {
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

        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(negativeAmount)
                .type(TransactionType.POST_CHARGE)
                .referenceType("POST_LOCK")
                .referenceId(referenceId)
                .status(TransactionStatus.SUCCESS)
                .notes("Khóa credit cho bài đăng: " + referenceId)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        walletRepository.save(wallet);

        kafkaProducerService.publishCreditLocked(userId, credits, referenceId);
        return CreditLockResult.paid(referenceId, credits);
    }

    private long getSecondsUntilMidnightGMT7() {
        java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
        java.time.ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId);
        return java.time.Duration.between(now, midnight).getSeconds();
    }
}