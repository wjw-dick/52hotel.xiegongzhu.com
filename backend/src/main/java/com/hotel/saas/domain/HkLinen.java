package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hk_linen")
public class HkLinen extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private String type;
    private String item;
    private Integer quantity;
    private String operator;
}
