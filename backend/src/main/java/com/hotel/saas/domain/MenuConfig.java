package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu_config")
public class MenuConfig extends BaseEntity {
    private Long tenantId;
    private String platform;
    private String code;
    private String title;
    private String icon;
    private String path;
    private String parentCode;
    private Integer sort;
    private Integer enabled;
}
