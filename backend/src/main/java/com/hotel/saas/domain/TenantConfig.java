package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_config")
public class TenantConfig extends BaseEntity {
    private Long tenantId;
    private String configKey;
    private String configValue;
}
