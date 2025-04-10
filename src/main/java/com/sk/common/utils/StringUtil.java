package com.sk.common.utils;

public class StringUtil {

    private StringUtil() {}

    public static String stripNewlinesAndTabs(String inputStr) {
        if (inputStr == null) return null;
        return inputStr.replaceAll("[\r\n]+", "");
    }

    public static String maskAll(String input) {
        return "*".repeat(Math.max(8, input.length()));
    }

    public static String maskPartial(String input, int prefix, int suffix) {
        if (input == null || input.length() <= prefix + suffix) return "*".repeat(input.length());
        StringBuilder sb = new StringBuilder();
        sb.append(input, 0, prefix);
        sb.append("*".repeat(input.length() - prefix - suffix));
        sb.append(input, input.length() - suffix, input.length());
        return sb.toString();
    }

    public static String formatPageNumber(int pageNumber) {
        if (pageNumber < 0 || pageNumber > 999) {
            throw new IllegalArgumentException("pageNumber must be between 0 and 999");
        }
        return String.format("page_%03d", pageNumber);
    }

}
