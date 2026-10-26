package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_building")
public class Building extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private String name;
    private Integer floors;
    private Integer sort;
}
