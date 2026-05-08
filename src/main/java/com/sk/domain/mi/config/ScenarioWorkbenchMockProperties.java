package com.sk.domain.mi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mi.scenario-workbench.mock")
public class ScenarioWorkbenchMockProperties {

    private Events events = new Events();
    private Scenarios scenarios = new Scenarios();
    private Overview overview = new Overview();

    @Getter
    @Setter
    public static class Events {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean getList = false;
            private boolean getDetail = false;
            private boolean getRecords = false;
            private boolean getOutlooks = false;
        }
    }

    @Getter
    @Setter
    public static class Scenarios {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean getList = false;
            private boolean getDetail = false;
        }
    }

    @Getter
    @Setter
    public static class Overview {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean getEvents = false;
            private boolean getScenarios = false;
            private boolean getPriceForecasts = false;
        }
    }

}
