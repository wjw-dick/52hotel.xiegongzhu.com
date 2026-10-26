package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_checkin")
public class Checkin extends BaseEntity {
    private Long tenantId;
    private Long orderId;
    private Long storeId;
    private Long roomId;
    private String guestName;
    private String idNo;
    private LocalDateTime checkInTime;
    private LocalDateTime expectCheckOut;
    private BigDecimal deposit;
    private String depositPayType;
    private String cardNo;
    private String minorInfo;
    private String status;
}
