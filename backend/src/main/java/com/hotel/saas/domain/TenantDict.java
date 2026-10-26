package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_dict")
public class TenantDict extends BaseNoUpdateEntity {
    private Long tenantId;
    private String type;
    private String code;
    private String name;
    private Integer sort;
}
