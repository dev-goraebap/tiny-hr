package com.example.tinyhr.notification.adapter.mapper.viewmodel;

import java.time.Instant;

/**
 * 본인 알림 목록 조회 뷰(읽기 전용). kind 는 문자열로 내려간다.
 */
public record NotificationListItem(
        String eventId,
        String kind,
        String title,
        String message,
        String link,
        boolean unread,
        Instant createdAt,
        Instant readAt) {}
