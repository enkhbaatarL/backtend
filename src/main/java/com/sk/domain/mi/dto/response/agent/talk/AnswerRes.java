package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record AnswerRes(

        List<MessageBundle> bundles

) {

    public static AnswerRes of(ModelAgentRes modelAgentRes, Map<Integer, GoodsDataSourceDetail> gdsMap) {
        return AnswerRes.builder()
                .bundles(modelAgentRes.output().messages()
                        .stream()
                        .map(message -> MessageBundle.of(message, gdsMap))
                        .toList())
                .build();
    }

}
