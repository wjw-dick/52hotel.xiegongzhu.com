package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_wechat_config")
public class WechatConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String appType;
    private String appId;
    private String appSecret;
    private String token;
    private String aesKey;
    private String domain;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
