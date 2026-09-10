package com.dems.security.context;

import com.dems.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CurrentUserContext {

    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return CurrentUser.unauthenticated();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User user) {
            Set<String> roles = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .collect(Collectors.toSet());

            UUID userId = extractUserId(authentication);
            return CurrentUser.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .roles(roles)
                    .authenticated(true)
                    .build();
        }

        if (principal instanceof CurrentUser currentUser) {
            return currentUser;
        }

        return CurrentUser.unauthenticated();
    }

    private UUID extractUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof UUID userId) {
            return userId;
        }
        if (details instanceof String && !details.toString().isEmpty()) {
            try {
                return UUID.fromString(details.toString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public UUID requireCurrentUserId() {
        CurrentUser user = getCurrentUser();
        if (!user.isAuthenticated() || user.getUserId() == null) {
            throw new ApiException("User is not authenticated", HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED");
        }
        return user.getUserId();
    }

    public CurrentUser requireAuthenticatedUser() {
        CurrentUser user = getCurrentUser();
        if (!user.isAuthenticated()) {
            throw new ApiException("User is not authenticated", HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED");
        }
        return user;
    }
}
