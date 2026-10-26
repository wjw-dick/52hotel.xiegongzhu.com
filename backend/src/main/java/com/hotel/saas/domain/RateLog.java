package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_rate_log")
public class RateLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long roomTypeId;
    private LocalDate rateDate;
    private String priceType;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String operator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
