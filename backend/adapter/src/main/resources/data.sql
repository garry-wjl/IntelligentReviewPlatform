MERGE INTO integration_setting (num, callback_url, subscribed_events, classify_threshold, create_no, update_no)
    KEY (num) VALUES ('INT-DEFAULT', '', 'evaluation.scored,evaluation.finalized,evaluation.failed', 0.7000, 'system', 'system');

MERGE INTO auditor (num, name, kind, description, enabled, create_no, update_no)
    KEY (num) VALUES ('AUD-SYS', '系统占位-目录同步', 'ORDINARY', '不可被任务选用', 0, 'system', 'system');

MERGE INTO agent_catalog (num, agent_num, name, description, provider, create_no, update_no)
    KEY (agent_num) VALUES ('AGC-1', 'agent-quality', '文档质量审核 Agent', '擅长报告结构、论证充分性与表述质量判断。', 'Agent 平台', 'system', 'system');

MERGE INTO agent_catalog (num, agent_num, name, description, provider, create_no, update_no)
    KEY (agent_num) VALUES ('AGC-2', 'agent-risk', '风险合规审核 Agent', '侧重风险披露、合规条款与红线项识别。', 'Agent 平台', 'system', 'system');

MERGE INTO agent_catalog (num, agent_num, name, description, provider, create_no, update_no)
    KEY (agent_num) VALUES ('AGC-3', 'agent-general', '通用文本审核 Agent', '通用多文档理解与逐条打分。', 'Agent 平台', 'system', 'system');

MERGE INTO scene (num, name, description, extra_params_json, enabled, create_no, update_no)
    KEY (num) VALUES ('SCN-DEFAULT', '通用文档审核', '内置 Input 与 Attachment，可按规则集扩展。', '[]', 1, 'system', 'system');
