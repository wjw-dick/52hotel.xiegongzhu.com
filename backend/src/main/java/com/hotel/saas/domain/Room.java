package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_room")
public class Room extends BaseEntity {
    private Long tenantId;
    private Long storeId;
    private Long buildingId;
    private Integer floor;
    private String roomNo;
    private Long roomTypeId;
    private String status;
    private String lockCode;
    private String remark;
}
