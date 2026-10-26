package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("biz_police_report")
public class PoliceReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long checkinId;
    private String guestName;
    private String idNo;
    private String roomNo;
    private String reportStatus;
    private LocalDateTime reportTime;
    private String response;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
