package com.example.tinyhr.approval.adapter.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 결재선 검증용 organization 존재 여부 조회 매퍼(MyBatis, 읽기 전용).
 * SQL 은 resources/mapper/approval/OrgDirectoryQueryMapper.xml.
 */
@Mapper
public interface OrgDirectoryQueryMapper {

    long countEmployee(@Param("employeeId") String employeeId);

    long countDepartment(@Param("departmentId") String departmentId);
}
