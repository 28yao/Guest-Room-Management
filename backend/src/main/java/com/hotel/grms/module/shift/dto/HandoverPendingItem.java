package com.hotel.grms.module.shift.dto;

/**
 * 交班待办事项条目。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public class HandoverPendingItem {

    /** 类型：IN_HOUSE / HK / RESERVATION */
    private String type;
    private Long refId;
    private String title;
    private String detail;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
