package com.sk.common.utils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StringUtil {

    private StringUtil() {
    }

    public static String stripNewlinesAndTabs(String inputStr) {
        if (inputStr == null) return null;
        return inputStr.replaceAll("[\r\n]+", "");
    }

    public static String maskAll(String input) {
        if (input == null) return null;
        return "*".repeat(Math.max(8, input.length()));
    }

    // 26.07.28 취약점 진단 수정, '누락된 널 값 검사-102116'
    public static String maskPartial(String input, int prefix, int suffix) {
        if (input == null) return null;
        if (input.length() <= prefix + suffix) return "*".repeat(input.length());
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

    public static Pair<String, String> extractBucketAndPath(String url) {
        if (url == null)
            return null;

        int baseIndex = url.indexOf(".com/");
        if (baseIndex == -1 || baseIndex + 5 >= url.length())
            return null;

        String fullPath = url.substring(baseIndex + 5); // ex: skt-mi-d-osb-ss-skhy/TEMP/abc.pdf
        int firstSlash = fullPath.indexOf("/");

        if (firstSlash == -1 || firstSlash + 1 >= fullPath.length())
            return null;

        String bucket = fullPath.substring(0, firstSlash);
        String path = fullPath.substring(firstSlash + 1);

        return new ImmutablePair<>(bucket, path);
    }

    public static String stripFileExtension(String path) {
        if (path == null)
            return null;

        int lastDotIndex = path.lastIndexOf('.');
        int lastSlashIndex = path.lastIndexOf('/');

        // 확장자가 있고, 그 위치가 마지막 슬래시 뒤에 있을 때만 제거
        if (lastDotIndex > lastSlashIndex) {
            return path.substring(0, lastDotIndex);
        }
        return path;
    }

    public static String joinPoundSignNonNull(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        return Arrays.stream(parts)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("#"));
    }

    public static String removeDuplicates(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] items = input.split(",");
        Set<String> unique = new LinkedHashSet<>();
        for (String item : items) {
            unique.add(item);
        }
        // LinkedHashSet 에 담긴 순서 그대로 다시 콤마로 연결
        return String.join(",", unique);
    }

}
