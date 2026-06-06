package com.example.tinyhr.iam.adapter.mapper;

import com.example.tinyhr.iam.adapter.mapper.viewmodel.RoleListItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 역할 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/iam/RoleQueryMapper.xml. */
@Mapper
public interface RoleQueryMapper {

    /** 관리자 화면용 역할 목록(활성 우선, 이름순) + 권한 + 활성 부여 수. */
    List<RoleListItem> listForAdmin();
}
