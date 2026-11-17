package io.github.jaycong.user.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.jaycong.user.domain.entity.MemberOperation;

@Mapper
public interface MemberOperationMapper extends BaseMapper<MemberOperation> {}
