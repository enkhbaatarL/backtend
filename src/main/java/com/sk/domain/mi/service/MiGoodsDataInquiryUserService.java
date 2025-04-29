package com.sk.domain.mi.service;

import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInquiryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInquiryUser;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.repository.master.tlm.GoodsDataInquiryUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiGoodsDataInquiryUserService {

    private final GoodsDataInquiryUserRepository goodsDataInquiryUserRepository;

    @Transactional
    public void createGoodsDataInquiryUser(GoodsData goodsData, String subject) {
        goodsDataInquiryUserRepository.save(GoodsDataInquiryUser.of(GoodsDataInquiryUserId.of(goodsData, subject)));
    }

}
