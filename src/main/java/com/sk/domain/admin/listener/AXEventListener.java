package com.sk.domain.admin.listener;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.listener.handler.AxpKnowledgeRegister;
import com.sk.domain.admin.listener.handler.AxpKnowledgeRegisterStrategyResolver;
import com.sk.domain.shared.entity.master.tcm.AxpRepository;
import com.sk.domain.shared.repository.master.tcm.AxpRepositoryRepository;
import org.springframework.context.event.EventListener;
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
    private final AxpRepositoryRepository axpRepositoryRepository;
    private final AxpKnowledgeRegisterStrategyResolver axpKnowledgeRegisterStrategyResolver;

    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploadFileEvent(AXUploadFileEvent event) {
        Integer goodsDataNo = event.getGoodsDataNo();

        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo).orElse(null);
        if (goodsData == null) {
            log.error("[AX Platform] GoodsData not found: goodsDataNo= {}", goodsDataNo);
            return;
        }

        String vectorRepositoryId = goodsData.getGoods().getVectorRepositoryId();
        if(vectorRepositoryId == null || vectorRepositoryId.isEmpty()){
            log.warn("[AX Platform] AXP Repository Id is Empty. GoodsNo={}, GoodsDataNo={}", goodsData.getGoods().getGoodsNo(), goodsDataNo);
            return;
        }

        AxpRepository axpRepository = axpRepositoryRepository.findById(vectorRepositoryId).orElseThrow(()-> AppErrorException.of(
            ResultCode.Error.SYS_INVALID_CONFIG,
            "AXP Repository not found. vectorRepositoryId=" + vectorRepositoryId));

        AxpKnowledgeRegister axpKnowledgeRegister = axpKnowledgeRegisterStrategyResolver.resolve(axpRepository.getComponentTypeId());
        axpKnowledgeRegister.process(event, axpRepository.getDataSourceId(), goodsData);
    }
}
