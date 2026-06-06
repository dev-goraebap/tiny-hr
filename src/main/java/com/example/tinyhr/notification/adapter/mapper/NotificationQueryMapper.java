package com.example.tinyhr.notification.adapter.mapper;

import com.example.tinyhr.notification.adapter.mapper.viewmodel.NotificationListItem;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 알림 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/notification/NotificationQueryMapper.xml. */
@Mapper
public interface NotificationQueryMapper {

    /** 본인 알림 목록(최신순). cursor(이전 페이지 마지막 createdAt) 이후를 limit 만큼. */
    List<NotificationListItem> listForRecipient(
            @Param("recipientId") String recipientId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursor") Instant cursor,
            @Param("limit") int limit);

    /** 본인 미읽음 알림 수. */
    long unreadCount(@Param("recipientId") String recipientId);
}
