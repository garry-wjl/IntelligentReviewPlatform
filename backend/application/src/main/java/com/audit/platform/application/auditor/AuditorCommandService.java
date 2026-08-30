package com.audit.platform.application.auditor;

import com.audit.platform.client.auditor.dto.AuditorCreateParamDTO;
import com.audit.platform.client.auditor.dto.AuditorEnabledParamDTO;
import com.audit.platform.client.auditor.dto.AuditorUpdateParamDTO;
import com.audit.platform.client.common.dto.CountDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.NumDTO;
import com.audit.platform.domain.auditor.Auditor;
import com.audit.platform.domain.auditor.factory.AuditorFactory;
import com.audit.platform.infra.common.constant.LockKeyConstant;
import com.audit.platform.infra.common.lock.RedisLockHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorCommandService {
    public static final String SYSTEM_AUDITOR_NUM = "AUD-SYS";

    @Resource
    private AuditorFactory auditorFactory;
    @Resource
    private RedisLockHelper redisLockHelper;

    @Transactional(rollbackFor = Exception.class)
    public NumDTO create(AuditorCreateParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.AUDITOR + "create:" + param.getName(), () -> {
            Auditor auditor = auditorFactory.create(param.getName(), param.getKind(), param.getAgentNum(),
                    param.getDescription());
            auditor.save(param.getOperatorId());
            return NumDTO.builder().num(auditor.getNum()).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(AuditorUpdateParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.AUDITOR + param.getNum(), () -> {
            Auditor auditor = auditorFactory.createByNum(param.getNum());
            auditor.updateProfile(param.getName(), param.getKind(), param.getAgentNum(), param.getDescription(),
                    param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(AuditorEnabledParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.AUDITOR + param.getNum(), () -> {
            Auditor auditor = auditorFactory.createByNum(param.getNum());
            auditor.setEnabledFlag(param.getEnabled(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public CountDTO syncAgentCatalog(EmptyParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.AGENT_CATALOG, () -> {
            Auditor auditor = auditorFactory.createByNum(SYSTEM_AUDITOR_NUM);
            auditor.refreshAgentCatalog(param.getOperatorId());
            return CountDTO.builder().count(1).build();
        });
    }
}
