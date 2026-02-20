package com.project.hanspoon.oneday.reservation.domain;

public enum ReservationStatus {
    // 결제 대기(가예약) 상태
    HOLD("예약 대기"),
    // 결제 완료 상태
    PAID("예약 확정"),
    // 취소 요청 상태(관리자 대시보드 집계용 하위호환)
    CANCEL_REQUESTED("취소 요청"),
    // 최종 취소 상태
    CANCELED("예약 취소"),
    // 결제 대기 시간이 지나 자동 만료된 상태
    EXPIRED("기간 만료"),
    // 수강 완료 상태
    COMPLETED("수강 완료");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}