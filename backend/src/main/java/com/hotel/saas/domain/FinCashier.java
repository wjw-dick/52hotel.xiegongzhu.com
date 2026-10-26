package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_cashier")
public class FinCashier extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private String bizType;
    private BigDecimal amount;
    private String payType;
    private String orderNo;
    private String operator;
    private Long shiftId;
}
