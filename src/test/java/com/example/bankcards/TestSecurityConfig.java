package com.example.bankcards;

import com.example.bankcards.util.JwtUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtUtils jwtUtils() {
        return mock(JwtUtils.class);
    }
}
