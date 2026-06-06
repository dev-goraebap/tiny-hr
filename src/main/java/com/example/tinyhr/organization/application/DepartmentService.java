package com.example.tinyhr.organization.application;

import com.example.tinyhr.organization.application.dto.CreateDepartmentRequest;
import com.example.tinyhr.organization.application.dto.ReorderDepartmentsRequest;
import com.example.tinyhr.organization.application.dto.UpdateDepartmentRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.department.Department;
import com.example.tinyhr.organization.domain.department.DepartmentRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부서(부서 &gt; 팀 2뎁스 트리)를 등록·수정·정렬·아카이브한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /** 새 부서(또는 하위 팀) 등록. 같은 상위 아래 부서명은 겹칠 수 없다. */
    public String create(CreateDepartmentRequest request) {
        Department parent = null;
        if (request.parentId() != null) {
            parent = departmentRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(OrganizationErrorCode.DEPARTMENT_PARENT_NOT_FOUND));
        }
        if (departmentRepository.existsInParentByName(request.name().trim(), request.parentId())) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_NAME_DUPLICATED);
        }
        // 트리 깊이·상위 활성 불변식은 엔티티가 강제한다.
        Department department = Department.create(parent, request.name());
        departmentRepository.save(department);
        return department.getId();
    }

    /** 부서 정보 수정(이름·직무기술). null 필드는 변경하지 않는다. */
    public void update(String departmentId, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.DEPARTMENT_NOT_FOUND));

        if (request.name() != null) {
            boolean duplicated = departmentRepository.existsInParentByNameExcludingId(
                    request.name().trim(), department.getParentId(), departmentId);
            if (duplicated) {
                throw new BusinessException(OrganizationErrorCode.DEPARTMENT_NAME_DUPLICATED);
            }
            department.rename(request.name());
        }
        if (request.responsibilities() != null) {
            department.updateResponsibilities(request.responsibilities());
        }
        departmentRepository.save(department);
    }

    /** 부서 아카이브(소프트 삭제). 활성 하위 부서가 있으면 거부한다. */
    public void archive(String departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.DEPARTMENT_NOT_FOUND));
        boolean hasActiveChild = departmentRepository.findByParentId(departmentId).stream()
                .anyMatch(Department::isActive);
        if (hasActiveChild) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_HAS_ACTIVE_CHILDREN);
        }
        department.archive();
        departmentRepository.save(department);
    }

    /** 같은 상위 아래 형제 부서들의 노출 순서를 입력 순서대로 재정렬한다. */
    public void reorderSiblings(ReorderDepartmentsRequest request) {
        List<String> ids = request.orderedDepartmentIds();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_REORDER_DUPLICATED);
        }

        String parentId = request.parentId();
        List<Department> targets = new ArrayList<>();
        for (String id : ids) {
            Department department = departmentRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(OrganizationErrorCode.DEPARTMENT_NOT_FOUND));
            if (!Objects.equals(department.getParentId(), parentId)) {
                throw new BusinessException(OrganizationErrorCode.DEPARTMENT_REORDER_PARENT_MISMATCH);
            }
            targets.add(department);
        }

        for (int i = 0; i < targets.size(); i++) {
            targets.get(i).changeOrder(i + 1);
        }
        departmentRepository.saveAll(targets);
    }
}
