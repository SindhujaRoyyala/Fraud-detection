package com.dems.dashboard.controller;

import com.dems.common.response.ApiResponse;
import com.dems.dashboard.dto.DashboardStats;
import com.dems.dashboard.dto.RelationshipGraph;
import com.dems.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Analytics, statistics, and relationship graph APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics", description = "Role-scoped analytics and key metrics for dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardStats()));
    }

    @GetMapping("/cases/{caseId}/graph")
    @Operation(summary = "Case relationship graph (React Flow)", description = "Nodes and edges for React Flow visualization of a case")
    public ResponseEntity<ApiResponse<RelationshipGraph>> getCaseRelationshipGraph(@PathVariable UUID caseId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCaseRelationshipGraph(caseId)));
    }
}
