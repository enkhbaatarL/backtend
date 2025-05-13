package com.sk.common.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class NcpSignerUtil {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final String SPACE = " ";
    private static final String NEWLINE = "\n";


    /**
     * @param method     GET, POST
     * @param urlPath    /api/v1/secrets/{id}/values?stage=active  (도메인 제외, 쿼리 포함)
     * @param timestamp  String.valueOf(System.currentTimeMillis())
     * @param accessKey  x-ncp-iam-access-key
     * @param secretKey  서브 계정 Secret Key
     */
    public static String sign(String method, String urlPath, String timestamp, String accessKey, String secretKey) throws Exception {

        String msg = method + SPACE + urlPath + NEWLINE + timestamp + NEWLINE + accessKey;

        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALG);
        Mac mac = Mac.getInstance(HMAC_ALG);
        mac.init(key);
        byte[] raw = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

}
