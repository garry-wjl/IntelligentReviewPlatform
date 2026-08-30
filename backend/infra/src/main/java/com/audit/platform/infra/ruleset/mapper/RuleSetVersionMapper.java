package com.audit.platform.infra.ruleset.mapper;

import com.audit.platform.infra.ruleset.entity.RuleSetVersionEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RuleSetVersionMapper extends BaseMapper<RuleSetVersionEntity> {

    @Select("SELECT * FROM rule_set_version WHERE num = #{num} LIMIT 1")
    RuleSetVersionEntity selectRawByNum(@Param("num") String num);

    @Update("UPDATE rule_set_version SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
