package com.example.tinyhr.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.notification.domain.NotificationErrorCode;
import com.example.tinyhr.notification.domain.NotificationKind;
import com.example.tinyhr.notification.domain.delivery.NotificationDelivery;
import com.example.tinyhr.notification.domain.delivery.NotificationDeliveryRepository;
import com.example.tinyhr.notification.domain.message.NotificationMessage;
import com.example.tinyhr.notification.domain.message.NotificationMessageRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationMessageRepository messageRepository;
    @Mock NotificationDeliveryRepository deliveryRepository;

    @InjectMocks NotificationService notificationService;

    @Test
    @DisplayName("메시지 1건과 수신자별 전달 N건을 저장한다(중복 제거)")
    @SuppressWarnings("unchecked")
    void create() {
        notificationService.create(List.of("u1", "u2", "u1"),
                NotificationKind.LEAVE_APPROVED, "제목", "본문", "/link");

        then(messageRepository).should().save(any(NotificationMessage.class));
        ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.forClass(List.class);
        then(deliveryRepository).should().saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .extracting(NotificationDelivery::getRecipientId)
                .containsExactly("u1", "u2");
    }

    @Test
    @DisplayName("수신자가 없으면 아무것도 하지 않는다")
    void create_empty() {
        notificationService.create(List.of(), NotificationKind.LEAVE_CC, "t", "m", null);

        then(messageRepository).should(never()).save(any());
        then(deliveryRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("없는 알림은 읽음 처리할 수 없다")
    void markRead_notFound() {
        given(deliveryRepository.findByEventIdAndRecipientId("evt-1", "u1"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead("evt-1", "u1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 알림을 읽음 처리한다")
    void markRead() {
        NotificationDelivery delivery = NotificationDelivery.dispatch("evt-1", "u1");
        given(deliveryRepository.findByEventIdAndRecipientId("evt-1", "u1"))
                .willReturn(Optional.of(delivery));

        notificationService.markRead("evt-1", "u1");

        assertThat(delivery.isUnread()).isFalse();
        then(deliveryRepository).should().save(delivery);
    }

    @Test
    @DisplayName("일괄 읽음은 변경 건수를 돌려준다")
    void markAllRead() {
        given(deliveryRepository.markAllReadForRecipient(eq("u1"), any())).willReturn(3);

        assertThat(notificationService.markAllRead("u1")).isEqualTo(3);
    }
}
