package com.sk.domain.admin.listener.event;

import com.sk.domain.shared.client.dto.AXMetaFileDto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AXUploadFileEvent {
    private final Integer goodsDataNo;
    private final Boolean isNew;
}
