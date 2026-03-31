package com.example.bankcards.service;

import com.example.bankcards.dto.response.AccessTokenResponse;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.TokenType;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findUserByEmail(request.email())
                .orElseThrow(() -> new AuthException("email or password is incorrect"));

        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = jwtUtils.generateRefreshToken(user);

        return new AuthResponse(accessToken,
                refreshToken.getToken(),
                jwtUtils.extractExpiration(accessToken, TokenType.ACCESS).getTime());
    }

    public AccessTokenResponse refreshToken(RefreshTokenRequest request) {
      RefreshToken refreshToken = jwtUtils.verifyExistingRefreshToken(request.refreshToken(), TokenType.REFRESH);
      return new AccessTokenResponse(jwtUtils.generateAccessToken(refreshToken.getUser()));
    }
}
