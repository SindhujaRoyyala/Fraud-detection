package com.dems.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthEndpoint healthEndpoint;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController(healthEndpoint);
    }

    @Test
    @DisplayName("Health endpoint returns UP status")
    void checkHealth_returnsUp() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        ResponseEntity<com.dems.common.response.ApiResponse<Map<String, Object>>> response =
                healthController.checkHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        Map<String, Object> data = response.getBody().getData();
        assertNotNull(data);
        assertEquals("UP", data.get("status"));
        assertEquals("dems-backend", data.get("service"));
        assertEquals("1.0.0", data.get("version"));
        assertNotNull(data.get("timestamp"));
    }

    @Test
    @DisplayName("Health endpoint returns DEPENDENCIES DEGRADED on DOWN")
    void checkHealth_degradedDependencies() {
        when(healthEndpoint.health()).thenReturn(Health.down().build());

        ResponseEntity<com.dems.common.response.ApiResponse<Map<String, Object>>> response =
                healthController.checkHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DEGRADED", response.getBody().getData().get("dependencies"));
    }

    @Test
    @DisplayName("Liveness probe returns OK")
    void liveness_returnsLive() {
        var response = healthController.liveness();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().getData().get("live"));
    }

    @Test
    @DisplayName("Readiness probe returns OK")
    void readiness_returnsReady() {
        var response = healthController.readiness();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().getData().get("ready"));
    }
}
