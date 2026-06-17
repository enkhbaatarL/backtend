package com.sk.domain.shared.client.dto.response;

import java.util.UUID;

public record FileCryptoUploadRes(
        int result,
        String errorMsg,
        UUID fid) {
}
