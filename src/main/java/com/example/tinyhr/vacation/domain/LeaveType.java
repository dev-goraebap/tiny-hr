package com.example.tinyhr.vacation.domain;

/**
 * 연차 소비 단위 휴가 종류. 소비량은 0.25일(쿼터) 단위 정수로 다룬다(부동소수 오차 회피).
 *
 * <ul>
 *   <li>FULL_DAY = 4쿼터(1일)</li>
 *   <li>HALF_DAY_AM/PM = 2쿼터(0.5일)</li>
 *   <li>QUARTER_DAY = 1쿼터(0.25일)</li>
 * </ul>
 */
public enum LeaveType {
    FULL_DAY(4),
    HALF_DAY_AM(2),
    HALF_DAY_PM(2),
    QUARTER_DAY(1);

    private final int unitsPerDay;

    LeaveType(int unitsPerDay) {
        this.unitsPerDay = unitsPerDay;
    }

    /** 하루치 소비 쿼터(FULL=4). 부분휴가는 하루 안에서의 소비량이기도 하다. */
    public int unitsPerDay() {
        return unitsPerDay;
    }

    public boolean isFullDay() {
        return this == FULL_DAY;
    }
}
