package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mem_coupon")
public class MemCoupon extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long memberId;
    private String name;
    private BigDecimal amount;
    private String status;
    private LocalDate expireDate;
}
