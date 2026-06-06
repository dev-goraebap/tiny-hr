package com.example.tinyhr.notification.domain;

/**
 * 인앱 알림 종류(공개 언어). 다른 BC 가 알림을 발행할 때 분류로 사용한다.
 *
 * <p>휴가(LEAVE_*)·연장근무(OVERTIME_*)·공지(ANNOUNCEMENT_*) 이벤트 카탈로그를 그대로 둔다 —
 * 해당 BC 이식 시 메시지를 조립해 {@code NotificationOpenHostService.notify} 로 발행한다.
 */
public enum NotificationKind {
    LEAVE_SUBMITTED,
    LEAVE_APPROVED,
    LEAVE_REJECTED,
    LEAVE_CANCELLED,
    LEAVE_CC,
    OVERTIME_SUBMITTED,
    OVERTIME_APPROVED,
    OVERTIME_REJECTED,
    OVERTIME_CANCELLED,
    ANNOUNCEMENT_PUBLISHED
}
