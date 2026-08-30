package com.audit.platform.infra.evaluation.mapper;

import com.audit.platform.infra.evaluation.entity.EvaluationAnnotationEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EvaluationAnnotationMapper extends BaseMapper<EvaluationAnnotationEntity> {

    @Select("SELECT * FROM evaluation_annotation WHERE num = #{num} LIMIT 1")
    EvaluationAnnotationEntity selectRawByNum(@Param("num") String num);

    @Update("UPDATE evaluation_annotation SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
