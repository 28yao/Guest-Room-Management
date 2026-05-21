package com.hotel.grms.module.room.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 房型创建/更新请求体。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class RoomTypeRequest {

    @NotBlank(message = "房型名称不能为空")
    private String name;
    private String description;
    @NotNull(message = "门市价不能为空")
    @DecimalMin(value = "0.01", message = "门市价必须大于 0")
    private BigDecimal rackRate;
    private String bedType;
    private String windowType;
    private Integer nonSmoking;
    private Integer maxGuests;
    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getRackRate() {
        return rackRate;
    }

    public void setRackRate(BigDecimal rackRate) {
        this.rackRate = rackRate;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public String getWindowType() {
        return windowType;
    }

    public void setWindowType(String windowType) {
        this.windowType = windowType;
    }

    public Integer getNonSmoking() {
        return nonSmoking;
    }

    public void setNonSmoking(Integer nonSmoking) {
        this.nonSmoking = nonSmoking;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
