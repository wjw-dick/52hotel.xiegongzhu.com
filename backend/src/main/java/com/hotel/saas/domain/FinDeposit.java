package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_deposit")
public class FinDeposit extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long checkinId;
    private BigDecimal amount;
    private String type;
    private String payType;
    private String operator;
}
