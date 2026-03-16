package com.sk.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Component
@ConfigurationProperties(prefix = "mip")
public class MipConfig {

    private Map<String, String> companyUrls = new HashMap<>();

    public void setCompanyUrls(Map<String, String> companyUrls) {
        this.companyUrls = companyUrls;
    }

    /**
     * 회사 코드에 해당하는 MIP URL을 반환합니다.
     *
     * @param companyCd 회사 코드
     * @return 설정된 URL, 없으면 null
     */
    public String getMipUrlByCompanyCd(String companyCd) {
        if (companyCd == null || companyUrls == null) {
            return null;
        }
        return companyUrls.get(companyCd);
    }

    /**
     * 회사 코드에 대한 설정이 있는지 확인합니다.
     *
     * @param companyCd 회사 코드
     * @return 설정 존재 여부
     */
    public boolean hasMipUrl(String companyCd) {
        String url = getMipUrlByCompanyCd(companyCd);
        return url != null && !url.isBlank();
    }
}
