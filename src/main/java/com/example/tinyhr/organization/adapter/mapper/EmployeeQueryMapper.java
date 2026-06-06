package com.example.tinyhr.organization.adapter.mapper;

import com.example.tinyhr.organization.adapter.mapper.viewmodel.EmployeeDetailView;
import com.example.tinyhr.organization.adapter.mapper.viewmodel.EmployeeListItem;
import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 사원 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/organization/EmployeeQueryMapper.xml. */
@Mapper
public interface EmployeeQueryMapper {

    /** 관리자 사원 목록(필터·정렬·페이징). departmentId 가 "NONE" 이면 미배정만. */
    List<EmployeeListItem> listForAdmin(
            @Param("status") EmployeeStatus status,
            @Param("departmentId") String departmentId,
            @Param("rankId") String rankId,
            @Param("q") String q,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /** 동일 필터의 전체 건수(페이징 메타). */
    long countForAdmin(
            @Param("status") EmployeeStatus status,
            @Param("departmentId") String departmentId,
            @Param("rankId") String rankId,
            @Param("q") String q);

    /** 사원 단건 상세. 없으면 null. */
    EmployeeDetailView findDetail(@Param("employeeId") String employeeId);
}
