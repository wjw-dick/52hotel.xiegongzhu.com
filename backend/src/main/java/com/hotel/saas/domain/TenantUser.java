package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_user")
public class TenantUser extends BaseEntity {
    private Long tenantId;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String roleCode;
    private Long storeId;
    private Integer status;
}
