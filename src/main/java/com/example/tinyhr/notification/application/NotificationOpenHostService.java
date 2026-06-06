package com.example.tinyhr.notification.application;

import com.example.tinyhr.notification.domain.NotificationKind;
import java.util.Collection;
import org.springframework.stereotype.Service;

/**
 * notification 컨텍스트가 외부에 공개하는 Open Host Service.
 *
 * <p>다른 BC(approval·vacation·announcement 등)가 도메인 행위 직후 이 한 토큰으로 인앱 알림을
 * 발행한다. 메시지 제목·본문·링크 조립은 호출하는 BC 의 책임이며, 여기서는 발행만 담당한다.
 *
 * <p>MVP 범위: 인앱 알림만. Web Push fan-out 은 후속(별도 어댑터)로 미룬다.
 *
 * @actor 시스템
 */
@Service
public class NotificationOpenHostService {

    private final NotificationService notificationService;

    public NotificationOpenHostService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 지정한 수신자들에게 인앱 알림을 발행한다. */
    public void notify(
            Collection<String> recipientIds,
            NotificationKind kind,
            String title,
            String message,
            String link) {
        notificationService.create(recipientIds, kind, title, message, link);
    }
}
