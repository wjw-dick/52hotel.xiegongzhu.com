package com.hotel.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hotel.saas.common.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class Tenant extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private String status;
    private String contact;
    private String phone;
    private LocalDate openTime;
    private LocalDate expireTime;
    private Integer maxUsers;
    private Integer maxRooms;
    private String logo;
    private String remark;
}
