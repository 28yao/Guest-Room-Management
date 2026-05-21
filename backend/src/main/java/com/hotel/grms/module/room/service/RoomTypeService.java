package com.hotel.grms.module.room.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.grms.common.BusinessException;
import com.hotel.grms.module.room.dto.RoomTypeRequest;
import com.hotel.grms.module.room.dto.RoomTypeResponse;
import com.hotel.grms.module.room.entity.RoomType;
import com.hotel.grms.module.room.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 房型维护服务。
 *
 * @author liuxinsi
 * @date 2026-05-21
 */
@Service
public class RoomTypeService {

    private final RoomTypeMapper roomTypeMapper;

    public RoomTypeService(RoomTypeMapper roomTypeMapper) {
        this.roomTypeMapper = roomTypeMapper;
    }

    /**
     * 查询全部房型。
     *
     * @return 房型列表
     */
    public List<RoomTypeResponse> listAll() {
        List<RoomType> types = roomTypeMapper.selectList(
                new LambdaQueryWrapper<RoomType>().orderByAsc(RoomType::getId));
        List<RoomTypeResponse> result = new ArrayList<RoomTypeResponse>(types.size());
        for (RoomType type : types) {
            result.add(toResponse(type));
        }
        return result;
    }

    /**
     * 按 ID 查询房型。
     *
     * @param id 房型 ID
     * @return 房型
     */
    public RoomType getById(Long id) {
        RoomType type = roomTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(40012, "房型不存在");
        }
        return type;
    }

    /**
     * 创建房型。
     *
     * @param request 请求
     * @return 新房型
     */
    public RoomTypeResponse create(RoomTypeRequest request) {
        RoomType entity = fromRequest(request);
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        roomTypeMapper.insert(entity);
        return toResponse(entity);
    }

    /**
     * 更新房型。
     *
     * @param id      房型 ID
     * @param request 请求
     * @return 更新后房型
     */
    public RoomTypeResponse update(Long id, RoomTypeRequest request) {
        RoomType entity = getById(id);
        applyRequest(entity, request);
        roomTypeMapper.updateById(entity);
        return toResponse(roomTypeMapper.selectById(id));
    }

    private RoomType fromRequest(RoomTypeRequest request) {
        RoomType entity = new RoomType();
        applyRequest(entity, request);
        return entity;
    }

    private void applyRequest(RoomType entity, RoomTypeRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setRackRate(request.getRackRate());
        entity.setBedType(request.getBedType());
        entity.setWindowType(request.getWindowType());
        entity.setNonSmoking(request.getNonSmoking() == null ? 0 : request.getNonSmoking());
        entity.setMaxGuests(request.getMaxGuests() == null ? 2 : request.getMaxGuests());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
    }

    private RoomTypeResponse toResponse(RoomType entity) {
        RoomTypeResponse response = new RoomTypeResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setRackRate(entity.getRackRate());
        response.setBedType(entity.getBedType());
        response.setWindowType(entity.getWindowType());
        response.setNonSmoking(entity.getNonSmoking());
        response.setMaxGuests(entity.getMaxGuests());
        response.setStatus(entity.getStatus());
        return response;
    }
}
