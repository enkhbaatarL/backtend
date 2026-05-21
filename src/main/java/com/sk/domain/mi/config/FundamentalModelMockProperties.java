package com.sk.domain.mi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mi.fundamental-model.mock")
public class FundamentalModelMockProperties {

    private FundamentalModelCases fundamentalModelCases = new FundamentalModelCases();
    private FundamentalModelRuns fundamentalModelRuns = new FundamentalModelRuns();

    @Getter
    @Setter
    public static class FundamentalModelCases {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean listCases = true;
            private boolean createCase = true;
            private boolean run = true;
            private boolean getStatus = true;
            private boolean reset = true;
        }
    }

    @Getter
    @Setter
    public static class FundamentalModelRuns {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean callback = true;
        }
    }

}
