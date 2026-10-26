package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fin_ar_ledger")
public class FinArLedger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long arId;
    private String type;
    private BigDecimal amount;
    private String remark;
    private String operator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
