package com.sk.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareCustomizer implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        // todo Auditing
        return null;

    }

}
