package com.hotel.saas.auth;

import com.hotel.saas.common.ApiResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Data
    public static class LoginReq {
        private String username;
        private String password;
        private String tenantCode;
    }

    @Data
    public static class PasswordReq {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class WechatLoginReq {
        private String tenantCode;
        private String openId;
        private String unionId;
        private String nickname;
        private String phone;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody LoginReq req) {
        return ApiResult.ok(authService.login(req.getUsername(), req.getPassword(), req.getTenantCode()));
    }

    @PostMapping("/wechat-login")
    public ApiResult<Map<String, Object>> wechatLogin(@RequestBody WechatLoginReq req) {
        return ApiResult.ok(authService.wechatLogin(req.getTenantCode(), req.getOpenId(),
                req.getUnionId(), req.getNickname(), req.getPhone()));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        return ApiResult.ok(authService.profile());
    }

    @PostMapping("/password")
    public ApiResult<Void> password(@RequestBody PasswordReq req) {
        authService.changePassword(req.getOldPassword(), req.getNewPassword());
        return ApiResult.ok();
    }
}
