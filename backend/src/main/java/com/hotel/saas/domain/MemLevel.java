package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mem_level")
public class MemLevel extends BaseNoUpdateEntity {
    private Long tenantId;
    private String name;
    private BigDecimal minAmount;
    private BigDecimal discount;
    private String benefits;
    private Integer sort;
}
