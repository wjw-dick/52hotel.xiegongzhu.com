package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseNoUpdateEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hk_complaint")
public class HkComplaint extends BaseNoUpdateEntity {
    private Long tenantId;
    private Long storeId;
    private String roomNo;
    private String guest;
    private String content;
    private String status;
    private String handler;
}
