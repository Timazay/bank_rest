package com.example.bankcards.controller;

import com.example.bankcards.TestSecurityConfig;
import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.FindAllUsersRequest;
import com.example.bankcards.dto.response.CreateUserResponse;
import com.example.bankcards.dto.response.FindUserResponse;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.service.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(AdminUserController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "ADMIN")
public class AdminUserControllerTest {

    @MockBean
    private AdminUserService adminUserService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUserById_WhenUserExists_ThenReturnOkAndFindUserResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        FindUserResponse response = FindUserResponse.builder()
                .email("user@example.com")
                .fullName("John Doe")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .roleNames(List.of(UserRole.USER, UserRole.ADMIN))
                .build();

        when(adminUserService.findUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/users/{userId}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roleNames").isArray())
                .andExpect(jsonPath("$.roleNames.length()").value(2))
                .andExpect(jsonPath("$.roleNames[0]").value("USER"))
                .andExpect(jsonPath("$.roleNames[1]").value("ADMIN"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getUserById_WhenUserNotFound_ThenReturnNotFoundWithErrorResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        when(adminUserService.findUserById(userId))
                .thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/admin/users/{userId}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void createUser_WhenValidRequest_ThenReturnCreatedAndCreateUserResponse() throws Exception {
        String email = "user@example.com";
        String password = "Password123!";
        String fullName = "John Doe";

        CreateUserRequest request = CreateUserRequest.builder()
                .email(email)
                .password(password)
                .fullName(fullName)
                .build();

        UUID createdUserId = UUID.fromString("7cfdf440-e206-4b4c-bbf1-b9c741306cce");
        CreateUserResponse response = new CreateUserResponse(createdUserId);

        when(adminUserService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(createdUserId.toString()));
    }

    @Test
    void createUser_WhenEmailIsInvalid_ThenReturnBadRequest() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("invalid-email")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void blockUser_WhenUserExists_ThenReturnNoContent() throws Exception {
        UUID userId = UUID.randomUUID();

        doNothing().when(adminUserService).blockUser(userId);

        mockMvc.perform(put("/api/v1/admin/users/{userId}/block", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void blockUser_WhenUserNotFound_ThenReturnNotFoundWithErrorResponse() throws Exception {
        UUID userId = UUID.fromString("7cfdf440-e206-4b4c-bbf1-b9c741306cce");

        doThrow(new NotFoundException("User not found")).when(adminUserService).blockUser(userId);

        mockMvc.perform(put("/api/v1/admin/users/{userId}/block", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("404 NOT FOUND"))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void findAllUsers_WhenValidRequest_ThenReturnOkAndPageOfUsers() throws Exception {
        int page = 0;
        int size = 10;
        String search = "john";
        UserRole role = UserRole.USER;
        Boolean isEnabled = true;

        LocalDateTime now = LocalDateTime.now();

        FindUserResponse user1 = FindUserResponse.builder()
                .email("john.doe@example.com")
                .fullName("John Doe")
                .enabled(true)
                .createdAt(now)
                .roleNames(List.of(UserRole.USER))
                .build();

        FindUserResponse user2 = FindUserResponse.builder()
                .email("john.smith@example.com")
                .fullName("John Smith")
                .enabled(true)
                .createdAt(now.minusDays(1))
                .roleNames(List.of(UserRole.USER, UserRole.ADMIN))
                .build();

        List<FindUserResponse> users = List.of(user1, user2);
        Page<FindUserResponse> userPage = new PageImpl<>(users, PageRequest.of(page, size), 2);

        when(adminUserService.findAll(any(FindAllUsersRequest.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("search", search)
                        .param("role", role.name())
                        .param("isEnabled", String.valueOf(isEnabled))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10))

                .andExpect(jsonPath("$.content[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.content[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$.content[0].enabled").value(true))
                .andExpect(jsonPath("$.content[0].roleNames").isArray())
                .andExpect(jsonPath("$.content[0].roleNames.length()").value(1))
                .andExpect(jsonPath("$.content[0].roleNames[0]").value("USER"))
                .andExpect(jsonPath("$.content[0].createdAt").exists())

                .andExpect(jsonPath("$.content[1].email").value("john.smith@example.com"))
                .andExpect(jsonPath("$.content[1].fullName").value("John Smith"))
                .andExpect(jsonPath("$.content[1].enabled").value(true))
                .andExpect(jsonPath("$.content[1].roleNames").isArray())
                .andExpect(jsonPath("$.content[1].roleNames.length()").value(2))
                .andExpect(jsonPath("$.content[1].roleNames[0]").value("USER"))
                .andExpect(jsonPath("$.content[1].roleNames[1]").value("ADMIN"));
    }

    @Test
    void findAllUsers_WhenSizeIsZero_ThenReturnBadRequest() throws Exception {
        int page = 0;
        int size = 0;

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("400 BAD REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }
}
