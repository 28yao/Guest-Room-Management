package com.hotel.grms.module.room.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.room.dto.RoomTypeResponse;
import com.hotel.grms.module.room.service.RoomTypeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 房型维护接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    /**
     * 房型列表。
     *
     * @return 房型列表
     */
    @GetMapping
    public R<List<RoomTypeResponse>> list() {
        return R.ok(roomTypeService.listAll());
    }

    /**
     * 创建房型。
     *
     * @param request 请求
     * @return 新房型
     */
    @PostMapping
    @PreAuthorize("hasAuthority('room:type:manage')")
    public R<RoomTypeResponse> create(@Validated @RequestBody RoomTypeRequest request) {
        return R.ok(roomTypeService.create(request));
    }

    /**
     * 更新房型。
     *
     * @param id      房型 ID
     * @param request 请求
     * @return 更新后房型
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('room:type:manage')")
    public R<RoomTypeResponse> update(@PathVariable Long id, @Validated @RequestBody RoomTypeRequest request) {
        return R.ok(roomTypeService.update(id, request));
    }
}
