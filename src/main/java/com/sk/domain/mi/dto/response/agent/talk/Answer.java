package com.sk.domain.mi.dto.response.agent.talk;

import com.sk.common.model.TableRes;
import com.sk.domain.mi.dto.response.agent.StreamRes;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record Answer(
    @Schema(description = "메시지")
    String  message,

    @Schema(description = "시계열(차트) 데이터")
    List<Series> series,

    @Schema(description = "테이블 데이터")
    List<TableRes> tables
) {
    public static Answer of(ModelAgentRes.Output message) {
        return Answer.builder()
            .message(message.content().text())
            .build();
    }

    public static Answer of(ModelAgentStreamBody modelAgentStreamBody){
        return Answer.builder()
                .message(modelAgentStreamBody.getValueAsString())
                .build();
    }

    public static Answer of(AnswerRes answerRes){

        StreamRes streamRes = (StreamRes) answerRes.message();
        return Answer.builder()
            .message(String.valueOf(streamRes.data()))
            .build();
    }

    public static Answer of(String message) {
        return Answer.builder().message(message).build();
    }
}
