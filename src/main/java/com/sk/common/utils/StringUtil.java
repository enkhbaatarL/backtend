package com.sk.common.utils;

public class StringUtil {

    private StringUtil() {}

    public static String stripNewlinesAndTabs(String inputStr) {
        return inputStr.replaceAll("[\r\n]+", "");
    }

}
