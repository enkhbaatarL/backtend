package com.sk.domain.mi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mi.next-gen.mock")
public class NextGenMockProperties {

    private Events events = new Events();
    private ScenarioGroups scenarioGroups = new ScenarioGroups();
    private ModelExecutions modelExecutions = new ModelExecutions();
    private Design design = new Design();
    private PriceInterpretations priceInterpretations = new PriceInterpretations();

    @Getter
    @Setter
    public static class PriceInterpretations {
        private Api api = new Api();
        /** Agent 콜백(생성) 도착까지의 시뮬레이션 지연(초). 0이면 즉시(동기). */
        private long callbackDelaySeconds = 20;

        @Getter
        @Setter
        public static class Api {
            private boolean generate = false;
        }
    }

    @Getter
    @Setter
    public static class Design {
        private Api api = new Api();
        /** Agent 콜백(생성/검토/Signpost) 도착까지의 시뮬레이션 지연(초). 0이면 즉시(동기). */
        private long callbackDelaySeconds = 15;

        @Getter
        @Setter
        public static class Api {
            private boolean start = false;
            private boolean getState = false;
            private boolean getDimensionTemplates = false;
            private boolean validate = false;
            private boolean createSignposts = false;
            private boolean confirm = false;
            private boolean reset = false;
        }
    }

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
            private boolean designAvailability = false;
        }
    }

    @Getter
    @Setter
    public static class ScenarioGroups {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean getList = false;
            private boolean getScenarios = false;
            private boolean getGraph = false;
            private boolean getDetail = false;
            private boolean getScenarioDetail = false;
            private boolean updateScenarioStatus = false;
            private boolean deleteGroup = false;
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
            private boolean submit = false;
            private boolean getStatus = false;
        }

        @Getter
        @Setter
        public static class Workflow {
            private boolean dispatchSuccess = true;
            private long runningTimeoutMinutes = 10;
            private long autoCompleteSeconds = 5;
        }
    }

}
