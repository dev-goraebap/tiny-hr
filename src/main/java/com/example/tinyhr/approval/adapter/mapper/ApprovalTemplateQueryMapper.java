package com.example.tinyhr.approval.adapter.mapper;

import com.example.tinyhr.approval.adapter.mapper.viewmodel.ApprovalTemplateView;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 결재선 템플릿 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/approval/ApprovalTemplateQueryMapper.xml. */
@Mapper
public interface ApprovalTemplateQueryMapper {

    List<ApprovalTemplateView> listAll();

    List<ApprovalTemplateView> listByDepartment(@Param("departmentId") String departmentId);

    ApprovalTemplateView findById(@Param("approvalTemplateId") String approvalTemplateId);
}
