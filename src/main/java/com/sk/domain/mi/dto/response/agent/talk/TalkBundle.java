package com.sk.domain.mi.dto.response.agent.talk;

import java.util.List;

public record TalkBundle(

    DialogType dialogType,
    String content,
    List<String> timestamps,
    Series series

) {
}
