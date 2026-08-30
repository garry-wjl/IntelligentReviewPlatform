package com.audit.platform.domain.common;

/**
 * 跨领域领域事件类型常量。
 */
public final class DomainEventConstant {
    private DomainEventConstant() {
    }

    public static final String RULE_SET_SAVED = "RULE_SET_SAVED";
    public static final String RULE_SET_DELETED = "RULE_SET_DELETED";
    public static final String RULE_SET_DRAFT_CREATED = "RULE_SET_DRAFT_CREATED";
    public static final String RULE_SET_PUBLISHED = "RULE_SET_PUBLISHED";
    public static final String RULE_SET_ENABLED_CHANGED = "RULE_SET_ENABLED_CHANGED";
    public static final String RULE_SET_PUBLISH_DISABLED = "RULE_SET_PUBLISH_DISABLED";

    public static final String SCENE_SAVED = "SCENE_SAVED";
    public static final String SCENE_DELETED = "SCENE_DELETED";
    public static final String SCENE_ENABLED_CHANGED = "SCENE_ENABLED_CHANGED";

    public static final String AUDITOR_SAVED = "AUDITOR_SAVED";
    public static final String AUDITOR_DELETED = "AUDITOR_DELETED";
    public static final String AUDITOR_ENABLED_CHANGED = "AUDITOR_ENABLED_CHANGED";
    public static final String AGENT_CATALOG_REFRESHED = "AGENT_CATALOG_REFRESHED";

    public static final String EVALUATION_SAVED = "EVALUATION_SAVED";
    public static final String EVALUATION_DELETED = "EVALUATION_DELETED";
    public static final String EVALUATION_PARSED = "EVALUATION_PARSED";
    public static final String EVALUATION_CLASSIFIED = "EVALUATION_CLASSIFIED";
    public static final String EVALUATION_SCORED = "EVALUATION_SCORED";
    public static final String EVALUATION_UPDATED = "EVALUATION_UPDATED";
    public static final String EVALUATION_FINALIZED = "EVALUATION_FINALIZED";
    public static final String EVALUATION_FAILED = "EVALUATION_FAILED";
    public static final String EVALUATION_RECLASSIFIED = "EVALUATION_RECLASSIFIED";

    public static final String CREDENTIAL_CREATED = "CREDENTIAL_CREATED";
    public static final String CREDENTIAL_DISABLED = "CREDENTIAL_DISABLED";
    public static final String CREDENTIAL_DELETED = "CREDENTIAL_DELETED";
    public static final String INTEGRATION_UPDATED = "INTEGRATION_UPDATED";
    public static final String WEBHOOK_ENQUEUED = "WEBHOOK_ENQUEUED";
    public static final String WEBHOOK_SENT = "WEBHOOK_SENT";
    public static final String WEBHOOK_FAILED = "WEBHOOK_FAILED";
}
