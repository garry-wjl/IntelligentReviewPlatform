package com.audit.platform.domain.evaluation;

import com.audit.platform.domain.evaluation.gateway.EvaluationGateway;
import com.audit.platform.domain.evaluation.repository.EvaluationRepository;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.domain.evaluation.valueobject.ClassifyResultVO;
import com.audit.platform.domain.evaluation.valueobject.RuleItemSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.RuleScoreVO;
import com.audit.platform.domain.evaluation.valueobject.RuleSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.ScoreContextVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationScoringTest {

    @Test
    void allPassUsesDisplayScoreAndNoTotal() {
        Evaluation evaluation = scored("ALL_PASS", List.of(
                rule("R1", false, "8", "1"),
                rule("R2", false, "7", "1")
        ), Map.of("R1", "8", "R2", "7"));
        assertEquals("SCORED", evaluation.getStatus());
        assertTrue(evaluation.getPassed());
        assertNull(evaluation.getTotalScore());
    }

    @Test
    void weightedSumAggregatesWeightTimesScore() {
        Evaluation evaluation = scored("WEIGHTED_SUM", List.of(
                rule("R1", false, "8", "0.6"),
                rule("R2", false, "5", "0.4")
        ), Map.of("R1", "8", "R2", "5"));
        assertEquals(new BigDecimal("6.80"), evaluation.getTotalScore());
        assertFalse(evaluation.getPassed());
    }

    @Test
    void vetoWeightedExcludesVetoFromSum() {
        Evaluation evaluation = scored("VETO_WEIGHTED", List.of(
                rule("V1", true, "9", "1"),
                rule("R1", false, "8", "1")
        ), Map.of("V1", "9", "R1", "8"));
        assertEquals(new BigDecimal("8.00"), evaluation.getTotalScore());
        assertTrue(evaluation.getPassed());
    }

    @Test
    void humanPatchRecalculatesImmediately() {
        Evaluation evaluation = scored("WEIGHTED_SUM", List.of(
                rule("R1", false, "8", "1")
        ), Map.of("R1", "8"));
        evaluation.patchScore("R1", new BigDecimal("3"), "人工下调", "u1");
        assertEquals(new BigDecimal("3.00"), evaluation.getTotalScore());
        assertFalse(evaluation.getPassed());
    }

    private Evaluation scored(String mode, List<RuleItemSnapshotVO> rules, Map<String, String> scores) {
        StubGateway gateway = new StubGateway(mode, rules, scores);
        InMemoryRepo repo = new InMemoryRepo();
        Evaluation evaluation = new Evaluation("BIZ-1", "AUD-1", "RS-1", Boolean.FALSE,
                List.of(AttachmentVO.builder().fileName("a.pdf").objectKey("k").build()),
                repo, gateway, event -> {
        });
        evaluation.bindAuditorSnapshot("ORDINARY", null);
        evaluation.save("u1");
        evaluation.startParse("u1");
        evaluation.matchRuleSet("u1", new BigDecimal("0.7"));
        return evaluation;
    }

    private RuleItemSnapshotVO rule(String num, boolean veto, String pass, String weight) {
        return RuleItemSnapshotVO.builder()
                .ruleNum(num)
                .name(num)
                .standard("标准")
                .minScore(new BigDecimal("0"))
                .maxScore(new BigDecimal("10"))
                .passScore(new BigDecimal(pass))
                .weight(new BigDecimal(weight))
                .veto(veto)
                .build();
    }

    static class InMemoryRepo implements EvaluationRepository {
        private final Map<String, Evaluation> store = new ConcurrentHashMap<>();

        @Override
        public void save(Evaluation aggregate) {
            store.put(aggregate.getNum(), aggregate);
        }

        @Override
        public Evaluation findByNum(String num) {
            return store.get(num);
        }

        @Override
        public void deleteByNum(String num) {
            store.remove(num);
        }
    }

    static class StubGateway implements EvaluationGateway {
        private final String mode;
        private final List<RuleItemSnapshotVO> rules;
        private final Map<String, String> scores;
        private final AtomicInteger seq = new AtomicInteger();

        StubGateway(String mode, List<RuleItemSnapshotVO> rules, Map<String, String> scores) {
            this.mode = mode;
            this.rules = rules;
            this.scores = scores;
        }

        @Override
        public String generateNum(boolean trial) {
            return "EVL-" + seq.incrementAndGet();
        }

        @Override
        public String generateChildNum(String prefix) {
            return prefix + "-" + seq.incrementAndGet();
        }

        @Override
        public void ingestRemoteFiles(List<AttachmentVO> attachments) {
        }

        @Override
        public void parse(List<AttachmentVO> attachments) {
            attachments.forEach(a -> a.setParseFailed(Boolean.FALSE));
        }

        @Override
        public ClassifyResultVO classify(List<AttachmentVO> attachments) {
            return ClassifyResultVO.builder().ruleSetNum("RS-1").confidence(new BigDecimal("0.9")).reason("ok").build();
        }

        @Override
        public RuleSnapshotVO loadPublishedRules(String ruleSetNum) {
            return RuleSnapshotVO.builder()
                    .ruleSetNum(ruleSetNum)
                    .ruleSetVersionNum("RSV-1")
                    .versionNo(1)
                    .scoreMode(mode)
                    .overallPassScore(new BigDecimal("7"))
                    .rules(new ArrayList<>(rules))
                    .build();
        }

        @Override
        public RuleSnapshotVO loadDraftRules(String ruleSetVersionNum) {
            return loadPublishedRules("RS-1");
        }

        @Override
        public List<RuleScoreVO> score(RuleSnapshotVO snapshot, ScoreContextVO context, String auditorKind,
                                       String agentNum) {
            List<RuleScoreVO> list = new ArrayList<>();
            for (RuleItemSnapshotVO rule : snapshot.getRules()) {
                list.add(RuleScoreVO.builder()
                        .ruleNum(rule.getRuleNum())
                        .score(new BigDecimal(scores.get(rule.getRuleNum())))
                        .rationale("mock")
                        .failed(Boolean.FALSE)
                        .build());
            }
            return list;
        }

        @Override
        public String presignGet(String objectKey) {
            return "/internal/tos/" + objectKey;
        }
    }
}
