package com.hotel.saas.configcenter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hotel.saas.common.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StorageController {
    private final StorageService storageService;

    @GetMapping("/api/admin/storage")
    public ApiResult<Map<String, Object>> get() {
        return ApiResult.ok(storageService.safeConfig(TenantScope.requiredTenantId()));
    }

    @Data
    public static class StorageReq {
        private String driver = "LOCAL";
        private JSONObject config = JSONUtil.createObj();
    }

    @PostMapping("/api/admin/storage")
    public ApiResult<Void> save(@RequestBody StorageReq req) {
        storageService.save(TenantScope.requiredTenantId(), req.getDriver(), req.getConfig());
        return ApiResult.ok();
    }

    @PostMapping("/api/admin/storage/test")
    public ApiResult<Map<String, Object>> test(@RequestBody StorageReq req) {
        boolean ok = storageService.test(req.getDriver(), req.getConfig());
        return ApiResult.ok(Map.of("success", ok, "message", ok ? "连接测试通过" : "连接失败，请检查参数"));
    }

    @PostMapping("/api/admin/files/upload")
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(required = false) String category) {
        Long tid = TenantScope.requiredTenantId();
        String url = storageService.upload(tid, file, category);
        return ApiResult.ok(Map.of("url", url, "name", file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
    }

    @DeleteMapping("/api/admin/files")
    public ApiResult<Void> delete(@RequestParam String url) {
        storageService.delete(TenantScope.requiredTenantId(), url);
        return ApiResult.ok();
    }
}
