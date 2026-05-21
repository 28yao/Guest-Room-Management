package com.hotel.grms.module.room.controller;

import com.hotel.grms.common.R;
import com.hotel.grms.module.room.dto.ForceStatusRequest;
import com.hotel.grms.module.room.dto.MaintenanceEndRequest;
import com.hotel.grms.module.room.dto.MaintenanceStartRequest;
import com.hotel.grms.module.room.dto.RoomBoardItemDto;
import com.hotel.grms.module.room.dto.RoomRequest;
import com.hotel.grms.module.room.dto.RoomResponse;
import com.hotel.grms.module.room.entity.Room;
import com.hotel.grms.module.room.service.RoomBoardService;
import com.hotel.grms.module.room.service.RoomMaintenanceService;
import com.hotel.grms.module.room.service.RoomService;
import com.hotel.grms.module.room.service.RoomTypeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客房与房态图、维修、强制改态接口。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomBoardService roomBoardService;
    private final RoomMaintenanceService roomMaintenanceService;
    private final RoomTypeService roomTypeService;

    public RoomController(RoomService roomService, RoomBoardService roomBoardService,
                          RoomMaintenanceService roomMaintenanceService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomBoardService = roomBoardService;
        this.roomMaintenanceService = roomMaintenanceService;
        this.roomTypeService = roomTypeService;
    }

    /**
     * 房态图。
     *
     * @param floorNo 楼层
     * @return 房态图项
     */
    @GetMapping("/board")
    public R<List<RoomBoardItemDto>> board(@RequestParam(required = false) Integer floorNo) {
        return R.ok(roomBoardService.loadBoard(floorNo));
    }

    /**
     * 客房列表。
     *
     * @param floorNo 楼层
     * @return 客房列表
     */
    @GetMapping
    public R<List<RoomResponse>> list(@RequestParam(required = false) Integer floorNo) {
        return R.ok(roomService.listRooms(floorNo));
    }

    /**
     * 创建客房。
     *
     * @param request 请求
     * @return 新客房
     */
    @PostMapping
    @PreAuthorize("hasAuthority('room:manage')")
    public R<RoomResponse> create(@Validated @RequestBody RoomRequest request) {
        return R.ok(roomService.create(request));
    }

    /**
     * 更新客房。
     *
     * @param id      客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('room:manage')")
    public R<RoomResponse> update(@PathVariable Long id, @Validated @RequestBody RoomRequest request) {
        return R.ok(roomService.update(id, request));
    }

    /**
     * 开始维修。
     *
     * @param id      客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAuthority('room:status:maintenance')")
    public R<RoomResponse> startMaintenance(@PathVariable Long id,
                                            @Validated @RequestBody MaintenanceStartRequest request) {
        Room room = roomMaintenanceService.startMaintenance(id, request);
        return R.ok(toResponse(room));
    }

    /**
     * 结束维修。
     *
     * @param id      客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @PostMapping("/{id}/maintenance/end")
    @PreAuthorize("hasAuthority('room:status:maintenance')")
    public R<RoomResponse> endMaintenance(@PathVariable Long id,
                                          @Validated @RequestBody MaintenanceEndRequest request) {
        Room room = roomMaintenanceService.endMaintenance(id, request);
        return R.ok(toResponse(room));
    }

    /**
     * 强制改房态。
     *
     * @param id      客房 ID
     * @param request 请求
     * @return 更新后客房
     */
    @PostMapping("/{id}/status/force")
    @PreAuthorize("hasAuthority('room:status:force')")
    public R<RoomResponse> forceStatus(@PathVariable Long id, @Validated @RequestBody ForceStatusRequest request) {
        Room room = roomService.forceStatus(id, request);
        return R.ok(toResponse(room));
    }

    private RoomResponse toResponse(Room room) {
        String typeName = roomTypeService.getById(room.getRoomTypeId()).getName();
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomNo(room.getRoomNo());
        response.setRoomTypeId(room.getRoomTypeId());
        response.setRoomTypeName(typeName);
        response.setFloorNo(room.getFloorNo());
        response.setStatus(room.getStatus());
        response.setVersion(room.getVersion());
        return response;
    }
}
