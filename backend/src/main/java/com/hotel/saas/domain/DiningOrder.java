package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_dining_order")
public class DiningOrder extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private String roomNo;
    private String items;
    private BigDecimal amount;
    private String status;
    private String settleType;
}
