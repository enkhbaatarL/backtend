package com.sk.domain.shared.service.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Dedicated 전용 초기화 */
@Profile("dedicated")
@Component
public class DedicatedSchedulerConfig implements SchedulerConfig{
    @Value("${dedicated.company-no:}")
    private String dedicatedCompanyNo;

    @Getter
    private List<Integer> dedicatedCompanyNoList;
    @PostConstruct
    public void initDedicated(){
        if(dedicatedCompanyNo == null) return;
        dedicatedCompanyNoList = Arrays.stream(dedicatedCompanyNo.split(","))
            .map(String::trim)
            .map(s -> {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return null; // 실패 시 null 반환
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
