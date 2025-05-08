package com.sk.domain.shared.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KmsService {
    // URI
    private static String KMS_API_BASE_URI = "https://ocapi.ncloud.com";

    @Value("${ncp.kms.key-tag}")
    private String keyTag;

    @Value("${ncp.kms.access-key}")
    private String accessKey;

    @Value("${ncp.kms.secret-key}")
    private String secretKey;

    private JSONObject apiCaller(String endPoint, String data) throws Exception {
        HttpClient client = HttpClients.createDefault();

        long timestamp = System.currentTimeMillis();
        String signature = makeSignature(endPoint, timestamp);
        URIBuilder uriBuilder = new URIBuilder(KMS_API_BASE_URI);
        uriBuilder.setPath(endPoint);

        HttpPost request = new HttpPost(uriBuilder.build().toString());
        request.addHeader("x-ncp-apigw-timestamp", timestamp + "");
        request.addHeader("x-ncp-iam-access-key", accessKey);
        request.addHeader("x-ncp-apigw-signature-v2", signature);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(data));

        HttpResponse response = client.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        JSONParser parser = new JSONParser();
        JSONObject repsObj = (JSONObject) parser.parse(responseBody);
        JSONObject dataObj = (JSONObject) repsObj.get("data");
        return dataObj;
    }

    private String encodeToBase64(byte[] bytesToEncode) {
        return Base64.getEncoder().encodeToString(bytesToEncode);
    }

    private byte[] decodeFromBase64(String stringToDecode) {
        return Base64.getDecoder().decode(stringToDecode);
    }

    // API Gateway 인증용 API 호출 시그니처 생성 (API Gateway 사용 가이드 참조)
    private String makeSignature(String url, long timestamp) throws Exception {
        String message = new StringBuilder()
                .append("POST")
                .append(" ")
                .append(url)
                .append("\n")
                .append(timestamp)
                .append("\n")
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        String encodeBase64String = Base64.getEncoder().encodeToString(rawHmac);
        return encodeBase64String;
    }

    @SuppressWarnings("unchecked")
    public String encrypt(String plaintext) throws Exception {
        String endPoint = String.format("/kms/v1/keys/%s/encrypt", keyTag);

        JSONObject data = new JSONObject();
        String base64_plaintext = encodeToBase64(plaintext.getBytes(StandardCharsets.UTF_8));
        data.put("plaintext", base64_plaintext);

        JSONObject respJsonObject = apiCaller(endPoint, data.toJSONString());
        return respJsonObject.get("ciphertext").toString();
    }

    @SuppressWarnings("unchecked")
    public String decrypt(String ciphertext) throws Exception {
        String endPoint = String.format("/kms/v1/keys/%s/decrypt", keyTag);

        JSONObject data = new JSONObject();
        data.put("ciphertext", ciphertext);

        JSONObject respJsonObject = apiCaller(endPoint, data.toJSONString());
        String base64_plaintext = respJsonObject.get("plaintext").toString();

        return new String(decodeFromBase64(base64_plaintext), StandardCharsets.UTF_8);
    }
}
