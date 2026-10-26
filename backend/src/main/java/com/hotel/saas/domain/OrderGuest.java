package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order_guest")
public class OrderGuest extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long orderId;
    private String name;
    private String idType;
    private String idNo;
    private String phone;
    private Integer isMinor;
    private String guardianName;
    private String guardianPhone;
}
