package com.example.tinyhr.notification.domain.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDeliveryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("전달 생성 시 미읽음 상태다")
    void dispatch() {
        NotificationDelivery delivery = NotificationDelivery.dispatch("evt-1", "user-1");

        assertThat(delivery.getId()).isNotBlank();
        assertThat(delivery.getEventId()).isEqualTo("evt-1");
        assertThat(delivery.getRecipientId()).isEqualTo("user-1");
        assertThat(delivery.isUnread()).isTrue();
    }

    @Test
    @DisplayName("읽음 처리하면 읽은 시각이 남는다")
    void markRead() {
        NotificationDelivery delivery = NotificationDelivery.dispatch("evt-1", "user-1");

        delivery.markRead(T0);

        assertThat(delivery.isUnread()).isFalse();
        assertThat(delivery.getReadAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("읽음 처리는 멱등이다")
    void markReadIdempotent() {
        NotificationDelivery delivery = NotificationDelivery.dispatch("evt-1", "user-1");

        delivery.markRead(T0);
        delivery.markRead(T0.plusSeconds(60));

        assertThat(delivery.getReadAt()).isEqualTo(T0);
    }
}
