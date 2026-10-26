package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_param")
public class Param {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paramKey;
    private String paramValue;
    private String name;
    private String scope;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
