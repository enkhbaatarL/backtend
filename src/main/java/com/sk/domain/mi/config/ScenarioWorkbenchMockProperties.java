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
    private ModelExecutions modelExecutions = new ModelExecutions();
    private Design design = new Design();

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

    @Getter
    @Setter
    public static class Design {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean getEvents = false;
            private boolean postGenerate = false;
            private boolean postCallback = false;
            private boolean getGeneratedScenarios = false;
            private boolean getStatuses = false;
        }
    }

    @Getter
    @Setter
    public static class ModelExecutions {
        private Api api = new Api();
        private Workflow workflow = new Workflow();

        @Getter
        @Setter
        public static class Api {
            private boolean getParameters = false;
            private boolean submit = false;
            private boolean getStatus = false;
            private boolean callback = false;
        }

        @Getter
        @Setter
        public static class Workflow {
            private boolean dispatchSuccess = true;
            private long runningTimeoutMinutes = 10;
        }
    }

}
