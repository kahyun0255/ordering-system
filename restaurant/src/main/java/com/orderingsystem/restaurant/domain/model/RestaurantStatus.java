package com.orderingsystem.restaurant.domain.model;

import java.util.EnumSet;

public enum RestaurantStatus {

    PENDING_APPROVAL, // 관리자 승인 대기
    PRE_OPEN,         // 영업 전
    ACTIVE,           // 영업 중
    TEMP_CLOSED,      // 일시 휴업
    PERM_CLOSED,      // 완전 폐업
    SUSPENDED,        // 제재로 영업 정지
    DELETED;           // 데이터 삭제(논리 삭제)

    private static final EnumSet<RestaurantStatus> OWNER_TARGETS =
            EnumSet.of(PRE_OPEN, ACTIVE, TEMP_CLOSED, PERM_CLOSED);

    private static final EnumSet<RestaurantStatus> OWNER_LOCKED_CURRENTS =
            EnumSet.of(PENDING_APPROVAL, SUSPENDED, DELETED, PERM_CLOSED);

    public boolean ownerCanChangeTo(RestaurantStatus restaurantStatus){
        if (restaurantStatus == null) return false;
        if (OWNER_LOCKED_CURRENTS.contains(this)) return false;
        if (!OWNER_TARGETS.contains(restaurantStatus)) return false;

        return true;
    }

}

