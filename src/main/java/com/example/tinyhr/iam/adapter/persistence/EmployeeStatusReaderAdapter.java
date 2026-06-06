package com.example.tinyhr.iam.adapter.persistence;

import com.example.tinyhr.iam.adapter.mapper.EmployeeStatusQueryMapper;
import com.example.tinyhr.iam.domain.auth.EmployeeStatusReader;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link EmployeeStatusReader} 의 구현 — organization 의 employee 테이블에서 상태값을 읽는다.
 * 상태 문자열은 organization {@code EmployeeStatus} 와 동일 enum 이름이라 그대로 매핑된다.
 */
@Component
public class EmployeeStatusReaderAdapter implements EmployeeStatusReader {

    private final EmployeeStatusQueryMapper mapper;

    public EmployeeStatusReaderAdapter(EmployeeStatusQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<LoginStatus> findStatus(String employeeId) {
        String status = mapper.findStatus(employeeId);
        if (status == null) {
            return Optional.empty();
        }
        return Optional.of(LoginStatus.valueOf(status));
    }
}
