package com.example.tinyhr.iam.adapter.mapper;

import com.example.tinyhr.iam.adapter.mapper.viewmodel.RoleAssignmentListItem;
import com.example.tinyhr.iam.adapter.mapper.viewmodel.RoleAssignmentMember;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 역할 부여 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/iam/RoleAssignmentQueryMapper.xml. */
@Mapper
public interface RoleAssignmentQueryMapper {

    /** 특정 사원의 부여 목록(회수 포함, 감사용). */
    List<RoleAssignmentListItem> listByUser(@Param("userAccountId") String userAccountId);

    /** 특정 역할을 보유한 활성 사원 목록(표시용 employee 조인). */
    List<RoleAssignmentMember> listMembersByRole(@Param("roleId") String roleId);
}
