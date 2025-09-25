package com.sk.domain.admin.service.excel;

import com.sk.domain.shared.enums.ComponentType;
import java.util.List;

public interface DataCollectorStrategy {

    ComponentType getComponentType();
    List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt, String category);
    void postAction();
}
