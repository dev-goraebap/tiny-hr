package com.example.tinyhr.vacation.adapter.mapper;

import com.example.tinyhr.vacation.adapter.mapper.viewmodel.MyLeaveItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 내 휴가/잔액 읽기 전용 조회 매퍼(MyBatis). SQL 은 resources/mapper/vacation/MeLeaveQueryMapper.xml. */
@Mapper
public interface MeLeaveQueryMapper {

    /** 내 휴가 신청 목록(최신순) + 결재 상태. */
    List<MyLeaveItem> listMyLeaves(@Param("requesterId") String requesterId);

    /** 내 연차 잔액(쿼터 단위). 잔액 홀더가 없으면 null. */
    Long findBalanceUnits(@Param("employeeId") String employeeId);
}
