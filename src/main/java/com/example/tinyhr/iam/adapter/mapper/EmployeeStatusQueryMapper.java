package com.example.tinyhr.iam.adapter.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 로그인 게이트용 사원 상태 조회 매퍼(MyBatis, 읽기 전용).
 * SQL 은 resources/mapper/iam/EmployeeStatusQueryMapper.xml.
 */
@Mapper
public interface EmployeeStatusQueryMapper {

    /** 사원 상태 문자열(INVITED/ACTIVE/TERMINATED). 없으면 null. */
    String findStatus(@Param("employeeId") String employeeId);
}
