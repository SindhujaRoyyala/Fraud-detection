package com.dems.user.service;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.security.model.Role;
import com.dems.security.service.PasswordEncoderService;
import com.dems.user.dto.ChangePasswordRequest;
import com.dems.user.dto.UpdateRoleRequest;
import com.dems.user.dto.UpdateUserRequest;
import com.dems.user.dto.UserResponse;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dems.auth.service.AuthService.toUserResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserContext currentUserContext;
    private final PasswordEncoderService passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, Role role, Pageable pageable) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.isAdmin()) {
            throw new ApiException("Only administrators can view all users", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        return userRepository.findAll(pageable).map(UserService::toDto);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.isAdmin() && !currentUser.getUserId().equals(userId)) {
            throw new ApiException("You can only view your own profile", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return toDto(user);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.isAdmin() && !currentUser.getUserId().equals(userId)) {
            throw new ApiException("You can only update your own profile", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ApiException("Email is already in use", HttpStatus.CONFLICT, "EMAIL_TAKEN");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (currentUser.isAdmin()) {
            if (request.getActive() != null) {
                user.setActive(request.getActive());
                if (!Boolean.TRUE.equals(request.getActive())) {
                    auditService.log(AuditEvent.USER_DEACTIVATED, userId, user.getUsername(),
                            null, null, null,
                            String.format("User %s deactivated by admin %s", user.getUsername(), currentUser.getUsername()),
                            null);
                }
            }
            if (request.getLocked() != null) {
                user.setLocked(request.getLocked());
            }
        }

        User saved = userRepository.save(user);

        auditService.log(AuditEvent.USER_UPDATED, userId, user.getUsername(),
                null, null, null,
                String.format("User profile updated for %s", user.getUsername()),
                null);

        return toDto(saved);
    }

    @Transactional
    public UserResponse updateUserRole(UUID userId, UpdateRoleRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.isAdmin()) {
            throw new ApiException("Only administrators can update user roles", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Role oldRole = user.getRole();
        user.setRole(request.getRole());
        User saved = userRepository.save(user);

        auditService.log(AuditEvent.USER_ROLE_CHANGED, userId, user.getUsername(),
                null, null, null,
                String.format("User %s role changed from %s to %s by admin %s",
                        user.getUsername(), oldRole, request.getRole(), currentUser.getUsername()),
                null);

        return toDto(saved);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getUserId().toString()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST, "INVALID_PASSWORD");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditService.log(AuditEvent.USER_PASSWORD_CHANGED, user.getId(), user.getUsername(),
                null, null, null,
                String.format("Password changed for user %s", user.getUsername()),
                null);
    }

    public static UserResponse toDto(User user) {
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
}
