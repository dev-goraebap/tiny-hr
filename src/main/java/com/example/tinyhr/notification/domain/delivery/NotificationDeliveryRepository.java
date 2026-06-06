package com.example.tinyhr.notification.domain.delivery;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 알림 전달 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {

    Optional<NotificationDelivery> findByEventIdAndRecipientId(String eventId, String recipientId);

    /** 수신자의 모든 미읽음 전달을 일괄 읽음 처리하고 변경 건수를 돌려준다. */
    @Modifying
    @Query("update NotificationDelivery d set d.readAt = :now "
            + "where d.recipientId = :recipientId and d.readAt is null")
    int markAllReadForRecipient(@Param("recipientId") String recipientId, @Param("now") Instant now);
}
