package com.hotel.saas.common;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CryptoService {
    private final AES aes;

    public CryptoService(@Value("${app.jwt.secret}") String secret) {
        byte[] key = SecureUtil.md5(secret).getBytes(StandardCharsets.UTF_8);
        this.aes = SecureUtil.aes(key);
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return plain;
        return "ENC:" + aes.encryptBase64(plain);
    }

    public String decrypt(String cipher) {
        if (cipher == null || !cipher.startsWith("ENC:")) return cipher;
        try {
            return aes.decryptStr(cipher.substring(4));
        } catch (Exception e) {
            return "";
        }
    }

    public String mask(String plain) {
        String s = decrypt(plain);
        if (s == null || s.length() < 8) return s == null ? "" : "****";
        return s.substring(0, 4) + "**********" + s.substring(s.length() - 4);
    }
}
