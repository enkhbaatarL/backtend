package com.sk.domain.shared.service.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Common 전용 초기화 */
@Profile("!dedicated")
@Component
public class DefaultSchedulerConfig implements SchedulerConfig{
    @Value("${mail.except-company-no:}")
    private String exceptCompanyNo;

    @Getter
    private List<Integer> exceptCompanyNoList;

    @PostConstruct
    public void initDefault(){
        if(exceptCompanyNo == null){
            exceptCompanyNoList = Collections.emptyList();
            return;
        }
        exceptCompanyNoList = Arrays.stream(exceptCompanyNo.split(","))
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
