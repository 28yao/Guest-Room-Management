package com.hotel.grms.module.audit.support;

/**
 * 审计操作类型常量。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public final class AuditOpType {

    public static final String RES_ASSIGN_ROOM = "RES_ASSIGN_ROOM";
    public static final String RES_CANCEL = "RES_CANCEL";
    public static final String RES_CANCEL_REFUND = "RES_CANCEL_REFUND";
    public static final String RES_RELEASE = "RES_RELEASE";
    public static final String STAY_CHECK_IN = "STAY_CHECK_IN";
    public static final String STAY_CHECK_IN_RES = "STAY_CHECK_IN_RES";
    public static final String STAY_CHANGE_ROOM = "STAY_CHANGE_ROOM";
    public static final String STAY_CHECKOUT = "STAY_CHECKOUT";
    public static final String FOLIO_ADJUST_PRICE = "FOLIO_ADJUST_PRICE";
    public static final String ROOM_FORCE_STATUS = "ROOM_FORCE_STATUS";

    private AuditOpType() {
    }
}
