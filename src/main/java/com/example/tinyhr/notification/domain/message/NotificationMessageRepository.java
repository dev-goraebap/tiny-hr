package com.example.tinyhr.notification.domain.message;

import org.springframework.data.jpa.repository.JpaRepository;

/** 알림 메시지 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, String> {}
