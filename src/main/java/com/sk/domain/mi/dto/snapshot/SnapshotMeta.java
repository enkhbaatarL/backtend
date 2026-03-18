package com.sk.domain.mi.dto.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotMeta {
    private String emailSubject;
    private String subInfoText;
    private String coverImageUrl;
}
