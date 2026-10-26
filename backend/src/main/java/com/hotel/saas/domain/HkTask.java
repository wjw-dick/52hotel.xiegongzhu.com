package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hk_task")
public class HkTask extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private Long roomId;
    private String type;
    private String status;
    private String assignee;
    private String note;
    private LocalDateTime finishTime;
}
