package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.RefundRequestDTO;
import com.dathq.swd302.creditservice.entity.RefundRequest;

import java.util.List;
import java.util.UUID;

public interface IRefundRequestService {
    RefundRequest submitRefundRequest(UUID userId, RefundRequestDTO dto);
    RefundRequest approveRefundRequest(Long requestId, UUID adminId);
    RefundRequest rejectRefundRequest(Long requestId, UUID adminId, String reason);
    List<RefundRequest> getRefundRequestsByUser(UUID userId);
    List<RefundRequest> getAllPendingRequests();
}
