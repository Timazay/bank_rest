package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.Random;

@Component
public class CardUtils {

    @Value("${encryption.secret-key}")
    private String secretKey;

    public String encrypt(String data) {
        try {
            return Base64.getEncoder().encodeToString(
                    data.getBytes()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data");
        }
    }

    public String decrypt(String encryptedData) {
        try {
            return new String(
                    Base64.getDecoder().decode(encryptedData)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data");
        }
    }

    public String generateCardNumber() {
        return String.format("%04d-%04d-%04d-%04d",
                new Random().nextInt(10000),
                new Random().nextInt(10000),
                new Random().nextInt(10000),
                new Random().nextInt(10000)
        );
    }

    public String maskCardNumber(String cardNumber) {
        String[] parts = cardNumber.split("-");
        if (parts.length == 4) {
            return "**** **** **** " + parts[3];
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public LocalDate calculateExpiryDate(int month, int year) {
        return LocalDate.of(year, month, 1)
                .withDayOfMonth(YearMonth.of(year, month).lengthOfMonth());
    }
}
