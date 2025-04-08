package com.sk.common.utils;

public class PathUtil {

    public static String IncludeTrailingPathDelimiter(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (path.endsWith("/")) {
            return path;
        }
        return path + "/";
    }
}
