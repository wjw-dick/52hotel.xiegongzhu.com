package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_ar")
public class FinAr extends BaseEntity {
    private Long tenantId;
    private String company;
    private String contact;
    private String phone;
    private BigDecimal balance;
    private String status;
}
