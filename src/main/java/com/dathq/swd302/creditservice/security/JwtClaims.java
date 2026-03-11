package com.dathq.swd302.creditservice.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {
    private UUID userId;
    // Add additional claims here in the future:
    // private String email;
    // private List<String> roles;
}