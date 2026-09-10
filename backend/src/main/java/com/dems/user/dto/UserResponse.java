package com.dems.user.dto;

import com.dems.security.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private Boolean active;
    private Boolean locked;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
