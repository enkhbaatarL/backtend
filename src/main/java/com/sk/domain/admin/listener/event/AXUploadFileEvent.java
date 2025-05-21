package com.sk.domain.admin.listener.event;

import com.sk.domain.shared.client.dto.AXMetaFileDto;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AXUploadFileEvent {
    private final GoodsData goodsData;
    private final AXMetaFileDto axMetaFileDto;
    private final Boolean isUpdate;

}
