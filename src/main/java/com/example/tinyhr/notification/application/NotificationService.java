package com.example.tinyhr.notification.application;

import com.example.tinyhr.notification.domain.NotificationErrorCode;
import com.example.tinyhr.notification.domain.NotificationKind;
import com.example.tinyhr.notification.domain.delivery.NotificationDelivery;
import com.example.tinyhr.notification.domain.delivery.NotificationDeliveryRepository;
import com.example.tinyhr.notification.domain.message.NotificationMessage;
import com.example.tinyhr.notification.domain.message.NotificationMessageRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인앱 알림을 발행하고 읽음 상태를 처리한다.
 *
 * <p>발행은 메시지 1행 + 수신자 N행을 한 트랜잭션으로 저장한다. BC 외부에서의 발행은
 * {@code NotificationOpenHostService} 를 통한다.
 *
 * @actor 시스템, 일반사원
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationMessageRepository messageRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    public NotificationService(
            NotificationMessageRepository messageRepository,
            NotificationDeliveryRepository deliveryRepository) {
        this.messageRepository = messageRepository;
        this.deliveryRepository = deliveryRepository;
    }

    /** 알림을 발화한다. 수신자는 중복 제거하며, 비면 아무것도 하지 않는다. */
    public void create(
            Collection<String> recipientIds,
            NotificationKind kind,
            String title,
            String message,
            String link) {
        Set<String> recipients = new LinkedHashSet<>();
        if (recipientIds != null) {
            recipientIds.stream().filter(Objects::nonNull).forEach(recipients::add);
        }
        if (recipients.isEmpty()) {
            return;
        }

        NotificationMessage msg =
                NotificationMessage.issue(kind, title, message, link, Instant.now());
        messageRepository.save(msg);

        List<NotificationDelivery> deliveries = recipients.stream()
                .map(rid -> NotificationDelivery.dispatch(msg.getEventId(), rid))
                .toList();
        deliveryRepository.saveAll(deliveries);
    }

    /** 본인의 단일 알림을 읽음 처리한다(본인 전달만 조회되므로 소유자 검증은 암묵적). */
    public void markRead(String eventId, String recipientId) {
        NotificationDelivery delivery = deliveryRepository
                .findByEventIdAndRecipientId(eventId, recipientId)
                .orElseThrow(() ->
                        new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        delivery.markRead(Instant.now());
        deliveryRepository.save(delivery);
    }

    /** 본인의 모든 미읽음 알림을 일괄 읽음 처리하고 변경 건수를 돌려준다. */
    public int markAllRead(String recipientId) {
        return deliveryRepository.markAllReadForRecipient(recipientId, Instant.now());
    }
}
