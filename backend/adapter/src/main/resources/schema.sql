CREATE TABLE IF NOT EXISTS `rule_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(1024) NOT NULL DEFAULT '',
  `scene_num` VARCHAR(64) DEFAULT NULL,
  `scene_name` VARCHAR(128) DEFAULT NULL,
  `scene_params_json` CLOB,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `score_mode` VARCHAR(32) NOT NULL DEFAULT 'VETO_WEIGHTED',
  `overall_pass_score` DECIMAL(12,2) NOT NULL DEFAULT 70,
  `current_published_version_num` VARCHAR(64) DEFAULT NULL,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_set_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `rule_set_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `rule_set_num` VARCHAR(64) NOT NULL,
  `version_no` INT DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL,
  `current_flag` TINYINT NOT NULL DEFAULT 0,
  `score_mode` VARCHAR(32) NOT NULL,
  `overall_pass_score` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `based_on_version_no` INT DEFAULT NULL,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_set_version_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `rule_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `version_num` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `standard` TEXT NOT NULL,
  `min_score` DECIMAL(12,4) NOT NULL,
  `max_score` DECIMAL(12,4) NOT NULL,
  `pass_score` DECIMAL(12,4) NOT NULL,
  `weight` DECIMAL(12,4) NOT NULL DEFAULT 1,
  `veto` TINYINT NOT NULL DEFAULT 0,
  `positive_example` VARCHAR(2048) NOT NULL DEFAULT '',
  `negative_example` VARCHAR(2048) NOT NULL DEFAULT '',
  `sort_no` INT NOT NULL DEFAULT 1,
  `engine_kind` VARCHAR(32) NOT NULL DEFAULT 'ORDINARY',
  `engine_config_json` CLOB,
  `auditor_num` VARCHAR(64) DEFAULT NULL,
  `auditor_name` VARCHAR(128) DEFAULT NULL,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_item_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `auditor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `kind` VARCHAR(32) NOT NULL,
  `agent_num` VARCHAR(64) DEFAULT NULL,
  `agent_name` VARCHAR(128) DEFAULT NULL,
  `description` VARCHAR(1024) NOT NULL DEFAULT '',
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auditor_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `agent_catalog` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `agent_num` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(1024) NOT NULL DEFAULT '',
  `provider` VARCHAR(128) NOT NULL DEFAULT '',
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_catalog_agent_num` (`agent_num`)
);

CREATE TABLE IF NOT EXISTS `evaluation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `biz_id` VARCHAR(128) NOT NULL,
  `is_trial` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(32) NOT NULL,
  `auditor_num` VARCHAR(64) NOT NULL,
  `auditor_kind` VARCHAR(32) NOT NULL,
  `agent_name` VARCHAR(128) DEFAULT NULL,
  `rule_set_num` VARCHAR(64) DEFAULT NULL,
  `rule_set_version_num` VARCHAR(64) DEFAULT NULL,
  `rule_set_version_no` INT DEFAULT NULL,
  `rule_set_source` VARCHAR(32) DEFAULT NULL,
  `classify_confidence` DECIMAL(6,4) DEFAULT NULL,
  `classify_reason` VARCHAR(512) DEFAULT NULL,
  `score_mode` VARCHAR(32) DEFAULT NULL,
  `overall_pass_score` DECIMAL(12,2) DEFAULT NULL,
  `total_score` DECIMAL(16,4) DEFAULT NULL,
  `passed` TINYINT DEFAULT NULL,
  `complete` TINYINT NOT NULL DEFAULT 0,
  `fail_reason` VARCHAR(512) DEFAULT NULL,
  `credential_num` VARCHAR(64) DEFAULT NULL,
  `callback_url` VARCHAR(1024) DEFAULT NULL,
  `input_text` CLOB,
  `extra_params_json` CLOB,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluation_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `evaluation_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `evaluation_num` VARCHAR(64) NOT NULL,
  `object_key` VARCHAR(512) NOT NULL DEFAULT '',
  `file_name` VARCHAR(256) NOT NULL,
  `mime` VARCHAR(128) NOT NULL DEFAULT '',
  `role` VARCHAR(64) NOT NULL DEFAULT 'appendix',
  `sort_no` INT NOT NULL DEFAULT 1,
  `parse_failed` TINYINT NOT NULL DEFAULT 0,
  `excerpt` VARCHAR(2048) NOT NULL DEFAULT '',
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_att_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `evaluation_rule_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `evaluation_num` VARCHAR(64) NOT NULL,
  `rule_num` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(128) NOT NULL,
  `standard` TEXT NOT NULL,
  `min_score` DECIMAL(12,4) NOT NULL,
  `max_score` DECIMAL(12,4) NOT NULL,
  `pass_score` DECIMAL(12,4) NOT NULL,
  `weight` DECIMAL(12,4) NOT NULL,
  `veto` TINYINT NOT NULL DEFAULT 0,
  `machine_score` DECIMAL(12,4) DEFAULT NULL,
  `machine_rationale` TEXT,
  `human_score` DECIMAL(12,4) DEFAULT NULL,
  `human_reason` VARCHAR(1024) DEFAULT NULL,
  `failed` TINYINT NOT NULL DEFAULT 0,
  `fail_reason` VARCHAR(512) DEFAULT NULL,
  `evidence_json` TEXT,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_result_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `evaluation_annotation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `evaluation_num` VARCHAR(64) NOT NULL,
  `target` VARCHAR(16) NOT NULL,
  `rule_num` VARCHAR(64) DEFAULT NULL,
  `file_num` VARCHAR(64) DEFAULT NULL,
  `location` VARCHAR(256) DEFAULT NULL,
  `content` VARCHAR(2048) NOT NULL,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_ann_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `evaluation_timeline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `evaluation_num` VARCHAR(64) NOT NULL,
  `actor` VARCHAR(64) NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `detail` VARCHAR(1024) DEFAULT NULL,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_tl_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `credential` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `key_prefix` VARCHAR(32) NOT NULL,
  `secret_hash` VARCHAR(128) NOT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credential_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `integration_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `callback_url` VARCHAR(1024) NOT NULL DEFAULT '',
  `subscribed_events` VARCHAR(512) NOT NULL DEFAULT '',
  `classify_threshold` DECIMAL(6,4) NOT NULL DEFAULT 0.7000,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_integration_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `webhook_delivery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `event_id` VARCHAR(64) NOT NULL,
  `evaluation_num` VARCHAR(64) NOT NULL,
  `biz_id` VARCHAR(128) NOT NULL,
  `event_name` VARCHAR(64) NOT NULL,
  `payload_json` TEXT NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `retry_count` INT NOT NULL DEFAULT 0,
  `next_retry_time` DATETIME(3) DEFAULT NULL,
  `last_error` VARCHAR(1024) DEFAULT NULL,
  `callback_url` VARCHAR(1024) DEFAULT NULL,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_webhook_num` (`num`)
);

CREATE TABLE IF NOT EXISTS `scene` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `num` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(1024) NOT NULL DEFAULT '',
  `extra_params_json` CLOB,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `create_no` VARCHAR(64) NOT NULL,
  `update_no` VARCHAR(64) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_num` (`num`)
);

ALTER TABLE `rule_set` ADD COLUMN IF NOT EXISTS `scene_num` VARCHAR(64);
ALTER TABLE `rule_set` ADD COLUMN IF NOT EXISTS `scene_name` VARCHAR(128);
ALTER TABLE `rule_set` ADD COLUMN IF NOT EXISTS `scene_params_json` CLOB;
ALTER TABLE `rule_item` ADD COLUMN IF NOT EXISTS `engine_kind` VARCHAR(32) DEFAULT 'ORDINARY';
ALTER TABLE `rule_item` ADD COLUMN IF NOT EXISTS `engine_config_json` CLOB;
ALTER TABLE `evaluation` ADD COLUMN IF NOT EXISTS `input_text` CLOB;
ALTER TABLE `evaluation` ADD COLUMN IF NOT EXISTS `extra_params_json` CLOB;
ALTER TABLE `rule_set` ADD COLUMN IF NOT EXISTS `score_mode` VARCHAR(32) DEFAULT 'VETO_WEIGHTED';
ALTER TABLE `rule_set` ADD COLUMN IF NOT EXISTS `overall_pass_score` DECIMAL(12,2) DEFAULT 70;
ALTER TABLE `rule_item` ADD COLUMN IF NOT EXISTS `auditor_num` VARCHAR(64);
ALTER TABLE `rule_item` ADD COLUMN IF NOT EXISTS `auditor_name` VARCHAR(128);
