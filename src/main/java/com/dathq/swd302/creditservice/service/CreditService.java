package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionType; // Nhớ import cái này
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import com.dathq.swd302.creditservice.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {
    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public UserWallet rechargeBalance(Long userId, Double amountVnd) {
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
        wallet.setBalance(wallet.getBalance().add(creditsBigDecimal));
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
        return walletRepository.save(wallet);
    }

    // Lấy thông tin ví
    public UserWallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet không tồn tại!"));
    }

    // Xem lịch sử giao dịch
    public List<CreditTransaction> getTransactionHistory(Long userId) {
        // Sử dụng hàm findByWallet_UserId chúng ta đã sửa lúc nãy
        return transactionRepository.findByWallet_UserIdOrderByCreatedAtDesc(userId);
    }
}