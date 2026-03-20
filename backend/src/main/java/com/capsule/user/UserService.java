package com.capsule.user;

import com.capsule.shared.security.JwtService;
import com.capsule.user.dto.*;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final String REFRESH_TOKEN_KEY = "refresh:token:";
    private static final String REFRESH_USER_KEY = "refresh:user:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, StringRedisTemplate redis) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redis = redis;
    }

    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        var user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        return userRepository.save(user);
    }

    public TokenResponse login(LoginRequest req) {
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        var tokenKey = REFRESH_TOKEN_KEY + refreshToken;
        var userIdStr = redis.opsForValue().get(tokenKey);
        if (userIdStr == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        // Delete old token-keyed entry (rotation — old token is consumed)
        redis.delete(tokenKey);
        var userId = UUID.fromString(userIdStr);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        var tokenKey = REFRESH_TOKEN_KEY + refreshToken;
        var userIdStr = redis.opsForValue().get(tokenKey);
        redis.delete(tokenKey);
        if (userIdStr != null) {
            redis.delete(REFRESH_USER_KEY + userIdStr);
        }
    }

    public void invalidateRefreshTokenForUser(UUID userId) {
        var userKey = REFRESH_USER_KEY + userId.toString();
        var existingToken = redis.opsForValue().get(userKey);
        redis.delete(userKey);
        if (existingToken != null) {
            redis.delete(REFRESH_TOKEN_KEY + existingToken);
        }
    }

    public User upsertOAuthUser(String provider, String subject, String email) {
        return userRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> {
                    var user = new User();
                    user.setEmail(email);
                    user.setOauthProvider(provider);
                    user.setOauthSubject(subject);
                    return userRepository.save(user);
                });
    }

    public TokenResponse issueTokens(User user) {
        var correlationId = Objects.requireNonNullElse(MDC.get("correlationId"), "");
        var accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getTier().name(), correlationId);
        var refreshToken = UUID.randomUUID().toString();
        // Dual mapping: by token (for validation) and by userId (for invalidation)
        redis.opsForValue().set(REFRESH_TOKEN_KEY + refreshToken, user.getId().toString(), REFRESH_TTL);
        redis.opsForValue().set(REFRESH_USER_KEY + user.getId().toString(), refreshToken, REFRESH_TTL);
        return new TokenResponse(accessToken, refreshToken);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateEmail(UUID id, String newEmail) {
        var user = findById(id);
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
}
