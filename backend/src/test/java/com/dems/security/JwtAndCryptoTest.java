package com.dems.security;

import com.dems.security.model.Role;
import com.dems.security.service.JwtTokenService;
import com.dems.security.service.PasswordEncoderService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class JwtAndCryptoTest {

    private JwtTokenService jwtTokenService;
    private PasswordEncoderService passwordEncoderService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        jwtTokenService = new JwtTokenService(
                "test-secret-key-must-be-at-least-32-characters-long-enough-yes-indeed",
                300000L, 86400000L, "dems-test", redisTemplate
        );
        passwordEncoderService = new PasswordEncoderService();
    }

    @Test
    @DisplayName("Argon2id password hashing: encode and match")
    void argon2id_EncodeAndMatch_Success() {
        String raw = "MyStr0ng!P@ssword";

        String hash = passwordEncoderService.encode(raw);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(passwordEncoderService.matches(raw, hash));
    }

    @Test
    @DisplayName("Argon2id: wrong password does not match")
    void argon2id_WrongPassword_DoesNotMatch() {
        String hash = passwordEncoderService.encode("correct");

        assertFalse(passwordEncoderService.matches("wrong", hash));
        assertFalse(passwordEncoderService.matches("", hash));
        assertFalse(passwordEncoderService.matches(null, hash));
    }

    @Test
    @DisplayName("Argon2id: each hash is unique (salted)")
    void argon2id_DifferentHashes_SamePassword() {
        String pwd = "samepassword";
        String h1 = passwordEncoderService.encode(pwd);
        String h2 = passwordEncoderService.encode(pwd);

        assertNotEquals(h1, h2);
        assertTrue(passwordEncoderService.matches(pwd, h1));
        assertTrue(passwordEncoderService.matches(pwd, h2));
    }

    @Test
    @DisplayName("JWT: generate and validate access token")
    void jwt_GenerateAndValidateAccessToken_Success() {
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of(Role.ADMIN.name(), Role.INVESTIGATOR.name());

        String token = jwtTokenService.generateAccessToken(userId, "adminuser", roles);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtTokenService.validateAndParseToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("ACCESS", claims.get("type"));
        assertEquals("adminuser", claims.get("username"));
        assertEquals("dems-test", claims.getIssuer());
    }

    @Test
    @DisplayName("JWT: roles correctly extracted from claims")
    void jwt_RolesExtractedFromClaims() {
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of(Role.LEGAL_OFFICER.name(), Role.VIEWER.name());

        String token = jwtTokenService.generateAccessToken(userId, "legal", roles);
        Claims claims = jwtTokenService.validateAndParseToken(token);

        Set<String> parsed = jwtTokenService.getRolesFromClaims(claims);
        assertEquals(2, parsed.size());
        assertTrue(parsed.contains(Role.LEGAL_OFFICER.name()));
        assertTrue(parsed.contains(Role.VIEWER.name()));
    }

    @Test
    @DisplayName("JWT: tampered token fails validation")
    void jwt_TamperedToken_FailsValidation() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.generateAccessToken(userId, "u", Set.of(Role.VIEWER.name()));

        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThrows(Exception.class, () -> jwtTokenService.validateAndParseToken(tampered));
    }

    @Test
    @DisplayName("Role enum fromString: valid values")
    void roleEnum_FromString_ValidValues() {
        assertEquals(Role.ADMIN, Role.fromString("admin"));
        assertEquals(Role.INVESTIGATOR, Role.fromString("INVESTIGATOR"));
        assertEquals(Role.LEGAL_OFFICER, Role.fromString("legal_officer"));
        assertEquals(Role.VIEWER, Role.fromString("Viewer"));
    }

    @Test
    @DisplayName("Role enum fromString: invalid values throw")
    void roleEnum_FromString_InvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("SUPERUSER"));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString(""));
    }
}
