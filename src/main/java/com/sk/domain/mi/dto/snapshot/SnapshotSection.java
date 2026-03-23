package com.sk.domain.mi.dto.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
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
    private String code;
    private String name;
    private Integer order;
    private Boolean visibility;
    private String target;
    private Boolean removed;
    private Map<String, Object> fields;
    private Map<String, SnapshotFieldDef> fieldDefs;
    private List<SnapshotBlock> blocks;
}
