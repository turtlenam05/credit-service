package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionStatus;
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import com.dathq.swd302.creditservice.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    // 1. Tiêm các Repository vào để làm việc với DB
    private final TransactionRepository transactionRepository;
    private final UserWalletRepository userWalletRepository;

    @PostMapping("/payos")
    @Transactional // Thêm cái này để đảm bảo nếu lỗi thì DB không bị loạn
    public void handlePayOSWebhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println(">>> Webhook Received: " + payload);

            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data != null) {
                // Lấy orderCode dưới dạng String để tìm trong DB
                String orderCodeStr = data.get("orderCode").toString();
                String description = payload.get("desc") != null ? payload.get("desc").toString() : "";

                if ("success".equalsIgnoreCase(description)) {
                    // 2. Tìm đơn hàng trong DB
                    CreditTransaction transaction = transactionRepository.findByReferenceId(orderCodeStr);

                    // 3. Kiểm tra: Có đơn hàng và đơn hàng đó phải đang ở trạng thái PENDING
                    if (transaction != null && transaction.getStatus() == TransactionStatus.PENDING) {

                        // 4. Lấy ví của User gắn với giao dịch này
                        UserWallet wallet = transaction.getWallet();

                        // 5. Cộng tiền
                        BigDecimal amountToAdd = transaction.getAmount();
                        wallet.setBalance(wallet.getBalance().add(amountToAdd));

                        // 6. Cập nhật trạng thái giao dịch thành SUCCESS
                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setNotes("Thanh toán thành công qua PayOS");

                        // 7. Lưu lại tất cả vào Database
                        userWalletRepository.save(wallet);
                        transactionRepository.save(transaction);

                        System.out.println("====================================");
                        System.out.println(">>> ĐÃ CẬNG TIỀN THÀNH CÔNG!");
                        System.out.println("Mã đơn: " + orderCodeStr);
                        System.out.println("Số dư mới của User " + wallet.getUserId() + " là: " + wallet.getBalance());
                        System.out.println("====================================");
                    } else if (transaction == null) {
                        System.err.println("❌ Không tìm thấy mã đơn hàng: " + orderCodeStr);
                    } else {
                        System.out.println("⚠️ Giao dịch này đã được xử lý hoặc không còn ở trạng thái PENDING.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý Webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }
}