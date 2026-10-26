package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_room_type")
public class RoomType extends BaseEntity {
    private Long tenantId;
    private Long storeId;
    private String name;
    private String code;
    private BigDecimal area;
    private Integer capacity;
    private String bedType;
    private String facilities;
    private String cover;
    private String images;
    private BigDecimal basePrice;
    private String intro;
    private Integer status;
    private Integer sort;
}
