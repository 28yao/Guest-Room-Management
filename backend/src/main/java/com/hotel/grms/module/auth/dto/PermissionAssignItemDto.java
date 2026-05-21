package com.hotel.grms.module.auth.dto;

/**
 * 权限分配项（含是否已勾选）。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
public class PermissionAssignItemDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean assigned;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }
}
