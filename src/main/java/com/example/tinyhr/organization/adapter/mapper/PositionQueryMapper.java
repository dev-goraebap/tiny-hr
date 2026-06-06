package com.example.tinyhr.organization.adapter.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 직위 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/organization/PositionQueryMapper.xml. */
@Mapper
public interface PositionQueryMapper {

    /** 관리자 화면용 직위 목록(활성 우선, 정렬순). */
    List<PositionListItem> listForAdmin();
}
