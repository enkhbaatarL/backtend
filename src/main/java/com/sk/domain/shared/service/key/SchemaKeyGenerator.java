package com.sk.domain.shared.service.key;

import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("schemaKeyGen")
public class SchemaKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        DynamicDataReq req = (DynamicDataReq) params[0];
        String firstCol = req.columns().split(",")[0].trim();
        return String.join(":", req.targetDataSource(), req.tableName(), firstCol);
    }

}
