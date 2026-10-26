package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_i18n")
public class I18n extends BaseEntity {
    private Long tenantId;
    private String lang;
    private String msgKey;
    private String msgValue;
}
