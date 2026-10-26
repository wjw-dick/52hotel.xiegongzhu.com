package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_settlement")
public class FinSettlement extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long orderId;
    private Long checkinId;
    private BigDecimal roomFee;
    private BigDecimal depositDeduct;
    private BigDecimal discount;
    private BigDecimal otherFee;
    private BigDecimal total;
    private String payType;
    private String status;
    private String operator;
}
