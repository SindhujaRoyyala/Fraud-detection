package com.dems.security;

import com.dems.security.context.CurrentUser;
import com.dems.security.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RbacTest {

    @Test
    @DisplayName("CurrentUser: role checks work correctly")
    void currentUser_RoleChecks() {
        CurrentUser admin = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .username("admin")
                .roles(Set.of(Role.ADMIN.name()))
                .authenticated(true)
                .build();

        CurrentUser investigator = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .username("inv")
                .roles(Set.of(Role.INVESTIGATOR.name()))
                .authenticated(true)
                .build();

        CurrentUser legal = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .username("legal")
                .roles(Set.of(Role.LEGAL_OFFICER.name()))
                .authenticated(true)
                .build();

        CurrentUser viewer = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .username("viewer")
                .roles(Set.of(Role.VIEWER.name()))
                .authenticated(true)
                .build();

        CurrentUser guest = CurrentUser.unauthenticated();

        assertTrue(admin.isAdmin());
        assertFalse(investigator.isAdmin());
        assertFalse(guest.isAuthenticated());

        assertTrue(admin.canModifyCases());
        assertTrue(investigator.canModifyCases());
        assertFalse(legal.canModifyCases());
        assertFalse(viewer.canModifyCases());

        UUID creatorId = investigator.getUserId();
        assertTrue(admin.canAccessCase(creatorId));
        assertTrue(legal.canAccessCase(creatorId));
        assertTrue(investigator.canAccessCase(creatorId));

        UUID otherId = UUID.randomUUID();
        assertTrue(admin.canAccessCase(otherId));
        assertTrue(legal.canAccessCase(otherId));
        assertFalse(viewer.canAccessCase(otherId));
    }

    @Test
    @DisplayName("CurrentUser: hasRole with null roles returns false")
    void currentUser_HasRole_NullRoles() {
        CurrentUser u = CurrentUser.builder().roles(null).authenticated(true).build();
        assertFalse(u.hasRole("ADMIN"));
        assertFalse(u.isAdmin());
    }

    @Test
    @DisplayName("CurrentUser: unauthenticated user checks")
    void currentUser_Unauthenticated_Defaults() {
        CurrentUser guest = CurrentUser.unauthenticated();

        assertFalse(guest.isAuthenticated());
        assertNull(guest.getUserId());
        assertFalse(guest.isAdmin());
        assertFalse(guest.canModifyCases());
    }
}
