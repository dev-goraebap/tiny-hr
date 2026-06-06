package com.example.tinyhr.organization.adapter.mapper;

import com.example.tinyhr.organization.adapter.mapper.viewmodel.RankListItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 직급 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/organization/RankQueryMapper.xml. */
@Mapper
public interface RankQueryMapper {

    /** 관리자 화면용 직급 목록(활성 우선, 정렬순). */
    List<RankListItem> listForAdmin();
}
