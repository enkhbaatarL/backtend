package com.sk.domain.admin.listener.handler;

import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.enums.AxpKnowledgeRegisterComponentType;

public interface AxpKnowledgeRegister {
    AxpKnowledgeRegisterComponentType getType();

    void process(AXUploadFileEvent event, String datasourceId, GoodsData goodsData);
}
