package com.sk.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

@Slf4j
public class MDCUtil {

    private static final MDCAdapter MDC_ADAPTER = MDC.getMDCAdapter();

    /**
     * Request Sequence Id.
     */
    public static final String REQUEST_SEQ_ID = "REQUEST_SEQ_ID";

    /**
     * REQUEST_METHOD.
     */
    public static final String REQUEST_METHOD = "REQUEST_METHOD";

    /**
     * Request UUID.
     */
    public static final String REQUEST_UUID = "REQUEST_UUID";

    /**
     * REQUEST_START_TIME.
     */
    public static final String REQUEST_START_TIME = "REQUEST_START_TIME";

    /**
     * REQUEST_URI.
     */
    public static final String REQUEST_URI = "REQUEST_URI";

    /**
     * REQUEST_HEADER_MAP.
     */
    public static final String HEADER_MAP = "HEADER_MAP";

    /**
     * REQUEST_BODY.
     */
    public static final String REQUEST_BODY = "REQUEST_BODY";

    /**
     * REQUEST_QUERY_STRING_MAP.
     */
    public static final String REQUEST_QUERY_STRING = "REQUEST_QUERY_STRING";

    /**
     * REQUEST_USER_ID.
     */
    public static final String REQUEST_USER_ID = "REQUEST_USER_ID";

    /**
     * RESPONSE_ERROR_MSG.
     */
    public static final String RESPONSE_ERROR_MSG = "RESPONSE_ERROR_MSG";

    /**
     * ACCESS_IP_ADDRESS.
     */
    public static final String ACCESS_IP_ADDRESS = "ACCESS_IP_ADDRESS";

    /**
     * REQUEST_X_COMPANY_NO.
     */
    public static final String REQUEST_X_COMPANY_NO = "REQUEST_X_COMPANY_NO";

    /**
     * Set Value.
     *
     * @param key   the key
     * @param value the value
     */
    public static void setValue(String key, String value) {
        MDC_ADAPTER.put(key, value);
    }

    /**
     * Sets json value.
     *
     * @param key   the key
     * @param value the value
     */
    public static void setJsonValue(String key, Object value) {
        if (value != null) {
            MDC_ADAPTER.put(key, JsonUtil.getInstance().toJson(value));
        }
    }

    /**
     * Get string.
     *
     * @param key the key
     * @return the string
     */
    public static String getValue(String key) {
        return MDC_ADAPTER.get(key);
    }

    /**
     * MUC Apter Clear.
     */
    public static void clear() {
        MDC_ADAPTER.clear();
    }

    public static MDCAdapter getAdapter() {
        return MDC_ADAPTER;
    }

}
