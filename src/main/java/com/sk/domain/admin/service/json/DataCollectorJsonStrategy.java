package com.sk.domain.admin.service.json;

import com.sk.domain.shared.enums.ComponentType;
import java.util.List;

public interface DataCollectorJsonStrategy {

    ComponentType getComponentType();
    List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category);
}
