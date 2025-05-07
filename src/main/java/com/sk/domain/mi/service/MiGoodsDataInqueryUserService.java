package com.sk.domain.mi.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInqueryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInqueryUser;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.repository.master.tlm.GoodsDataInqueryUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiGoodsDataInqueryUserService {

    private final GoodsDataInqueryUserRepository goodsDataInquiryUserRepository;

    @Transactional
    public void createGoodsDataInquiryUser(GoodsData goodsData, String subject) {
        goodsDataInquiryUserRepository.save(GoodsDataInqueryUser.of(GoodsDataInqueryUserId.of(goodsData, subject)));
    }

    public List<RecentViewedRes> getGoodsDataInqueryUser(String userLoginId, Pageable pageable) {
        return goodsDataInquiryUserRepository.findById_UserLoginIdAndIsDeletedFalse(userLoginId, pageable);
    }
}
