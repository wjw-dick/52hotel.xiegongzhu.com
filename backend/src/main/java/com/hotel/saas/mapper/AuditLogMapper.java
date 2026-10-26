package com.hotel.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.saas.domain.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
