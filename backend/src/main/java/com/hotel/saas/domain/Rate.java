package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_rate")
public class Rate extends BaseEntity {
    private Long tenantId;
    private Long roomTypeId;
    private LocalDate rateDate;
    private String priceType;
    private BigDecimal price;
}
