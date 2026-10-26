package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hk_repair")
public class HkRepair extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private Long roomId;
    private String title;
    private String images;
    private String status;
    private String assignee;
    private String reportUser;
    private LocalDateTime finishTime;
}
