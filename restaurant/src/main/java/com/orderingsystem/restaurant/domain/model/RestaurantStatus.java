package com.orderingsystem.restaurant.domain.model;

public enum RestaurantStatus {

    PENDING_APPROVAL, // 관리자 승인 대기
    PRE_OPEN,         // 영업 전
    ACTIVE,           // 영업 중
    TEMP_CLOSED,      // 일시 휴업
    PERM_CLOSED,      // 완전 폐업
    SUSPENDED,        // 제재로 영업 정지
    DELETED           // 데이터 삭제(논리 삭제)

}

