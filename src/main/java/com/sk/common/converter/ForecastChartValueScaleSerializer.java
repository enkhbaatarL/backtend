package com.sk.common.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@JsonComponent
public class ForecastChartValueScaleSerializer extends JsonSerializer<Double> {

    private static final int COMPANY_NO_2015 = 2015;

    @Override
    public void serialize(Double doubleVal, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
        if (doubleVal == null) {
            jsonGenerator.writeNull();
            return;
        }

        int scale = resolveScaleFromRequest();
        BigDecimal scaled = BigDecimal.valueOf(doubleVal).setScale(scale, RoundingMode.HALF_UP);
        jsonGenerator.writeString(scaled.toPlainString());
    }

    private int resolveScaleFromRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return 2;
            }
            HttpServletRequest request = attributes.getRequest();
            if (request == null) {
                return 2;
            }
            Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (!(variables instanceof Map<?, ?> map)) {
                return 2;
            }
            Object companyNoRaw = map.get("companyNo");
            if (companyNoRaw == null) {
                return 2;
            }
            int companyNo = Integer.parseInt(companyNoRaw.toString());
            return companyNo == COMPANY_NO_2015 ? 3 : 2;
        } catch (Exception ignored) {
            return 2;
        }
    }
}
