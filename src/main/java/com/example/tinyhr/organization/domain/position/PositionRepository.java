package com.example.tinyhr.organization.domain.position;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 직위 쓰기 리포지토리. Spring Data JPA 가 구현을 생성하므로 별도 구현체를 두지 않는다.
 */
public interface PositionRepository extends JpaRepository<Position, String> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    boolean existsByDisplayOrder(int displayOrder);

    List<Position> findByActiveTrue();
}
