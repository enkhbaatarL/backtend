package com.sk.domain.mi.dto.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotSection {
    private String id;
    private String type;
    private String title;
    private Integer order;
    private Boolean visibility;
    private String target;
    private Boolean isCollapsed;
    private List<SnapshotBlock> blocks;
}
