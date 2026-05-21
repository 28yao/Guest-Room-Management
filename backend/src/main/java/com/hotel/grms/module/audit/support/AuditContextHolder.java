package com.hotel.grms.module.audit.support;

/**
 * 审计上下文：在业务方法返回前绑定 bizId 与前后快照，供切面落库。
 *
 * @author liuxinsi
 * @date 2026-05-22
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditPayload> CONTEXT = new ThreadLocal<AuditPayload>();

    private AuditContextHolder() {
    }

    /**
     * 绑定本次操作的审计载荷。
     *
     * @param bizId       业务主键
     * @param beforeValue 变更前 JSON
     * @param afterValue  变更后 JSON
     * @param summary     摘要
     */
    public static void bind(Long bizId, String beforeValue, String afterValue, String summary) {
        AuditPayload payload = new AuditPayload();
        payload.setBizId(bizId);
        payload.setBeforeValue(beforeValue);
        payload.setAfterValue(afterValue);
        payload.setSummary(summary);
        CONTEXT.set(payload);
    }

    /**
     * 取出并清除上下文。
     *
     * @return 载荷或 null
     */
    public static AuditPayload poll() {
        AuditPayload payload = CONTEXT.get();
        CONTEXT.remove();
        return payload;
    }

    /**
     * 审计载荷。
     */
    public static class AuditPayload {

        private Long bizId;
        private String beforeValue;
        private String afterValue;
        private String summary;

        public Long getBizId() {
            return bizId;
        }

        public void setBizId(Long bizId) {
            this.bizId = bizId;
        }

        public String getBeforeValue() {
            return beforeValue;
        }

        public void setBeforeValue(String beforeValue) {
            this.beforeValue = beforeValue;
        }

        public String getAfterValue() {
            return afterValue;
        }

        public void setAfterValue(String afterValue) {
            this.afterValue = afterValue;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }
    }
}
