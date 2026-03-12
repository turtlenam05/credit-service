package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionStatus;
import com.dathq.swd302.creditservice.entity.TransactionType;
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.exception.PayOSException;
import com.dathq.swd302.creditservice.exception.PaymentAmountException;
import com.dathq.swd302.creditservice.exception.WalletNotFoundException;
import com.dathq.swd302.creditservice.repository.TransactionRepository;
import com.dathq.swd302.creditservice.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private static final int MIN_AMOUNT = 10_000;
    private static final int MAX_AMOUNT = 1_000_000;
    private static final String PAYOS_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    private final TransactionRepository transactionRepository;
    private final UserWalletRepository userWalletRepository;
    private final RestTemplate restTemplate;

    @Value("${PAYOS_CLIENT_ID}")
    private String clientId;

    @Value("${PAYOS_API_KEY}")
    private String apiKey;

    @Value("${PAYOS_CHECKSUM_KEY}")
    private String checksumKey;

    @Value("${app.base-url:http://estate.maik.io.vn/credit}")
    private String baseUrl;


    @Value("$https://api.estate.maik.io.vn/credit/payment/success")
    private String returnUrl;

    @Value("https://api.estate.maik.io.vn/credit/payment/cancel")
    private String cancelUrl;

    @Override
    public String createPaymentLink(UUID userId, int amount) {
        validateAmount(amount);

        UserWallet wallet = userWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        long orderCode = System.currentTimeMillis() / 1000;
        String description = "Nap " + userId.toString().substring(0, 8);

        CreditTransaction transaction = createPendingTransaction(wallet, amount, orderCode);

        try {
            String checkoutUrl = callPayOS(amount, orderCode, description, returnUrl, cancelUrl);
            log.info("PayOS payment link created for userId={}, orderCode={}", userId, orderCode);
            return checkoutUrl;
        } catch (Exception ex) {
            markTransactionFailed(transaction, ex.getMessage());
            throw new PayOSException(ex.getMessage(), ex);
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void validateAmount(int amount) {
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new PaymentAmountException(amount, MIN_AMOUNT, MAX_AMOUNT);
        }
    }

    private CreditTransaction createPendingTransaction(UserWallet wallet, int amount, long orderCode) {
        CreditTransaction transaction = CreditTransaction.builder()
                .wallet(wallet)
                .amount(new BigDecimal(amount))
                .type(TransactionType.PURCHASE)
                .referenceType("PAYOS_ORDER")
                .referenceId(String.valueOf(orderCode))
                .status(TransactionStatus.PENDING)
                .notes("Khởi tạo thanh toán PayOS")
                .build();
        return transactionRepository.save(transaction);
    }

    private void markTransactionFailed(CreditTransaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setNotes("Lỗi khi gọi PayOS: " + reason);
        transactionRepository.save(transaction);
        log.warn("Transaction {} marked FAILED: {}", transaction.getReferenceId(), reason);
    }

    private String callPayOS(int amount, long orderCode, String description,
                             String returnUrl, String cancelUrl) {
        // TreeMap ensures alphabetical key order required for HMAC signing
        Map<String, Object> data = new TreeMap<>();
        data.put("amount", amount);
        data.put("cancelUrl", cancelUrl);
        data.put("description", description);
        data.put("orderCode", orderCode);
        data.put("returnUrl", returnUrl);

        String rawData = data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        String signature = hmacSha256(rawData, checksumKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        Map<String, Object> body = new HashMap<>(data);
        body.put("signature", signature);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PAYOS_URL, new HttpEntity<>(body, headers), Map.class);

            Map<?, ?> resBody = response.getBody();
            if (resBody == null || !resBody.containsKey("data")) {
                throw new PayOSException("Empty or malformed response from PayOS");
            }

            Map<?, ?> resData = (Map<?, ?>) resBody.get("data");
            Object checkoutUrl = resData.get("checkoutUrl");
            if (checkoutUrl == null) {
                throw new PayOSException("Missing checkoutUrl in PayOS response");
            }

            return checkoutUrl.toString();
        } catch (RestClientException ex) {
            throw new PayOSException("HTTP call failed: " + ex.getMessage(), ex);
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new PayOSException("Failed to generate HMAC signature", ex);
        }
    }
}