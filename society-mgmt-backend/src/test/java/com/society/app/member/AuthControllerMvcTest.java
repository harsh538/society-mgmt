package com.society.app.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.app.config.SecurityConfig;
import com.society.app.member.dto.LoginRequest;
import com.society.app.security.CustomUserDetailsService;
import com.society.app.security.JwtAuthFilter;
import com.society.app.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice tests for the login endpoint.
 * Validates HTTP contract: body validation returns 400 with fieldErrors,
 * bad credentials return 401 with the ApiResponse envelope.
 *
 * <p>Imports the real {@link SecurityConfig} so the {@code /auth/login} permit-all
 * rule is active. The {@link JwtAuthFilter} mock is configured to pass through so
 * the request reaches the controller.</p>
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class AuthControllerMvcTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean AuthService authService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void configureFilterPassThrough() throws Exception {
        // JwtAuthFilter is a OncePerRequestFilter mock — its doFilter() does nothing by default,
        // which stops the filter chain. Configure it to delegate to the next filter.
        lenient().doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter((HttpServletRequest) inv.getArgument(0),
                               (HttpServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void login_returns400_whenPhoneIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void login_returns400_whenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("9999999999", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void login_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("9999999999", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
