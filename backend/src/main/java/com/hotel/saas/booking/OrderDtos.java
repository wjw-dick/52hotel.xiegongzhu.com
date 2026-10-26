package com.hotel.saas.booking;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrderDtos {
    @Data
    public static class CreateReq {
        private Long storeId;
        private String source = "WALK_IN";
        private String channel;
        private Long roomTypeId;
        private Long roomId;
        private Long memberId;
        private String guestName;
        private String guestPhone;
        private String idNo;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private Integer roomCount = 1;
        private String payType = "ARRIVAL";
        private String company;
        private String remark;
        private BigDecimal discount;
    }

    @Data
    public static class CheckinReq {
        private Long roomId;
        private String guestName;
        private String idNo;
        private BigDecimal deposit;
        private String depositPayType = "WECHAT";
        private String cardNo;
        private String minorInfo;
    }

    @Data
    public static class CheckoutReq {
        private BigDecimal discount;
        private BigDecimal otherFee;
        private BigDecimal depositDeduct;
        private String payType = "WECHAT";
        private String remark;
    }

    @Data
    public static class ChangeRoomReq {
        private Long roomId;
        private String remark;
    }

    @Data
    public static class ExtendReq {
        private LocalDate newCheckOut;
        private String remark;
    }

    @Data
    public static class DepositReq {
        private BigDecimal amount;
        private String type = "COLLECT";
        private String payType = "WECHAT";
    }
}
