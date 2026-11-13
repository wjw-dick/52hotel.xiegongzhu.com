package com.hotel.saas.configcenter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.saas.common.*;
import com.hotel.saas.domain.StorageConfig;
import com.hotel.saas.mapper.StorageConfigMapper;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final StorageConfigMapper storageMapper;
    private final CryptoService crypto;

    @Value("${app.upload.dir}")
    private String uploadDir;
    @Value("${app.storage.local-base-url}")
    private String baseUrl;

    public StorageConfig config(Long tenantId) {
        StorageConfig c = storageMapper.selectOne(new LambdaQueryWrapper<StorageConfig>()
                .eq(StorageConfig::getTenantId, tenantId).last("limit 1"));
        if (c == null) {
            c = new StorageConfig();
            c.setTenantId(tenantId);
            c.setDriver("LOCAL");
            c.setConfigJson("{}");
            c.setEnabled(1);
            storageMapper.insert(c);
        }
        return c;
    }

    public Map<String, Object> safeConfig(Long tenantId) {
        StorageConfig c = config(tenantId);
        JSONObject json = JSONUtil.parseObj(c.getConfigJson() == null ? "{}" : c.getConfigJson());
        for (String key : new String[]{"secretKey", "accessKey"}) {
            if (json.containsKey(key) && json.getStr(key) != null && !json.getStr(key).isBlank()) {
                String plain = crypto.decrypt(json.getStr(key));
                json.set(key, plain.length() > 4 ? plain.substring(0, 2) + "******" + plain.substring(plain.length() - 2) : "******");
            }
        }
        return Map.of("id", c.getId(), "driver", c.getDriver(), "enabled", c.getEnabled(), "config", json);
    }

    public void save(Long tenantId, String driver, JSONObject body) {
        StorageConfig c = config(tenantId);
        JSONObject old = JSONUtil.parseObj(c.getConfigJson() == null ? "{}" : c.getConfigJson());
        JSONObject merged = JSONUtil.createObj();
        merged.putAll(body);
        for (String key : new String[]{"secretKey", "accessKey"}) {
            String val = body.getStr(key);
            if (val != null && val.contains("******") && old.containsKey(key)) {
                merged.set(key, old.getStr(key));
            } else if (val != null && !val.isBlank()) {
                merged.set(key, crypto.encrypt(val));
            }
        }
        c.setDriver(driver);
        c.setConfigJson(merged.toString());
        c.setEnabled(1);
        if (c.getId() == null) storageMapper.insert(c);
        else storageMapper.updateById(c);
    }

    public String upload(Long tenantId, MultipartFile file, String category) {
        if (file == null || file.isEmpty()) throw new BizException("请选择文件");
        if (file.getSize() > MAX_IMAGE_SIZE) throw new BizException("图片大小不能超过 5MB");
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        if (!IMAGE_EXTENSIONS.contains(ext)) throw new BizException("仅支持 JPG / PNG / WebP / GIF 格式图片");
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String object = tenantId + "/" + (category == null ? "common" : category) + "/" + month + "/" + IdUtil.fastSimpleUUID() + ext;
        StorageConfig c = config(tenantId);
        try {
            if ("LOCAL".equals(c.getDriver())) {
                File dest = Path.of(uploadDir, object).toFile();
                FileUtil.mkParentDirs(dest);
                file.transferTo(dest);
                return baseUrl + "/uploads/" + object;
            }
            JSONObject cfg = JSONUtil.parseObj(c.getConfigJson());
            String endpoint = cfg.getStr("endpoint");
            String accessKey = crypto.decrypt(cfg.getStr("accessKey"));
            String secretKey = crypto.decrypt(cfg.getStr("secretKey"));
            String bucket = cfg.getStr("bucket");
            MinioClient client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            try (InputStream in = file.getInputStream()) {
                client.putObject(PutObjectArgs.builder().bucket(bucket).object(object)
                        .stream(in, file.getSize(), -1).contentType(file.getContentType()).build());
            }
            String domain = cfg.getStr("domain");
            return (domain == null || domain.isBlank() ? endpoint + "/" + bucket : domain) + "/" + object;
        } catch (Exception e) {
            log.error("上传对象存储失败", e);
            throw new BizException("上传失败：" + e.getMessage());
        }
    }

    public boolean test(String driver, JSONObject cfg) {
        if ("LOCAL".equals(driver)) {
            File dir = new File(uploadDir);
            return dir.exists() || dir.mkdirs();
        }
        try {
            String endpoint = cfg.getStr("endpoint");
            String accessKey = crypto.decrypt(cfg.getStr("accessKey"));
            String secretKey = crypto.decrypt(cfg.getStr("secretKey"));
            String bucket = cfg.getStr("bucket");
            if (endpoint == null || accessKey == null || secretKey == null || bucket == null) return false;
            MinioClient client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
            return client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            return false;
        }
    }

    public void delete(Long tenantId, String url) {
        StorageConfig c = config(tenantId);
        if (!"LOCAL".equals(c.getDriver()) || url == null) return;
        String prefix = baseUrl + "/uploads/";
        if (url.startsWith(prefix)) {
            File f = Path.of(uploadDir, url.substring(prefix.length())).toFile();
            FileUtil.del(f);
        }
    }
}
