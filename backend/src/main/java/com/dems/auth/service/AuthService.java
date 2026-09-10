package com.dems.auth.service;

import com.dems.auth.dto.*;
import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.security.model.Role;
import com.dems.security.service.JwtTokenService;
import com.dems.security.service.PasswordEncoderService;
import com.dems.user.dto.UserResponse;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoderService passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    @Transactional
    public LoginResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException("Username is already taken", HttpStatus.CONFLICT, "USERNAME_TAKEN");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already registered", HttpStatus.CONFLICT, "EMAIL_TAKEN");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : Role.VIEWER)
                .active(true)
                .locked(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());

        auditService.log(AuditEvent.USER_REGISTERED, savedUser.getId(), savedUser.getUsername(),
                null, null, null,
                String.format("User %s registered with role %s", savedUser.getUsername(), savedUser.getRole()),
                null);

        return createLoginResponse(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElse(null);

        String ipAddress = getClientIp(httpRequest);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.log(AuditEvent.USER_LOGIN_FAILED,
                    user != null ? user.getId() : null,
                    request.getUsername(),
                    null, null, null,
                    String.format("Failed login attempt for user: %s from IP: %s", request.getUsername(), ipAddress),
                    null);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApiException("User account is disabled", HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new ApiException("User account is locked", HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception e) {
            auditService.log(AuditEvent.USER_LOGIN_FAILED, user.getId(), user.getUsername(),
                    null, null, null,
                    String.format("Authentication manager rejected login for: %s", user.getUsername()),
                    null);
            throw new BadCredentialsException("Invalid username or password");
        }

        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        auditService.log(AuditEvent.USER_LOGIN, user.getId(), user.getUsername(),
                null, null, null,
                String.format("User %s logged in from IP: %s", user.getUsername(), ipAddress),
                null);

        return createLoginResponse(user);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            Claims claims = jwtTokenService.validateAndParseToken(request.getRefreshToken());
            String tokenType = jwtTokenService.getTokenType(claims);
            if (!"REFRESH".equals(tokenType)) {
                throw new ApiException("Invalid token type", HttpStatus.BAD_REQUEST, "INVALID_TOKEN_TYPE");
            }

            UUID userId = jwtTokenService.getUserIdFromClaims(claims);
            String tokenId = jwtTokenService.getTokenId(claims);

            if (!jwtTokenService.validateRefreshToken(tokenId, userId)) {
                throw new ApiException("Refresh token has been revoked or expired", HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

            if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())) {
                throw new ApiException("User account is not active", HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
            }

            jwtTokenService.deleteRefreshToken(tokenId);

            Set<String> roles = Set.of(user.getRole().name());
            String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
            String newRefreshToken = jwtTokenService.generateRefreshToken(user.getId());

            auditService.log(AuditEvent.USER_TOKEN_REFRESHED, user.getId(), user.getUsername(),
                    null, null, null,
                    String.format("Token refreshed for user %s", user.getUsername()),
                    null);

            return TokenResponse.of(accessToken, newRefreshToken, jwtTokenService.getAccessTokenExpirationMs() / 1000);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }
    }

    @Transactional
    public void logout(String accessTokenHeader) {
        CurrentUser currentUser = currentUserContext.getCurrentUser();
        if (currentUser.isAuthenticated()) {
            String token = extractTokenFromHeader(accessTokenHeader);
            if (token != null) {
                try {
                    Claims claims = jwtTokenService.validateAndParseToken(token);
                    String tokenId = jwtTokenService.getTokenId(claims);
                    long expiryMs = jwtTokenService.getAccessTokenExpirationMs();
                    jwtTokenService.revokeAccessToken(tokenId, expiryMs);
                } catch (Exception ignored) {
                }
            }

            auditService.log(AuditEvent.USER_LOGOUT, currentUser.getUserId(), currentUser.getUsername(),
                    null, null, null,
                    String.format("User %s logged out", currentUser.getUsername()),
                    null);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getUserId().toString()));
        return toUserResponse(user);
    }

    private LoginResponse createLoginResponse(User user) {
        Set<String> roles = Set.of(user.getRole().name());
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        TokenResponse tokens = TokenResponse.of(
                accessToken, refreshToken, jwtTokenService.getAccessTokenExpirationMs() / 1000
        );

        return LoginResponse.builder()
                .tokens(tokens)
                .user(toUserResponse(user))
                .build();
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .locked(user.getLocked())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String extractTokenFromHeader(String bearerHeader) {
        if (StringUtils.hasText(bearerHeader) && bearerHeader.startsWith("Bearer ")) {
            return bearerHeader.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
