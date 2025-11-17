package com.sk.domain.shared.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import org.apache.commons.lang3.ObjectUtils;

@Builder
public record ModelAgentReq(

    Input input

) {

    public static ModelAgentReq of(QnaReq.Insert talkReq, List<ChatbotTalkQna> histories, Integer companyNo, Integer userNo) {
        return ModelAgentReq.builder()
            .input(Input.of(talkReq, histories, companyNo, userNo))
            .build();
    }

    @Builder
    public record Input(

        String query,

        @JsonProperty("web_search")
        Boolean shouldWebSearch,

        List<History> histories,

        // 06-24 회사번호 변경요청(문자열로 요청받음..)
        @JsonProperty("company_code")
        String companyNo,

        @JsonProperty("user_num")
        Integer userNo

    ) {

        public static Input of(QnaReq.Insert talkReq, List<ChatbotTalkQna> histories, Integer companyNo, Integer userNo) {
            return Input.builder()
                .query(talkReq.question())
                .shouldWebSearch(ObjectUtils.defaultIfNull(talkReq.shouldWebSearch(), Boolean.TRUE))
                .histories(History.ofList(histories))
                .companyNo(String.valueOf(companyNo))
                .userNo(userNo)
                .build();
        }

        @Builder
        public record History(
            String user,
            List<Ai> ai
        ) {

            public static List<History> ofList(List<ChatbotTalkQna> histories) {
                return Optional.ofNullable(histories).orElse(List.of()).stream()
                    .filter(h -> h.getChatbotAnswerCntt() != null)
                    .map(h -> {
                        AnswerRes parsed = JsonUtil.fromJson(h.getChatbotAnswerCntt(), AnswerRes.class);
                        Ai ai1 = Ai.builder().text(parsed.message().toString()).build();
//                        var bundles = Optional.ofNullable(parsed)
//                            .map(AnswerRes::bundles)
//                            .orElse(List.of());
//
//                        List<Ai> aiList = bundles.stream()
//                            .map(b -> Ai.builder()
//                                .text(b.content())
//                                .chart(
//                                    Optional.ofNullable(b.series()).orElse(List.of()).stream()
//                                        .filter(Objects::nonNull)
//                                        .map(c -> ModelAgentRes.Output.Message.Content.Chart.builder()
//                                            .order(c.order())
//                                            .type(Optional.ofNullable(c.type()).map(Series.Type::getModelRes).orElse(null))
//                                            .legend(c.legend())
//                                            .date(c.timestamps())
//                                            .value(Optional.ofNullable(c.values())
//                                                .orElse(List.of())
//                                                .stream()
//                                                .map(BigDecimal::doubleValue)
//                                                .toList())
//                                            .build())
//                                        .toList())
//                                .build())
//                            .toList();

                        return History.builder()
                            .user(h.getUserQuestionCntt())
                            .ai(List.of(ai1))
                            .build();
                    })
                    .toList();
            }

            @Builder
            public record Ai(
                String text,
                List<ModelAgentRes.Output> chart
            ) {
            }


        }

    }


}
