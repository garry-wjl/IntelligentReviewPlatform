package com.audit.platform.infra.access.mapper;

import com.audit.platform.infra.access.entity.WebhookDeliveryEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WebhookDeliveryMapper extends BaseMapper<WebhookDeliveryEntity> {

    @Select("SELECT * FROM webhook_delivery WHERE num = #{num} LIMIT 1")
    WebhookDeliveryEntity selectRawByNum(@Param("num") String num);

    @Update("UPDATE webhook_delivery SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
