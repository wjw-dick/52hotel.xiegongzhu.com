package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_channel")
public class Channel extends BaseNoUpdateEntity {
    private Long tenantId;
    private String name;
    private String code;
    private Integer status;
    private Integer syncStock;
    private LocalDateTime lastSync;
}
