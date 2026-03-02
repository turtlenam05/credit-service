package com.dathq.swd302.creditservice.service;

import java.util.UUID;

public interface IPaymentService {
    String createPaymentLink(UUID userId, int amount) throws Exception;
}
