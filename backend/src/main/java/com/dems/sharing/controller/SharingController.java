package com.dems.sharing.controller;

import com.dems.common.response.ApiResponse;
import com.dems.sharing.dto.CreateShareRequest;
import com.dems.sharing.dto.PublicSharedDocumentResponse;
import com.dems.sharing.dto.SharedLinkResponse;
import com.dems.sharing.service.SharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Sharing", description = "Secure document sharing with expiration, access codes, and view limits")
public class SharingController {

    private final SharingService sharingService;

    @PostMapping("/sharing")
    @Operation(summary = "Create share link", description = "Create time-limited secure share link (with optional access code)")
    public ResponseEntity<ApiResponse<SharedLinkResponse>> createShareLink(
            @Valid @RequestBody CreateShareRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Share link created", sharingService.createShareLink(request)));
    }

    @GetMapping("/sharing")
    @Operation(summary = "List my share links", description = "Paginated list of share links created by current user")
    public ResponseEntity<ApiResponse<Page<SharedLinkResponse>>> getMyShareLinks(
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(sharingService.getMyShareLinks(activeOnly, pageable)));
    }

    @PostMapping("/sharing/{shareId}/revoke")
    @Operation(summary = "Revoke share link", description = "Immediately revoke a share link")
    public ResponseEntity<ApiResponse<Void>> revokeShareLink(@PathVariable UUID shareId) {
        sharingService.revokeShareLink(shareId);
        return ResponseEntity.ok(ApiResponse.ok("Share link revoked", null));
    }

    @GetMapping("/sharing/public/{token}")
    @Operation(summary = "View shared document (public)", description = "Public endpoint to view a shared document via token")
    public ResponseEntity<ApiResponse<PublicSharedDocumentResponse>> getPublicShared(
            @PathVariable String token,
            @RequestHeader(value = "X-Share-Code", required = false) String accessCode,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sharingService.getPublicSharedDocument(token, accessCode, request)));
    }

    @GetMapping("/sharing/public/{token}/download")
    @Operation(summary = "Download shared document (public)", description = "Public download of shared document if allowed")
    public ResponseEntity<Resource> downloadPublicShared(
            @PathVariable String token,
            @RequestHeader(value = "X-Share-Code", required = false) String accessCode,
            HttpServletRequest request) {
        return sharingService.downloadPublicShared(token, accessCode, request);
    }
}
