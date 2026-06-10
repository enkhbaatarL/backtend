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
    public static class ScenarioGroups {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private boolean getList = false;
            private boolean getScenarios = false;
            private boolean getGraph = false;
            private boolean getGraphExport = false;
            private boolean getDetail = false;
            private boolean getScenarioDetail = false;
            private boolean updateScenarioStatus = false;
            private boolean bulkUpdateScenarioStatus = false;
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
            private long autoCompleteSeconds = 5;
        }
    }

}
