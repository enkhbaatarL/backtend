package com.sk.domain.admin.service.excel;

import com.sk.domain.shared.enums.ComponentType;
import java.util.List;
import java.util.Map;

public interface DataCollectorStrategy {

    ComponentType getComponentType();
    List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt,
        String category, Map<String, String> conditionMap, List<String> dbColumns);
    default void postAction(List<Object[]> values){};
}
