package com.example.tinyhr.organization.domain.department;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 부서 쓰기 리포지토리. parentId 가 null(최상위) 인 경우를 위해 명시 쿼리를 둔다. */
public interface DepartmentRepository extends JpaRepository<Department, String> {

    /** 특정 부서의 직속 하위들. */
    List<Department> findByParentId(String parentId);

    @Query("""
            select count(d) > 0 from Department d
            where d.name = :name
              and ((:parentId is null and d.parentId is null) or d.parentId = :parentId)
            """)
    boolean existsInParentByName(@Param("name") String name, @Param("parentId") String parentId);

    @Query("""
            select count(d) > 0 from Department d
            where d.name = :name
              and d.id <> :excludeId
              and ((:parentId is null and d.parentId is null) or d.parentId = :parentId)
            """)
    boolean existsInParentByNameExcludingId(
            @Param("name") String name,
            @Param("parentId") String parentId,
            @Param("excludeId") String excludeId);
}
