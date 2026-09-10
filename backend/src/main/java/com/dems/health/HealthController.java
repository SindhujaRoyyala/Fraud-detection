package com.dems.health;

import com.dems.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Application health check endpoints")
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping
    @Operation(summary = "Application health status", description = "Returns overall application health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", Instant.now());
        healthInfo.put("service", "dems-backend");
        healthInfo.put("version", "1.0.0");

        try {
            HealthComponent health = healthEndpoint.health();
            if (health.getStatus() == Status.UP) {
                healthInfo.put("dependencies", "OK");
            } else {
                healthInfo.put("dependencies", "DEGRADED");
            }
        } catch (Exception e) {
            healthInfo.put("dependencies", "UNKNOWN");
        }

        return ResponseEntity.ok(ApiResponse.ok("Health check successful", healthInfo));
    }

    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe", description = "Kubernetes liveness probe endpoint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveness() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("live", true);
        data.put("timestamp", Instant.now());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe", description = "Kubernetes readiness probe endpoint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ready", true);
        data.put("timestamp", Instant.now());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
