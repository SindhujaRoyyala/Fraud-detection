package com.dems.notification.controller;

import com.dems.common.response.ApiResponse;
import com.dems.notification.dto.NotificationResponse;
import com.dems.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification and alert APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List notifications", description = "Paginated list of current user's notifications")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @RequestParam(required = false) Boolean read,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getMyNotifications(read, pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread count", description = "Count of unread notifications for current user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUnreadCount()));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a specific notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all read", description = "Mark all current user's notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead() {
        int count = notificationService.markAllAsRead();
        Map<String, Object> result = Map.of("markedCount", count);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
