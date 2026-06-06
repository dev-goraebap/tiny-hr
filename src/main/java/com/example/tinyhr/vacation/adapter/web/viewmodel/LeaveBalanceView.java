package com.example.tinyhr.vacation.adapter.web.viewmodel;

/** 내 연차 잔액 응답. units 는 쿼터(0.25일), days 는 일 단위. */
public record LeaveBalanceView(long balanceUnits, double balanceDays) {

    public static LeaveBalanceView ofUnits(long units) {
        return new LeaveBalanceView(units, units / 4.0);
    }
}
