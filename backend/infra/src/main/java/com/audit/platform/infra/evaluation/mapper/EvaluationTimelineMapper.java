package com.audit.platform.infra.evaluation.mapper;

import com.audit.platform.infra.evaluation.entity.EvaluationTimelineEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EvaluationTimelineMapper extends BaseMapper<EvaluationTimelineEntity> {

    @Select("SELECT * FROM evaluation_timeline WHERE num = #{num} LIMIT 1")
    EvaluationTimelineEntity selectRawByNum(@Param("num") String num);

    @Update("UPDATE evaluation_timeline SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
