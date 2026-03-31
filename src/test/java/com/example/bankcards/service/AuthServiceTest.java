package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.dto.response.AccessTokenResponse;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.TokenType;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticate_WhenValidCredentials_ShouldReturnAuthResponse() {
        String email = "user@example.com";
        String password = "password123";
        LoginRequest request = new LoginRequest(email, password);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("encodedPassword")
                .fullName("FullName")
                .roles(List.of(new Role(UUID.randomUUID(), UserRole.USER)))
                .build();

        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-123")
                .user(user)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        Date date = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtils.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtUtils.generateRefreshToken(user)).thenReturn(refreshToken);
        when(jwtUtils.extractExpiration(accessToken, TokenType.ACCESS)).thenReturn(date);

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken.getToken(), response.refreshToken());
        assertEquals(date.getTime(), response.expiresIn());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findUserByEmail(email);
        verify(jwtUtils).generateAccessToken(user);
        verify(jwtUtils).generateRefreshToken(user);
        verify(jwtUtils).extractExpiration(accessToken, TokenType.ACCESS);
    }

    @Test
    void authenticate_WhenUserNotFoundAfterAuth_ShouldThrowAuthException() {
        String email = "user@example.com";
        String password = "password123";
        LoginRequest request = new LoginRequest(email, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.authenticate(request));

        assertEquals("email or password is incorrect", exception.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findUserByEmail(email);
        verify(jwtUtils, never()).generateAccessToken(any());
        verify(jwtUtils, never()).generateRefreshToken(any());
    }

    @Test
    void refreshToken_WhenValidRefreshToken_ShouldReturnNewAccessToken() {
        String refreshTokenValue = "valid-refresh-token-123";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshTokenValue);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .fullName("John Doe")
                .roles(List.of(new Role(UUID.randomUUID(), UserRole.USER)))
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        String newAccessToken = "new-generated-access-token-xyz";

        when(jwtUtils.verifyExistingRefreshToken(request.refreshToken(), TokenType.REFRESH))
                .thenReturn(refreshToken);
        when(jwtUtils.generateAccessToken(user)).thenReturn(newAccessToken);

        AccessTokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals(newAccessToken, response.accessToken());

        verify(jwtUtils).verifyExistingRefreshToken(request.refreshToken(), TokenType.REFRESH);
        verify(jwtUtils).generateAccessToken(user);
    }
}
