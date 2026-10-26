package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_shift")
public class FinShift extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private String shiftName;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal cashAmount;
    private String status;
}
