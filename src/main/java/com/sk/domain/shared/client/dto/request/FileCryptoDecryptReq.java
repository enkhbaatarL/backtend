package com.sk.domain.shared.client.dto.request;

import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public record FileCryptoDecryptReq(
        String userId,
        UUID fid,
        String pid) {

    public MultiValueMap<String, String> toFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("UserID", userId);
        formData.add("FID", fid.toString());
        formData.add("PID", pid);
        return formData;
    }
}
