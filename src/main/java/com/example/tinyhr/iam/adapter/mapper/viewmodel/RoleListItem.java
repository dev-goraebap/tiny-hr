package com.example.tinyhr.iam.adapter.mapper.viewmodel;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 역할 목록 조회 뷰(읽기 전용).
 *
 * <p>다른 ViewModel 은 record 지만, 이 뷰는 {@code permissions} 가 자식 테이블(role_permission)을
 * 묶은 중첩 컬렉션이라 MyBatis {@code <collection>} 매핑이 필요해 setter 기반 POJO 로 둔다.
 *
 * @see com.example.tinyhr.iam.adapter.mapper.RoleQueryMapper
 */
@Getter
@Setter
public class RoleListItem {

    private String roleId;
    private String name;
    private String description;
    private List<String> permissions = new ArrayList<>();
    private boolean system;
    private boolean active;
    private long assignedCount;
}
