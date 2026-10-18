package com.sk.domain.admin.listener;

import com.sk.domain.admin.listener.handler.AxpKnowledgeRegister;
import com.sk.domain.admin.listener.handler.AxpKnowledgeRegisterStrategyResolver;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AXEventListener {
    private final GoodsDataRepository goodsDataRepository;
    private final AxpKnowledgeRegisterStrategyResolver axpKnowledgeRegisterStrategyResolver;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploadFileEvent(AXUploadFileEvent event) {
        Integer goodsDataNo = event.goodsDataNo();

        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo).orElse(null);
        if (goodsData == null) {
            log.error("[AX Platform] GoodsData not found: goodsDataNo= {}", goodsDataNo);
            return;
        }

        String axpDataSourceId = goodsData.getGoods().getAxpDataSourceId();
        if(axpDataSourceId == null || axpDataSourceId.isEmpty()){
            log.warn("[AX Platform] AXP Repository Id is Empty. GoodsNo={}, GoodsDataNo={}", goodsData.getGoods().getGoodsNo(), goodsDataNo);
            return;
        }
        String axpUploadComponentId = goodsData.getGoods().getAxpUploadComponentId();
        if(axpUploadComponentId == null || axpUploadComponentId.isEmpty()){
            log.warn("[AX Platform] AXP Upload Component Id is Empty. GoodsNo={}, GoodsDataNo={}", goodsData.getGoods().getGoodsNo(), goodsDataNo);
            return;
        }

        AxpKnowledgeRegister axpKnowledgeRegister = axpKnowledgeRegisterStrategyResolver.resolve(axpUploadComponentId);
        axpKnowledgeRegister.process(event, axpDataSourceId, goodsData);
    }
}
