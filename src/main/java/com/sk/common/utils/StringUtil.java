package com.sk.common.utils;

public class StringUtil {

    private StringUtil() {}

    public static String stripNewlinesAndTabs(String inputStr) {
        if (inputStr == null) return null;
        return inputStr.replaceAll("[\r\n]+", "");
    }

}
