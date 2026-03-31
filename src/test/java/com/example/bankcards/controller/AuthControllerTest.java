package com.example.bankcards.controller;

import com.example.bankcards.TestSecurityConfig;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.dto.response.AccessTokenResponse;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void login_WhenValidCredentials_ShouldReturnAuthResponse() throws Exception {
        String email = "user@example.com";
        String password = "password123";
        LoginRequest request = new LoginRequest(email, password);

        AuthResponse authResponse = new AuthResponse(
                "access-token-123",
                "refresh-token-456",
                3600000L
        );

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.expiresIn").value(3600000L))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").isNumber());

        verify(authService).authenticate(any(LoginRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_WhenInvalidCredentials_ShouldReturnUnauthorizedStatus() throws Exception {
        String email = "user@example.com";
        String password = "wrongpassword";
        LoginRequest request = new LoginRequest(email, password);

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new AuthException("userEmail or password is incorrect"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("userEmail or password is incorrect"))
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isString());

        verify(authService).authenticate(any(LoginRequest.class));
    }

    @Test
    void login_WhenEmailIsInvalid_ShouldReturnBadRequestStatus() throws Exception {
        String invalidEmail = "invalid-userEmail";
        String password = "password123";
        LoginRequest request = new LoginRequest(invalidEmail, password);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"));

        verify(authService, never()).authenticate(any());
    }

    @Test
    void refresh_WhenValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        String refreshTokenValue = "valid-refresh-token-123";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

        AccessTokenResponse accessTokenResponse = new AccessTokenResponse("new-access-token-456");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(accessTokenResponse);

        mockMvc.perform(put("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token-456"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void refresh_WhenInvalidRefreshToken_ShouldReturnUnauthorizedStatus() throws Exception {
        String invalidRefreshToken = "invalid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(invalidRefreshToken);

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new AuthException("Invalid refresh token"));

        mockMvc.perform(put("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isString());

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void refresh_WhenRefreshTokenIsNull_ShouldReturnBadRequestStatus() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(null);

        mockMvc.perform(put("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"));

        verify(authService, never()).refreshToken(any());
    }
}
