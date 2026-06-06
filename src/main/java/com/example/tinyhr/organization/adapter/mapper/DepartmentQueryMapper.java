package com.example.tinyhr.organization.adapter.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 부서 읽기 전용 조회 매퍼(MyBatis). */
@Mapper
public interface DepartmentQueryMapper {

    /** 전체 부서 목록(depth, displayOrder 순). */
    List<DepartmentListItem> listAll();
}
