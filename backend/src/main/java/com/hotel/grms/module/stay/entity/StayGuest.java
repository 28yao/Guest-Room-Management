package com.hotel.grms.module.stay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 在住客人信息实体（MVP 每单一条主客人）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@TableName("stay_guest")
public class StayGuest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stayOrderId;
    private String guestName;
    private String guestPhone;
    private String idCard;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStayOrderId() {
        return stayOrderId;
    }

    public void setStayOrderId(Long stayOrderId) {
        this.stayOrderId = stayOrderId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}
