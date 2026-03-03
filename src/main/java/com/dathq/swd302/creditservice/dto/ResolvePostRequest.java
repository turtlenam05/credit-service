package com.dathq.swd302.creditservice.dto;

public record ResolvePostRequest(String transactionId, PostAction action) {
}
