package com.sk.domain.admin.util;

import com.sk.domain.admin.dto.CodeData;
import java.util.List;

public class GoodsUtil {

    public static String getLanguage(List<? extends CodeData> codeDataList){
        if(codeDataList == null || codeDataList.isEmpty()){
            return getDefaultLanguage();
        }
        String language = null;
        for(CodeData code : codeDataList){
            if(code == null) continue;
            language = getLanguageFromCode(code.codeId());
        }
        return language == null ? getDefaultLanguage() : language;
    }

    private static String getLanguageFromCode(String codeId){
        if(codeId == null) return null;
        return switch (codeId.trim()) {
            case "A_ENG" -> "English";
            case "A_KOR" -> "Korean";
            case "A_OTHERS" -> "Other";
            default -> null;
        };
    }

    private static String getDefaultLanguage(){
        return "English";
    }


}
