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
public class SnapshotBlock {
    private String id;
    private String type;
    private String format;
    private Integer order;
    private Boolean visibility;
    private Boolean isCollapsed;
    private String title;
    private Boolean plotOnChart;
    private String target;
    private List<String> editableFields;
    private Map<String, Object> content;
}
