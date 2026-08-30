package com.audit.platform.domain.scene;

import com.audit.platform.domain.scene.gateway.SceneGateway;
import com.audit.platform.domain.scene.repository.SceneRepository;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneTest {

    @Test
    void builtinsAlwaysPresentAndExtraValidated() {
        InMemoryRepo repo = new InMemoryRepo();
        Scene scene = new Scene("立项材料", "说明",
                "[{\"key\":\"projectName\",\"label\":\"项目名称\"}]",
                repo, new SequenceGateway(), event -> {
                });
        scene.save("u1");
        assertTrue(scene.getNum().startsWith("SCN-"));
        assertEquals(3, scene.resolveParams().size());
        assertEquals(SceneParamCatalog.INPUT, scene.resolveParams().get(0).getKey());
        assertEquals("projectName", scene.resolveParams().get(2).getKey());
    }

    @Test
    void reservedExtraKeyRejected() {
        Scene scene = new Scene("场景", "", "[{\"key\":\"Input\",\"label\":\"冲突\"}]",
                new InMemoryRepo(), new SequenceGateway(), event -> {
                });
        assertThrows(Exception.class, () -> scene.save("u1"));
    }

    static class InMemoryRepo implements SceneRepository {
        private final Map<String, Scene> store = new ConcurrentHashMap<>();

        @Override
        public void save(Scene aggregate) {
            store.put(aggregate.getNum(), aggregate);
        }

        @Override
        public Scene findByNum(String num) {
            return store.get(num);
        }

        @Override
        public void deleteByNum(String num) {
            store.remove(num);
        }
    }

    static class SequenceGateway implements SceneGateway {
        private final AtomicInteger seq = new AtomicInteger();

        @Override
        public String generateNum() {
            return "SCN-" + seq.incrementAndGet();
        }
    }
}
