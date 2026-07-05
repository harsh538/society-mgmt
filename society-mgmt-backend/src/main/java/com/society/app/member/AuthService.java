package com.society.app.member;

import com.society.app.member.dto.AuthResponse;
import com.society.app.member.dto.ChangePasswordRequest;
import com.society.app.member.dto.LoginRequest;
import com.society.app.member.dto.MemberDto;
import com.society.app.member.dto.RefreshRequest;
import com.society.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Authentication / account-management service. Encapsulates the
 * authentication-manager call, token issuance, and password rotation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getPhone(), req.getPassword()));

        Member member = memberRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new UsernameNotFoundException("Member not found"));

        member.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        memberRepository.save(member);

        String accessToken = jwtService.generateAccessToken(req.getPhone());
        String refreshToken = jwtService.generateRefreshToken(req.getPhone());
        return new AuthResponse(accessToken, refreshToken, MemberDto.from(member));
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        if (!jwtService.isRefreshTokenValid(req.getRefreshToken())) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        String phone = jwtService.extractPhone(req.getRefreshToken());
        Member member = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found"));
        if (!member.isActive()) {
            throw new DisabledException("Account is disabled");
        }

        // Rotate: invalidate the presented token, issue a brand-new pair.
        jwtService.invalidateRefreshToken(req.getRefreshToken());
        String newAccess = jwtService.generateAccessToken(phone);
        String newRefresh = jwtService.generateRefreshToken(phone);
        return new AuthResponse(newAccess, newRefresh, MemberDto.from(member));
    }

    public void logout(String phone, String refreshToken) {
        jwtService.invalidateRefreshToken(refreshToken);
    }

    @Transactional(readOnly = true)
    public MemberDto getMe(String phone) {
        return memberRepository.findByPhone(phone)
                .map(MemberDto::from)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + phone));
    }

    @Transactional
    public void changePassword(String phone, ChangePasswordRequest req) {
        Member member = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found"));
        if (!passwordEncoder.matches(req.getOldPassword(), member.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        member.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        memberRepository.save(member);
    }
}
