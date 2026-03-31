package com.sk.domain.shared.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record ModelAgentReq(

    Input input

) {

    public static ModelAgentReq of(QnaReq.Insert talkReq, List<ChatbotTalkQna> histories,
        Integer companyNo, Integer userNo, Set<ContentProviderAccess> contentProviderAccesses,
        List<ContentProviderAccess> contentProviderVdbAccesses,
        boolean shouldWebSearch, boolean useMiDataset) {

        return ModelAgentReq.builder()
            .input(Input.of(
                talkReq, histories, companyNo, userNo,
                contentProviderAccesses.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(ContentProviderAccess::authGdsList)
                    .filter(java.util.Objects::nonNull)
                    .flatMap(Set::stream)
                    .distinct()
                    .toList(),
                contentProviderVdbAccesses,
                shouldWebSearch, useMiDataset, null))
            .build();
    }

    @Builder
    public record Input(

        @JsonProperty("user_query")
        String userQuery,

        @JsonProperty("company_code")
        String companyNo,

        @JsonProperty("user_num")
        Integer userNo,

        @JsonProperty("content_provider_rdb_accesses")
        ContentProviderRdbAccesses contentProviderRdbAccesses,

        @JsonProperty("content_provider_vdb_accesses")
        List<ContentProviderAccess> contentProviderVdbAccesses,

//        @JsonProperty("rdb_search_metadata_repo_id")
//        String rdbSearchMetadataRepoId,

        List<History> histories,

        @JsonProperty("search_sources")
        Set<String> searchSources

    ) {

        public static Input of(QnaReq.Insert talkReq, List<ChatbotTalkQna> histories,
            Integer companyNo, Integer userNo, List<Integer> authorizedProductNums,
            List<ContentProviderAccess> contentProviderVdbAccesses,
            boolean shouldWebSearch, boolean useMiDataset, String rdbSearchMetadataRepoId) {
            Set<String> searchSources = new HashSet<>();
            if(shouldWebSearch) searchSources.add("web");
            if(useMiDataset) {
                searchSources.add("rdb");
                searchSources.add("vdb");
            }
            List<ContentProviderAccess> safeVdbAccesses =
                contentProviderVdbAccesses == null ? List.of() : contentProviderVdbAccesses;
            return Input.builder()
                .userQuery(talkReq.question())
                .searchSources(searchSources)
                .histories(History.ofList(histories))
                .companyNo(String.valueOf(companyNo))
                .userNo(userNo)
                .contentProviderRdbAccesses(new ContentProviderRdbAccesses(authorizedProductNums))
                .contentProviderVdbAccesses(safeVdbAccesses)
                //.rdbSearchMetadataRepoId(rdbSearchMetadataRepoId)
                .build();
        }

        @Builder
        public record ContentProviderRdbAccesses(
            @JsonProperty("authorized_product_nums")
            List<Integer> authorizedProductNums
        ) {
            public ContentProviderRdbAccesses(List<Integer> authorizedProductNums) {
                this.authorizedProductNums = authorizedProductNums;
            }
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
                        String aiText;
                        try {
                            AnswerRes parsed = JsonUtil.fromJson(h.getChatbotAnswerCntt(), AnswerRes.class);
                            aiText = parsed.message() == null ? "" : parsed.message().toString();
                        } catch (Exception e) {
                            aiText = h.getChatbotAnswerCntt();
                        }
                        Ai ai1 = Ai.builder().text(aiText).build();
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
