package com.ecommerce.monolith.user.service;

import com.ecommerce.monolith.common.exception.BusinessException;
import com.ecommerce.monolith.common.exception.ErrorCode;
import com.ecommerce.monolith.user.dto.UserRequest;
import com.ecommerce.monolith.user.entity.User;
import com.ecommerce.monolith.user.repository.UserRepository;
import com.ecommerce.monolith.user.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        UserRequest.Register request = new UserRequest.Register();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setName("User");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void loginThrowsInvalidCredentialsWhenEmailNotFound() {
        UserRequest.Login request = new UserRequest.Login();
        request.setEmail("missing@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginThrowsInvalidCredentialsWhenPasswordMismatch() {
        UserRequest.Login request = new UserRequest.Login();
        request.setEmail("user@example.com");
        request.setPassword("wrongPassword");

        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded")
                .name("User")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginThrowsAccountDisabledWhenUserInactive() {
        UserRequest.Login request = new UserRequest.Login();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .password("encoded")
                .name("User")
                .status(User.UserStatus.SUSPENDED)
                .role(User.UserRole.USER)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void registerEncodesPasswordAndPersistsUser() {
        UserRequest.Register request = new UserRequest.Register();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setName("User");
        request.setPhoneNumber("010-1234-5678");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = userService.register(request);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }
}
