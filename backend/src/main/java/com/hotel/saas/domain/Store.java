package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_store")
public class Store extends BaseEntity {
    private Long tenantId;
    private String name;
    private String code;
    private String city;
    private String address;
    private String phone;
    private String intro;
    private String cover;
    private Integer status;
}
