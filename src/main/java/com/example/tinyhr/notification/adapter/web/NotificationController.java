package com.example.tinyhr.notification.adapter.web;

import com.example.tinyhr.iam.adapter.security.AuthPrincipal;
import com.example.tinyhr.notification.adapter.mapper.NotificationQueryMapper;
import com.example.tinyhr.notification.adapter.mapper.viewmodel.NotificationListItem;
import com.example.tinyhr.notification.application.NotificationService;
import com.example.tinyhr.shared.kernel.ApiResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 알림 목록·읽음 처리 HTTP 진입점. {@code /me/**} 는 인증 필요(SecurityConfig).
 */
@RestController
@RequestMapping("/me/notifications")
public class NotificationController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final NotificationService notificationService;
    private final NotificationQueryMapper notificationQueryMapper;

    public NotificationController(
            NotificationService notificationService,
            NotificationQueryMapper notificationQueryMapper) {
        this.notificationService = notificationService;
        this.notificationQueryMapper = notificationQueryMapper;
    }

    @GetMapping
    public ApiResponse<List<NotificationListItem>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        int clamped = Math.max(1, Math.min(MAX_LIMIT, limit == 0 ? DEFAULT_LIMIT : limit));
        return ApiResponse.of(notificationQueryMapper.listForRecipient(
                principal.userAccountId(), unread, parseCursor(cursor), clamped));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.of(notificationQueryMapper.unreadCount(principal.userAccountId()));
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> readAll(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.of(notificationService.markAllRead(principal.userAccountId()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String id) {
        notificationService.markRead(id, principal.userAccountId());
    }

    /** ISO-8601 커서 파싱. 비었거나 형식이 틀리면 첫 페이지(null)로 본다. */
    private static Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
