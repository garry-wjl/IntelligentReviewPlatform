package com.audit.platform.domain.evaluation.gateway;

import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.domain.evaluation.valueobject.ClassifyResultVO;
import com.audit.platform.domain.evaluation.valueobject.RuleScoreVO;
import com.audit.platform.domain.evaluation.valueobject.RuleSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.ScoreContextVO;

import java.util.List;

public interface EvaluationGateway {
    String generateNum(boolean trial);

    String generateChildNum(String prefix);

    /**
     * 将 fileUrl 拉取到 TOS，回填 objectKey。
     */
    void ingestRemoteFiles(List<AttachmentVO> attachments);

    void parse(List<AttachmentVO> attachments);

    ClassifyResultVO classify(List<AttachmentVO> attachments);

    RuleSnapshotVO loadPublishedRules(String ruleSetNum);

    RuleSnapshotVO loadDraftRules(String ruleSetVersionNum);

    List<RuleScoreVO> score(RuleSnapshotVO snapshot, ScoreContextVO context, String auditorKind, String agentNum);

    String presignGet(String objectKey);
}
