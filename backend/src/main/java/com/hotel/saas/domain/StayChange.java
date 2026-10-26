package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_stay_change")
public class StayChange {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long checkinId;
    private String type;
    private String oldValue;
    private String newValue;
    private String operator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
