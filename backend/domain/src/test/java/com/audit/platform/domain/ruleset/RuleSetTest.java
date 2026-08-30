package com.audit.platform.domain.ruleset;

import com.audit.platform.domain.ruleset.gateway.RuleSetGateway;
import com.audit.platform.domain.ruleset.repository.RuleSetRepository;
import com.audit.platform.domain.ruleset.valueobject.AuditorSnapshotVO;
import com.audit.platform.domain.ruleset.valueobject.RuleItemVO;
import com.audit.platform.domain.ruleset.valueobject.RuleSetVersionVO;
import com.audit.platform.domain.ruleset.valueobject.SceneSnapshotVO;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleSetTest {
    private InMemoryRuleSetRepository repository;
    private RecordingPublisher publisher;
    private SequenceGateway gateway;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRuleSetRepository();
        publisher = new RecordingPublisher();
        gateway = new SequenceGateway();
    }

    @Test
    void createDraftPublishAndScoreMode() {
        RuleSet ruleSet = new RuleSet("立项报告", "说明", "SCN-1", repository, gateway, publisher);
        ruleSet.save("u1");
        String draftNum = ruleSet.createDraft(null, "u1");
        ruleSet.changeScoreMode("ALL_PASS", BigDecimal.ZERO, "u1");
        ruleSet.upsertRule(draftNum, RuleItemVO.builder()
                .name("结构完整")
                .standard("章节齐全")
                .minScore(new BigDecimal("0"))
                .maxScore(new BigDecimal("10"))
                .passScore(new BigDecimal("6"))
                .weight(new BigDecimal("1"))
                .veto(Boolean.FALSE)
                .auditorNum("AUD-1")
                .sortNo(1)
                .build(), "u1");
        Integer versionNo = ruleSet.publish("u1");
        assertEquals(1, versionNo);
        RuleSet loaded = repository.findByNum(ruleSet.getNum());
        assertNotNull(loaded.getCurrentPublishedVersionNum());
        assertEquals("PUBLISHED", loaded.getVersions().get(0).getStatus());
    }

    @Test
    void vetoModeRequiresVetoRule() {
        RuleSet ruleSet = new RuleSet("风险", "", "SCN-1", repository, gateway, publisher);
        ruleSet.save("u1");
        String draftNum = ruleSet.createDraft(null, "u1");
        ruleSet.changeScoreMode("VETO_WEIGHTED", new BigDecimal("70"), "u1");
        ruleSet.upsertRule(draftNum, RuleItemVO.builder()
                .name("普通项")
                .standard("达标")
                .minScore(new BigDecimal("0"))
                .maxScore(new BigDecimal("10"))
                .passScore(new BigDecimal("6"))
                .weight(new BigDecimal("1"))
                .veto(Boolean.FALSE)
                .auditorNum("AUD-1")
                .build(), "u1");
        assertThrows(Exception.class, () -> ruleSet.publish("u1"));
    }

    @Test
    void cannotEditPublishedVersion() {
        RuleSet ruleSet = new RuleSet("立项报告", "", "SCN-1", repository, gateway, publisher);
        ruleSet.save("u1");
        String draftNum = ruleSet.createDraft(null, "u1");
        ruleSet.changeScoreMode("ALL_PASS", BigDecimal.ZERO, "u1");
        ruleSet.upsertRule(draftNum, sampleRule("结构"), "u1");
        ruleSet.publish("u1");
        assertThrows(Exception.class, () -> ruleSet.upsertRule(draftNum, sampleRule("再改"), "u1"));
    }

    @Test
    void newDraftInheritsLatestPublishedRules() {
        RuleSet ruleSet = new RuleSet("立项报告", "", "SCN-1", repository, gateway, publisher);
        ruleSet.save("u1");
        String draftNum = ruleSet.createDraft(null, "u1");
        ruleSet.changeScoreMode("ALL_PASS", BigDecimal.ZERO, "u1");
        ruleSet.upsertRule(draftNum, sampleRule("结构"), "u1");
        ruleSet.publish("u1");
        String next = ruleSet.createDraft(null, "u1");
        RuleSetVersionVO draft = ruleSet.getVersions().stream()
                .filter(v -> next.equals(v.getNum()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, draft.getRules().size());
        assertEquals("结构", draft.getRules().get(0).getName());
        assertEquals(Integer.valueOf(1), draft.getBasedOnVersionNo());
    }

    private RuleItemVO sampleRule(String name) {
        return RuleItemVO.builder()
                .name(name)
                .standard("标准")
                .minScore(new BigDecimal("0"))
                .maxScore(new BigDecimal("10"))
                .passScore(new BigDecimal("6"))
                .weight(new BigDecimal("1"))
                .veto(Boolean.FALSE)
                .auditorNum("AUD-1")
                .build();
    }

    static class InMemoryRuleSetRepository implements RuleSetRepository {
        private final Map<String, RuleSet> store = new ConcurrentHashMap<>();

        @Override
        public void save(RuleSet aggregate) {
            store.put(aggregate.getNum(), aggregate);
        }

        @Override
        public RuleSet findByNum(String num) {
            return store.get(num);
        }

        @Override
        public void deleteByNum(String num) {
            store.remove(num);
        }
    }

    static class SequenceGateway implements RuleSetGateway {
        private final AtomicInteger seq = new AtomicInteger(0);

        @Override
        public String generateNum() {
            return "RS-" + seq.incrementAndGet();
        }

        @Override
        public String generateVersionNum() {
            return "RSV-" + seq.incrementAndGet();
        }

        @Override
        public String generateRuleNum() {
            return "RUL-" + seq.incrementAndGet();
        }

        @Override
        public SceneSnapshotVO loadScene(String sceneNum) {
            return SceneSnapshotVO.builder()
                    .sceneNum(sceneNum)
                    .sceneName("通用场景")
                    .paramsJson(SceneParamCatalog.toResolvedJson(null))
                    .build();
        }

        @Override
        public AuditorSnapshotVO loadAuditor(String auditorNum) {
            return AuditorSnapshotVO.builder()
                    .num(auditorNum)
                    .name("测试审核器")
                    .kind("ORDINARY")
                    .enabled(Boolean.TRUE)
                    .build();
        }
    }

    static class RecordingPublisher implements DomainEventPublisher {
        @Override
        public void send(DomainEventDTO eventDTO) {
            assertTrue(eventDTO.getType() != null);
        }
    }
}
