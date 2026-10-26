package com.hotel.saas.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class BaseNoUpdateEntity {
    @com.baomidou.mybatisplus.annotation.TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;
    @com.baomidou.mybatisplus.annotation.TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
    @com.baomidou.mybatisplus.annotation.TableLogic
    private Integer deleted;
}
