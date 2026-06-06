package com.example.tinyhr.approval.adapter.persistence;

import com.example.tinyhr.approval.adapter.mapper.OrgDirectoryQueryMapper;
import com.example.tinyhr.approval.domain.template.OrgDirectoryReadRepository;
import org.springframework.stereotype.Component;

/**
 * {@link OrgDirectoryReadRepository} 구현 — organization 의 employee·department 테이블을 읽어
 * 존재 여부를 판정한다(읽기 측 프로젝션).
 */
@Component
public class OrgDirectoryReadAdapter implements OrgDirectoryReadRepository {

    private final OrgDirectoryQueryMapper mapper;

    public OrgDirectoryReadAdapter(OrgDirectoryQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean employeeExists(String employeeId) {
        return mapper.countEmployee(employeeId) > 0;
    }

    @Override
    public boolean departmentExists(String departmentId) {
        return mapper.countDepartment(departmentId) > 0;
    }
}
