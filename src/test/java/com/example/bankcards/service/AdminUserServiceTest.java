package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.FindAllUsersRequest;
import com.example.bankcards.dto.response.CreateUserResponse;
import com.example.bankcards.dto.response.FindUserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void createUser_WhenValidRequest_ThenReturnCreateUserResponse() {
        String email = "user@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";
        String fullName = "John Doe";

        CreateUserRequest request = new CreateUserRequest(
                email,
                fullName,
                rawPassword,
                false
        );

        Role userRole = Role.builder()
                .id(UUID.randomUUID())
                .name(UserRole.USER)
                .build();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(encodedPassword)
                .fullName(fullName)
                .enabled(true)
                .roles(List.of(userRole))
                .build();

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(userRole);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        CreateUserResponse response = adminUserService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(savedUser.getId());

        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(roleRepository, times(1)).findByName(UserRole.USER);
        verify(userRepository, times(1)).save(any(User.class));

        verify(userRepository).save(argThat(user ->
                user.getEmail().equals(email) &&
                        user.getPassword().equals(encodedPassword) &&
                        user.getFullName().equals(fullName) &&
                        user.getEnabled() &&
                        user.getRoles().contains(userRole)
        ));
    }

    @Test
    void findUserById_WhenUserExists_ThenReturnFindUserResponse() {
        UUID userId = UUID.fromString("7cfdf440-e206-4b4c-bbf1-b9c741306cce");

        Role userRole = Role.builder()
                .id(UUID.randomUUID())
                .name(UserRole.USER)
                .build();

        Role adminRole = Role.builder()
                .id(UUID.randomUUID())
                .name(UserRole.ADMIN)
                .build();

        LocalDateTime createdAt = LocalDateTime.now();

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("John Doe")
                .enabled(true)
                .createdAt(createdAt)
                .roles(List.of(userRole, adminRole))
                .build();

        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));

        FindUserResponse response = adminUserService.findUserById(userId);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.enabled()).isTrue();
        assertThat(response.roleNames()).hasSize(2);
        assertThat(response.roleNames()).containsExactlyInAnyOrder(UserRole.USER, UserRole.ADMIN);

        verify(userRepository, times(1)).findUserById(userId);
    }

    @Test
    void findUserById_WhenUserNotFound_ThenThrowNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.findUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findUserById(userId);
    }

    @Test
    void blockUser_WhenUserExists_ThenDisableUserAndSave() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("John Doe")
                .enabled(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        adminUserService.blockUser(userId);

        assertThat(user.getEnabled()).isFalse();

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void blockUser_WhenUserNotFound_ThenThrowNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.blockUser(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findAll_WhenValidRequest_ThenReturnPageOfFindUserResponse() {
        FindAllUsersRequest request = FindAllUsersRequest.builder()
                .page(0)
                .size(10)
                .search("john")
                .isEnabled(true)
                .userRole(UserRole.USER)
                .build();

        Role userRole = Role.builder()
                .id(UUID.randomUUID())
                .name(UserRole.USER)
                .build();

        Role adminRole = Role.builder()
                .id(UUID.randomUUID())
                .name(UserRole.ADMIN)
                .build();

        LocalDateTime now = LocalDateTime.now();

        User user1 = User.builder()
                .id(UUID.randomUUID())
                .email("john.doe@example.com")
                .fullName("John Doe")
                .enabled(true)
                .createdAt(now)
                .roles(List.of(userRole))
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email("john.smith@example.com")
                .fullName("John Smith")
                .enabled(true)
                .createdAt(now.minusDays(1))
                .roles(List.of(userRole, adminRole))
                .build();

        List<User> users = List.of(user1, user2);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> usersPage = new PageImpl<>(users, pageable, 2);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(usersPage);

        Page<FindUserResponse> result = adminUserService.findAll(request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        FindUserResponse firstResponse = result.getContent().get(0);
        assertThat(firstResponse.email()).isEqualTo("john.doe@example.com");
        assertThat(firstResponse.fullName()).isEqualTo("John Doe");
        assertThat(firstResponse.enabled()).isTrue();
        assertThat(firstResponse.createdAt()).isEqualTo(now);
        assertThat(firstResponse.roleNames()).hasSize(1);
        assertThat(firstResponse.roleNames()).containsExactly(UserRole.USER);

        FindUserResponse secondResponse = result.getContent().get(1);
        assertThat(secondResponse.email()).isEqualTo("john.smith@example.com");
        assertThat(secondResponse.fullName()).isEqualTo("John Smith");
        assertThat(secondResponse.enabled()).isTrue();
        assertThat(secondResponse.createdAt()).isEqualTo(now.minusDays(1));
        assertThat(secondResponse.roleNames()).hasSize(2);
        assertThat(secondResponse.roleNames()).containsExactlyInAnyOrder(UserRole.USER, UserRole.ADMIN);

        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }
}
