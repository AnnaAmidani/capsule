package com.capsule.user;

import com.capsule.shared.security.JwtService;
import com.capsule.user.dto.LoginRequest;
import com.capsule.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock StringRedisTemplate redis;
    @InjectMocks UserService userService;

    @Test
    void registerCreatesUserWithHashedPassword() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn(new BCryptPasswordEncoder().encode("password123"));

        var user = userService.register(new RegisterRequest("test@example.com", "password123"));

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("password123", user.getPasswordHash())).isTrue();
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("test@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static void setId(User user, UUID id) throws Exception {
        Field f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(user, id);
    }

    @Test
    void loginReturnsTokensForValidCredentials() throws Exception {
        var user = new User();
        setId(user, UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");

        var ops = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        var result = userService.login(new LoginRequest("test@example.com", "password123"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotNull();
    }

    @Test
    void loginThrowsForInvalidPassword() {
        var user = new User();
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest("x@example.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
