package com.hotel.saas.common;

import lombok.Data;

@Data
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static ApiResult<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
