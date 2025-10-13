package com.sk.domain.shared.service.config;

import java.util.List;

public interface SchedulerConfig {
    default List<Integer> getExceptCompanyNoList(){
        return null;
    };
    default List<Integer> getDedicatedCompanyNoList(){
        return null;
    };
}
