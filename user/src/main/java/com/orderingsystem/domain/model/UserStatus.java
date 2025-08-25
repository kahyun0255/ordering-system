package com.orderingsystem.domain.model;

public enum UserStatus {

    WITHDRAWN, // 사용자 자발적 탈퇴 (재가입 가능)
    DELETED,   // 운영자나 시스템에 의해 계정 삭제
    SUSPENDED, // 규칙 위반 등으로 일정 기간 사용 정지
    BLOCKED,   // 정책 위반으로 차단 (영구적 차단)
    ACTIVE     // 정상적으로 활동 가능한 상태

}
