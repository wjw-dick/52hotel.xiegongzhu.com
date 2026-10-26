package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order")
public class Order extends BaseEntity {
    private Long tenantId;
    private Long storeId;
    private String orderNo;
    private String source;
    private String channel;
    private Long roomTypeId;
    private Long roomId;
    private Long memberId;
    private String guestName;
    private String guestPhone;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer nights;
    private Integer roomCount;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status;
    private String payType;
    private String company;
    private String remark;
    private String createdBy;
}
