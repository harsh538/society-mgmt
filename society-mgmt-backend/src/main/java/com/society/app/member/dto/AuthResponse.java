package com.society.app.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login / refresh response. Field names are consumed by the frontend
 * {@code AuthContext.login(data)} — do not rename {@code member}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private MemberDto member;
}
