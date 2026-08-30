package com.audit.platform.application.access;

import com.alibaba.fastjson2.JSON;
import com.audit.platform.application.evaluation.EvaluationQueryService;
import com.audit.platform.client.access.dto.ApiKeyAuthParamDTO;
import com.audit.platform.client.access.dto.CredentialAuthDTO;
import com.audit.platform.client.access.dto.CredentialCreateParamDTO;
import com.audit.platform.client.access.dto.CredentialNumParamDTO;
import com.audit.platform.client.access.dto.CredentialSecretDTO;
import com.audit.platform.client.access.dto.IntegrationUpdateParamDTO;
import com.audit.platform.client.access.dto.PresignDTO;
import com.audit.platform.client.access.dto.PresignParamDTO;
import com.audit.platform.client.access.dto.WebhookEnqueueParamDTO;
import com.audit.platform.client.access.dto.WebhookNumParamDTO;
import com.audit.platform.client.common.dto.CountDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationDetailDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.domain.access.Credential;
import com.audit.platform.domain.access.IntegrationSetting;
import com.audit.platform.domain.access.factory.CredentialFactory;
import com.audit.platform.domain.access.factory.IntegrationSettingFactory;
import com.audit.platform.domain.access.valueobject.PresignVO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.common.constant.LockKeyConstant;
import com.audit.platform.infra.common.lock.RedisLockHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AccessCommandService {
    @Resource
    private CredentialFactory credentialFactory;
    @Resource
    private IntegrationSettingFactory integrationSettingFactory;
    @Resource
    private EvaluationQueryService evaluationQueryService;
    @Resource
    private AccessQueryService accessQueryService;
    @Resource
    private RedisLockHelper redisLockHelper;

    public PresignDTO presignUpload(PresignParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.ACCESS + "presign:" + param.getFileName(), () -> {
            IntegrationSetting setting = integrationSettingFactory.createByNum(IntegrationSetting.DEFAULT_NUM);
            PresignVO vo = setting.issuePresign(param.getFileName(), param.getContentType(), param.getOperatorId());
            return PresignDTO.builder()
                    .objectKey(vo.getObjectKey())
                    .uploadUrl(vo.getUploadUrl())
                    .method(vo.getMethod())
                    .build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public CredentialSecretDTO createCredential(CredentialCreateParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.ACCESS + "credential:create:" + param.getName(), () -> {
            Credential credential = credentialFactory.create(param.getName());
            credential.save(param.getOperatorId());
            return CredentialSecretDTO.builder()
                    .num(credential.getNum())
                    .name(credential.getName())
                    .keyPrefix(credential.getKeyPrefix())
                    .rawSecret(credential.getRawSecret())
                    .build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableCredential(CredentialNumParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.ACCESS + param.getNum(), () -> {
            Credential credential = credentialFactory.createByNum(param.getNum());
            credential.disable(param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public CredentialAuthDTO authenticate(ApiKeyAuthParamDTO param) {
        String prefix = param.getRawSecret().substring(0, Math.min(8, param.getRawSecret().length()));
        return redisLockHelper.execute(LockKeyConstant.ACCESS + "auth:" + prefix, () -> {
            Credential credential = credentialFactory.createByKeyPrefix(prefix);
            if (credential == null) {
                throw new BusinessException(401, "API Key 无效");
            }
            credential.assertSecret(param.getRawSecret(), param.getOperatorId());
            return CredentialAuthDTO.builder().credentialNum(credential.getNum()).name(credential.getName()).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateIntegration(IntegrationUpdateParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.ACCESS + IntegrationSetting.DEFAULT_NUM, () -> {
            IntegrationSetting setting = integrationSettingFactory.createByNum(IntegrationSetting.DEFAULT_NUM);
            setting.updateProfile(param.getCallbackUrl(), param.getSubscribedEvents(), param.getClassifyThreshold(),
                    param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void enqueueWebhook(WebhookEnqueueParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.EVALUATION + param.getEvaluationNum(), () -> {
            EvaluationNumParamDTO numParam = new EvaluationNumParamDTO();
            numParam.setNum(param.getEvaluationNum());
            EvaluationDetailDTO detail = evaluationQueryService.detail(numParam);
            IntegrationSetting setting = integrationSettingFactory.createByNum(IntegrationSetting.DEFAULT_NUM);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", param.getEventName());
            payload.put("evaluationNum", detail.getNum());
            payload.put("bizId", detail.getBizId());
            payload.put("status", detail.getStatus());
            payload.put("ruleSetNum", detail.getRuleSetNum());
            payload.put("ruleSetVersionNo", detail.getRuleSetVersionNo());
            payload.put("auditorNum", detail.getAuditorNum());
            payload.put("passed", detail.getPassed());
            payload.put("complete", detail.getComplete());
            payload.put("totalScore", detail.getTotalScore());
            setting.enqueueDelivery(detail.getNum(), detail.getBizId(), param.getEventName(), JSON.toJSONString(payload),
                    detail.getTrial(), detail.getCallbackUrl(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public CountDTO dispatchDue(EmptyParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.WEBHOOK + "due", () -> {
            int sent = 0;
            for (String deliveryNum : accessQueryService.listDueDeliveryNums(param)) {
                IntegrationSetting setting = integrationSettingFactory.createByNum(IntegrationSetting.DEFAULT_NUM);
                setting.dispatchOne(deliveryNum, param.getOperatorId());
                sent++;
            }
            return CountDTO.builder().count(sent).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void replayDead(WebhookNumParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.WEBHOOK + param.getNum(), () -> {
            IntegrationSetting setting = integrationSettingFactory.createByNum(IntegrationSetting.DEFAULT_NUM);
            setting.replayDead(param.getNum(), param.getOperatorId());
        });
    }
}
