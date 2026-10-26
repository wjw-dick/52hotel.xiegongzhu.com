package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_platform_user")
public class PlatformUser extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private String roleCode;
    private Integer status;
}
