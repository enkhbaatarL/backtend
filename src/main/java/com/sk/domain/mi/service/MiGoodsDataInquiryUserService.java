package com.sk.domain.mi.service;

import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInquiryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInquiryUser;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.repository.master.tlm.GoodsDataInquiryUserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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
        goodsDataInquiryUserRepository.save(
            GoodsDataInquiryUser.of(GoodsDataInquiryUserId.of(goodsData, subject)));
    }

    public List<RecentViewedRes> getGoodsDataInquiryUser(int companyNo, int userNo,
        String userLoginId, Pageable pageable) {
        return goodsDataInquiryUserRepository
            .findRecentViewdByUserLoginId(companyNo, userNo, userLoginId, pageable)
            .stream()
            .map(RecentViewedRes::of)
            .toList();
    }

    /**
     * 사용자의 상품 데이터 조회 이력 전체 삭제 (논리 삭제)
     *
     * @param userLoginId 사용자 로그인 ID
     */
    @Transactional
    public void clearGoodsDataInquiryUser(String userLoginId) {
        List<GoodsDataInquiryUser> inquiryUsers =
            goodsDataInquiryUserRepository.findByIdUserLoginIdAndIsDeletedFalse(userLoginId);

        inquiryUsers.forEach(GoodsDataInquiryUser::softDelete);
    }

}
