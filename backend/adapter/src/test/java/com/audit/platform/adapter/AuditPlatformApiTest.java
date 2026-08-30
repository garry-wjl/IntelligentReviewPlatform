package com.audit.platform.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuditPlatformApiTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminFrontendPathsAndOpenApiFlow() throws Exception {
        JsonNode scene = post("/admin/v1/scene/command/create", Map.of(
                "name", "联调场景",
                "description", "自动化测试",
                "extraParams", List.of(Map.of("key", "projectName", "label", "项目名称"))));
        String sceneNum = scene.path("data").path("num").asText();
        assertTrue(sceneNum.startsWith("SCN-"));

        JsonNode scenePage = get("/admin/v1/scene/query/page?pageNo=1&pageSize=20&name=联调");
        assertTrue(scenePage.path("data").path("total").asInt() >= 1);
        JsonNode sceneList = get("/admin/v1/scene/query/list");
        assertTrue(sceneList.path("data").isArray() && sceneList.path("data").size() >= 1);

        JsonNode createdSet = post("/admin/v1/ruleset/command/create", Map.of(
                "name", "联调规则集",
                "description", "自动化测试",
                "sceneNum", sceneNum));
        String ruleSetNum = createdSet.path("data").path("num").asText();
        assertTrue(ruleSetNum.startsWith("RS-"));

        JsonNode detail = get("/admin/v1/ruleset/query/detail?num=" + ruleSetNum);
        String versionNum = detail.path("data").path("versions").get(0).path("num").asText();
        assertNotNull(versionNum);

        JsonNode auditor = post("/admin/v1/auditor/command/create", Map.of(
                "name", "普通审核器",
                "kind", "ORDINARY",
                "description", "基线分"));
        String auditorNum = auditor.path("data").path("num").asText();

        post("/admin/v1/ruleset/command/score-mode", Map.of(
                "num", ruleSetNum,
                "scoreMode", "ALL_PASS",
                "overallPassScore", 0));
        JsonNode upsert = post("/admin/v1/ruleset/command/upsert-rule", new java.util.HashMap<String, Object>() {{
            put("num", ruleSetNum);
            put("versionNum", versionNum);
            put("name", "结构完整");
            put("standard", "章节齐全、论证充分");
            put("minScore", 0);
            put("maxScore", 10);
            put("passScore", 6);
            put("weight", 1);
            put("veto", false);
            put("auditorNum", auditorNum);
            put("engineKind", "ORDINARY");
            put("checks", List.of(Map.of("paramKey", "Input", "op", "NOT_BLANK")));
        }});
        assertTrue(upsert.path("data").path("ruleNum").asText().startsWith("RUL-"));
        JsonNode published = post("/admin/v1/ruleset/command/publish", Map.of("num", ruleSetNum));
        assertEquals(1, published.path("data").path("versionNo").asInt());

        JsonNode page = get("/admin/v1/ruleset/query/page?pageNo=1&pageSize=20&keyword=联调");
        assertTrue(page.path("data").path("total").asInt() >= 1);
        JsonNode pageByNum = get("/admin/v1/ruleset/query/page?pageNo=1&pageSize=20&num=" + ruleSetNum);
        assertTrue(pageByNum.path("data").path("total").asInt() >= 1);
        JsonNode pageByName = get("/admin/v1/ruleset/query/page?pageNo=1&pageSize=20&name=联调");
        assertTrue(pageByName.path("data").path("total").asInt() >= 1);

        JsonNode trial = post("/admin/v1/evaluation/command/trial", Map.of(
                "bizId", "TRIAL-IT-1",
                "ruleSetNum", ruleSetNum,
                "ruleSetVersionNum", versionNum,
                "inputText", "章节齐全、论证充分的立项报告正文",
                "attachments", List.of(Map.of(
                        "fileName", "report.pdf",
                        "mime", "application/pdf",
                        "role", "main"))));
        String evalNum = trial.path("data").path("num").asText();
        assertTrue(evalNum.startsWith("TRL-"));

        JsonNode scored = waitStatus(evalNum, "SCORED");
        assertEquals("SCORED", scored.path("status").asText());
        assertEquals(true, scored.path("complete").asBoolean());
        assertEquals(true, scored.path("passed").asBoolean());
        assertEquals(0, new BigDecimal("6").compareTo(scored.path("results").get(0).path("machineScore").decimalValue()));

        JsonNode auditorPage = get("/admin/v1/auditor/query/page?pageNo=1&pageSize=20&keyword=普通");
        assertTrue(auditorPage.path("data").path("total").asInt() >= 1);
        JsonNode auditorByNum = get("/admin/v1/auditor/query/page?pageNo=1&pageSize=20&num=" + auditorNum);
        assertTrue(auditorByNum.path("data").path("total").asInt() >= 1);
        JsonNode agents = get("/admin/v1/auditor/query/agents");
        assertTrue(agents.path("data").isArray() && agents.path("data").size() >= 1);

        JsonNode integration = get("/admin/v1/access/query/integration");
        assertNotNull(integration.path("data").path("num").asText(null));

        JsonNode taskPage = get("/admin/v1/evaluation/query/page?pageNo=1&pageSize=10&isTrial=true");
        assertTrue(taskPage.path("data").path("total").asInt() >= 1);

        JsonNode secret = post("/admin/v1/access/command/create-credential", Map.of("name", "联调密钥"));
        String rawSecret = secret.path("data").path("rawSecret").asText();
        assertTrue(rawSecret.length() > 8);

        JsonNode openCreated = postOpen("/open/v1/evaluation/command/create", rawSecret, Map.of(
                "bizId", "APP-IT-1",
                "ruleSetNum", ruleSetNum,
                "inputText", "开放接口提交的正文",
                "attachments", List.of(Map.of(
                        "fileName", "app.pdf",
                        "fileUrl", "https://example.com/app.pdf",
                        "mime", "application/pdf"))));
        String openNum = openCreated.path("data").path("num").asText();
        JsonNode openDetail = waitOpenStatus(openNum, rawSecret, "SCORED");
        assertEquals("SCORED", openDetail.path("status").asText());

        JsonNode idempotent = postOpen("/open/v1/evaluation/command/create", rawSecret, Map.of(
                "bizId", "APP-IT-1",
                "ruleSetNum", ruleSetNum,
                "attachments", List.of(Map.of("fileName", "app.pdf"))));
        assertEquals(true, idempotent.path("data").path("idempotent").asBoolean());
        assertEquals(openNum, idempotent.path("data").path("num").asText());
    }

    private JsonNode waitStatus(String num, String expected) throws Exception {
        for (int i = 0; i < 40; i++) {
            JsonNode detail = get("/admin/v1/evaluation/query/detail?num=" + num).path("data");
            String status = detail.path("status").asText();
            if (expected.equals(status) || "FAILED".equals(status) || "TYPE_PENDING".equals(status)) {
                assertEquals(expected, status, detail.toString());
                return detail;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("任务未在超时内到达 " + expected);
    }

    private JsonNode waitOpenStatus(String num, String apiKey, String expected) throws Exception {
        for (int i = 0; i < 40; i++) {
            JsonNode detail = getOpen("/open/v1/evaluation/query/detail?num=" + num, apiKey).path("data");
            String status = detail.path("status").asText();
            if (expected.equals(status) || "FAILED".equals(status) || "TYPE_PENDING".equals(status)) {
                assertEquals(expected, status, detail.toString());
                return detail;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("开放任务未在超时内到达 " + expected);
    }

    private JsonNode post(String path, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Operator-Id", "tester");
        ResponseEntity<String> response = restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
        JsonNode node = objectMapper.readTree(response.getBody());
        assertEquals(200, node.path("code").asInt(), path + " " + response.getBody());
        return node;
    }

    private JsonNode get(String path) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Operator-Id", "tester");
        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode node = objectMapper.readTree(response.getBody());
        assertEquals(200, node.path("code").asInt(), path + " " + response.getBody());
        return node;
    }

    private JsonNode postOpen(String path, String apiKey, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        ResponseEntity<String> response = restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
        JsonNode node = objectMapper.readTree(response.getBody());
        assertEquals(200, node.path("code").asInt(), path + " " + response.getBody());
        return node;
    }

    private JsonNode getOpen(String path, String apiKey) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode node = objectMapper.readTree(response.getBody());
        assertEquals(200, node.path("code").asInt(), path + " " + response.getBody());
        return node;
    }
}
