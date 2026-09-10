package com.dems.security.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {

    private UUID userId;
    private String username;
    private Set<String> roles;
    private boolean authenticated;

    public static CurrentUser unauthenticated() {
        return CurrentUser.builder()
                .authenticated(false)
                .build();
    }

    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isInvestigator() {
        return hasRole("INVESTIGATOR");
    }

    public boolean isLegalOfficer() {
        return hasRole("LEGAL_OFFICER");
    }

    public boolean isViewer() {
        return hasRole("VIEWER");
    }

    public boolean canModifyCases() {
        return isAdmin() || isInvestigator();
    }

    public boolean canAccessCase(UUID createdByUserId) {
        if (isAdmin() || isLegalOfficer()) {
            return true;
        }
        return userId.equals(createdByUserId);
    }
}
