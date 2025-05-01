package com.sk.domain.admin.service.excel;

import java.util.List;

public interface DataCollectorStrategy {
    List<Object[]> processData(Object data, List<String> actualColumns, String criteriaDt);
}
