package com.example.tinyhr.notification.domain.message;

import com.example.tinyhr.notification.domain.NotificationKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 메시지(내용) 애그리거트. 발행 후 변경되지 않는다(불변 컨텐츠).
 *
 * <p>수신자별 읽음 상태는 {@code NotificationDelivery} 가 따로 가진다.
 * 발화 시각 {@code issuedAt} 은 목록 정렬·커서 기준이다.
 */
@Entity
@Table(name = "notification_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationMessage {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationKind kind;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column
    private String link;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    /** 새 알림 메시지 발행. 식별자는 도메인이 발급한다. */
    public static NotificationMessage issue(
            NotificationKind kind, String title, String message, String link, Instant issuedAt) {
        NotificationMessage m = new NotificationMessage();
        m.eventId = UUID.randomUUID().toString();
        m.kind = kind;
        m.title = title;
        m.message = message;
        m.link = link;
        m.issuedAt = issuedAt;
        return m;
    }
}
