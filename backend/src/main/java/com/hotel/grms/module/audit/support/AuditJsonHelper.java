package com.hotel.grms.module.audit.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计快照 JSON 序列化辅助。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
@Component
public class AuditJsonHelper {

    private final ObjectMapper objectMapper;

    public AuditJsonHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将键值对序列化为 JSON 字符串。
     *
     * @param pairs 交替 key、value
     * @return JSON 或 null
     */
    public String pairs(Object... pairs) {
        if (pairs == null || pairs.length == 0) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        int index = 0;
        while (index + 1 < pairs.length) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
            index = index + 2;
        }
        return toJson(map);
    }

    /**
     * 对象转 JSON。
     *
     * @param value 对象
     * @return JSON 或 null
     */
    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
