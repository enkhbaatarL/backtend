package com.sk.domain.shared.client.dto.request;

import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public record FileCryptoStatusReq(
        UUID fid,
        String pid) {

    public MultiValueMap<String, String> toFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("fID", fid.toString());
        formData.add("pID", pid);
        return formData;
    }
}
