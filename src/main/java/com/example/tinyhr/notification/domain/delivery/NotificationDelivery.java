package com.example.tinyhr.notification.domain.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 전달 — 메시지 × 수신자 1건. 수신자별 읽음 상태를 가진다.
 *
 * <p>식별은 surrogate UUID 로 하되 {@code (eventId, recipientId)} 는 유일하다(같은 메시지를 같은
 * 사람에게 한 번만 전달).
 */
@Entity
@Table(name = "notification_delivery",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "recipient_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDelivery {

    @Id
    @Column(name = "delivery_id", length = 36)
    private String id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    /** 읽은 시각. null = 미읽음. */
    @Column(name = "read_at")
    private Instant readAt;

    /** 메시지를 수신자에게 전달. */
    public static NotificationDelivery dispatch(String eventId, String recipientId) {
        NotificationDelivery d = new NotificationDelivery();
        d.id = UUID.randomUUID().toString();
        d.eventId = eventId;
        d.recipientId = recipientId;
        d.readAt = null;
        return d;
    }

    /** 읽음 처리. 이미 읽었으면 멱등(변경 없음). */
    public void markRead(Instant now) {
        if (readAt != null) {
            return;
        }
        this.readAt = now;
    }

    public boolean isUnread() {
        return readAt == null;
    }
}
