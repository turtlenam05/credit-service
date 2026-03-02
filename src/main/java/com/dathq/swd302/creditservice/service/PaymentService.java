package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.entity.TransactionType;
import com.dathq.swd302.creditservice.entity.TransactionStatus;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import com.dathq.swd302.creditservice.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final TransactionRepository transactionRepository;
    private final UserWalletRepository userWalletRepository;

    @Value("${PAYOS_CLIENT_ID}")
    private String clientId;

    @Value("${PAYOS_API_KEY}")
    private String apiKey;

    @Value("${PAYOS_CHECKSUM_KEY}")
    private String checksumKey;

    @Value("${app.base-url:http://localhost:8086}")
    private String baseUrl;

    public String createPaymentLink(UUID userId, int amount) throws Exception {
        int MIN_AMOUNT = 10000;
        int MAX_AMOUNT = 1000000;

        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new RuntimeException("Số lượng credit mua mỗi lần phải nằm trong khoảng từ 10 đến 1.000 credit " +
                    "(tương đương 10.000 VNĐ - 1.000.000 VNĐ).");
        }

        // 0. Chuẩn bị dữ liệu cơ bản
        long orderCode = System.currentTimeMillis() / 1000;
        String description = "Nap tien " + userId;
        String returnUrl = baseUrl + "/swagger-ui/index.html";
        String cancelUrl = baseUrl + "/swagger-ui/index.html";

        // --- BƯỚC MỚI: LƯU VÀO DATABASE TRƯỚC ---
        // Tìm ví của User
        UserWallet wallet = userWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví cho User ID: " + userId));

        // Tạo bản ghi giao dịch ở trạng thái PENDING
        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(new BigDecimal(amount))
                .type(TransactionType.PURCHASE)
                .referenceType("PAYOS_ORDER")
                .referenceId(String.valueOf(orderCode)) // Lưu lại để đối soát Webhook
                .status(TransactionStatus.PENDING)
                .notes("Khởi tạo thanh toán PayOS")
                .build();

        transactionRepository.save(transaction);
        // ---------------------------------------

        // 1. Tạo chuỗi dữ liệu để ký (TreeMap để tự sắp xếp alphabet key)
        Map<String, Object> data = new TreeMap<>();
        data.put("amount", amount);
        data.put("cancelUrl", cancelUrl);
        data.put("description", description);
        data.put("orderCode", orderCode);
        data.put("returnUrl", returnUrl);

        String rawData = data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // 2. Tạo chữ ký HmacSHA256
        String signature = hmacSha256(rawData, checksumKey);

        // 3. Gọi API PayOS bằng RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        Map<String, Object> body = new HashMap<>(data);
        body.put("signature", signature);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api-merchant.payos.vn/v2/payment-requests", entity, Map.class);

            Map<String, Object> resBody = response.getBody();
            Map<String, Object> resData = (Map<String, Object>) resBody.get("data");

            // Trả về link thanh toán cho Controller
            return resData.get("checkoutUrl").toString();
        } catch (Exception e) {
            // Nếu gọi PayOS lỗi, ta nên đánh dấu giao dịch là FAILED
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setNotes("Lỗi khi gọi PayOS: " + e.getMessage());
            transactionRepository.save(transaction);
            throw new RuntimeException("PayOS Error: " + e.getMessage());
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}