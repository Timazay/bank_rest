package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.response.CreateUserResponse;
import com.example.bankcards.dto.response.FindUserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public CreateUserResponse createUser(CreateUserRequest request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .enabled(true)
                .roles(List.of(roleRepository.findByName(UserRole.USER)))
                .build();

       User savedUser = userRepository.save(user);

       return new CreateUserResponse(savedUser.getId());
    }

    public FindUserResponse findUserById(UUID userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return FindUserResponse.builder()
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .fullName(user.getFullName())
                .roleNames(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .enabled(user.getEnabled())
                .build();
    }

    public void blockUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setEnabled(false);

        userRepository.save(user);
    }
}
