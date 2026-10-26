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
@TableName("rpt_daily")
public class RptDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long storeId;
    private LocalDate statDate;
    private Integer roomCount;
    private Integer occupied;
    private Integer orderCount;
    private BigDecimal revenue;
    private Integer memberCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
