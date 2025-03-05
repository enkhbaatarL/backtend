package com.sk.domain.shared.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public record SampleRes(

        @JsonProperty("widget_group")
        int widgetGroup,

        double score,

        @JsonProperty("question_meta")
        QuestionMeta questionMeta,

        @JsonProperty("widget_group_based_results")
        WidgetGroupBasedResults widgetGroupBasedResults

) {

    @Builder
    public SampleRes(int widgetGroup, double score, QuestionMeta questionMeta, WidgetGroupBasedResults widgetGroupBasedResults) {
        this.widgetGroup = widgetGroup;
        this.score = score;
        this.questionMeta = questionMeta;
        this.widgetGroupBasedResults = widgetGroupBasedResults;
    }

    @Builder
    public record QuestionMeta(

            Ner ner,

            Eex eex,

            Nsm nsm

    ) {

        @Builder
        public record Ner(

            @JsonProperty("tagged_sentence")
            String taggedSentence,

            List<Entities> entities

        ) {

            @Builder
            public record Entities(

                    String entity,
                    String tag,
                    @JsonProperty("st_code")
                    int stCode
            ) {

                @Builder
                public Entities(String entity, String tag, int stCode) {
                    this.entity = entity;
                    this.tag = tag;
                    this.stCode = stCode;
                }

            }

        }

        @Builder
        public record Eex(

            String event,

            List<String> keywords

        ) {

        }

        @Builder
        public record Nsm(

            String sector,

            double sectorScore,

            @JsonProperty("sentiment_score")
            double sentimentScore

        ) {

        }

    }

    @Builder
    public record WidgetGroupBasedResults(
        Widget5 widget5,
        Widget6 widget6

    ) {

        @Builder
        public record Widget5(
                String name
        ) {

        }

        @Builder
        public record Widget6(
                String name
        ) {

        }

    }

}
