package com.sk.common.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.common.enums.ResultCode;
import java.util.UUID;
import lombok.Getter;

/** 알려진 검증 충돌만 전달한다. Agent 원문 대신 확인된 복구 식별자만 포함한다. */
@Getter
public class EventVerificationConflictException extends RuntimeException {
    private final ResultCode.Error errorCode;
    private final Data data;

    public EventVerificationConflictException(ResultCode.Error errorCode, Data data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }

    public record Data(
        @JsonProperty("error_code") int errorCode,
        @JsonProperty("error_name") String errorName,
        @JsonProperty("verification_id") UUID verificationId,
        @JsonProperty("mi_event_snapshot_id") Long miEventSnapshotId
    ) {}
}
