package com.sk.domain.shared.client.dto.request;

import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public record FileCryptoEncryptReq(
        String operationUserId,
        String labelId,
        UUID fid,
        String pid) {

    public MultiValueMap<String, String> toFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("operationUserId", operationUserId);
        formData.add("LableID", labelId);
        formData.add("FID", fid.toString());
        formData.add("PID", pid);
        return formData;
    }
}
