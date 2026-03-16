package com.sk.domain.mi.dto.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotRoot {
    private Integer schemaVersion;
    private Long templateId;
    private Integer newsletterId;
    private Integer issueNo;
    private LocalDateTime publishedAt;
    private SnapshotMeta meta;
    private List<SnapshotSection> sections;
}
