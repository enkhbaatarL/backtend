package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.GoodsCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminGoodsService {
    private final ProviderRepository providerRepository;
    private final GoodsRepository goodsRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;

    /**
    * 상품 목록을 페이지네이션 처리하여 조회한다.
    *
    * @param user 사용자 정보
    * @param page 조회할 페이지 번호 (min 1)
    * @param size 조회할 요소 사이즈 (min 1)
    * @param keyword 검색 키워드
    * @param providerNo 데이타 프로바이더 번호
    * @return PaginationRes<GoodsRes>
    */
    public PaginationRes<GoodsRes> getAllGoods(User user, int page, int size, String keyword, int providerNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // 관리자
        if (RoleUtil.isAdmin(user)) {
            Page<GoodsRes> goodsPage = goodsRepository.findGoodsByProviderNoForAdmin(providerNo, keyword, pageable);
            return PaginationRes.toRes(goodsPage);
        }

        // 운영자
        if (RoleUtil.isOperator(user)) {
            Page<GoodsRes> goodsPage = goodsRepository.findGoodsByProviderNoForOperator(user.getCustomerCompany().getCustomerCompanyNo(),
                    providerNo,
                    keyword, pageable);
            return PaginationRes.toRes(goodsPage);
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED, "user role not found");
    }

    /**
     * 상품을 등록한다.
     *
     * @param ProviderCreateReq 데이타 프로바이더 등록 데이터
     * @return ProviderCreateRes
     */
    @Transactional
    public void createGoods(int providerNo, GoodsCreateReq dto) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        goodsRepository.save(Goods.of(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic(), provider));

        return;
    }

    /**
    * 상품을 상세 조회한다.
    *
    * @param user 사용자 정보
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return GoodsDetailRes
    */
    public GoodsDetailRes getGoods(User user, int providerNo, int goodsNo) {

        // 프로바이더 정보 확인
        providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        // 관리자
        if (RoleUtil.isAdmin(user)) {
            return goodsRepository.findGoodsDetailByGoodsNoForAdmin(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));
        }

        // 운영자
        if (RoleUtil.isOperator(user)) {
            // 고객사와 프로바이더 맵핑 정보 확인
            customerCompanyProviderMappingRepository.findByCustomerCompanyAndId_ProviderNoAndIsDeletedFalse(user.getCustomerCompany(), providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.REQ_METHOD_NOT_ALLOWED, "provider no: %d".formatted(providerNo)));

            // 고객사와 상품 맵핑 정보 확인
            customerCompanyGoodsMappingRepository
                    .findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(user.getCustomerCompany().getCustomerCompanyNo(), goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.REQ_METHOD_NOT_ALLOWED, "goods no: %d".formatted(goodsNo)));

            return goodsRepository.findGoodsDetailByGoodsNoForOperator(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED, "user role not found");
    }

    /**
    * 상품을 수정한다.
    *
    * @param goodsNo 상품 번호
    * @param GoodsUpdateReq 상품 등록 데이터
    * @return GoodsUpdateRes
    */
    @Transactional
    public void updateGoods(int goodsNo, GoodsUpdateReq dto) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

        goods.update(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic());

        return;
    }

    /**
    * 상품을 삭제한다.
    *
    * @param goodsNo 상품 번호
    * @return GoodsDeleteRes
    */
    @Transactional
    public void deleteGoods(int goodsNo) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

        goods.softDelete();

        return;
    }

    /**
    * 해당 상품의 최신 수정일 노출
    * 상품 정보 수정 업데이트 범위 :
    *
    * @param goodsNo 상품 번호
    * @return GoodsLatestUpdatedRes
    */
    public GoodsLatestUpdatedRes getGoodsLatestUpdated(int goodsNo) {
        return goodsRepository.findGoodsLatestUpdatedByGoodsNo(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));
    }
}
