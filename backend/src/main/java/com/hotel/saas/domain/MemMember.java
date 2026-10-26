package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mem_member")
public class MemMember extends BaseEntity {
    private Long tenantId;
    private String unionId;
    private String openId;
    private String name;
    private String phone;
    private String password;
    private String gender;
    private Long levelId;
    private Integer points;
    private BigDecimal balance;
    private BigDecimal totalAmount;
    private String tags;
    private Integer status;
}
