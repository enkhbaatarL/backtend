package com.sk.domain.admin.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.enums.ComponentType;
import java.util.List;

public interface DataCollectorJsonStrategy {

    ComponentType getComponentType();
    List<Object[]> processJsonData(JsonNode data, List<String> actualColumns, String criteriaDt, String category);
}
