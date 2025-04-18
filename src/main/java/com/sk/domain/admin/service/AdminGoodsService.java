package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.GoodsCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.response.provider.GoodsCreateRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.Role;
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
    public PaginationRes<GoodsRes> getAllGoods(int companyNo, Role role, int page, int size, String keyword, int providerNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 프로바이더에 속한 모든 상품 조회
            return PaginationRes.toRes(goodsRepository.findGoodsByProviderNoForAdmin(providerNo, keyword, pageable));
        } else if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 고객사에 등록 된 프로바이더에 속한 모든 상품 조회
            return PaginationRes.toRes(goodsRepository.findGoodsByProviderNoForOperator(companyNo, providerNo, keyword, pageable));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
    }

    /**
     * 상품을 등록한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param GoodsCreateReq 상품 데이터
     * @return Goods
     */
    @Transactional
    public GoodsCreateRes createGoods(int providerNo, GoodsCreateReq dto) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        return GoodsCreateRes
                .of(goodsRepository.save(Goods.of(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic(), provider)));
    }

    /**
    * 상품을 상세 조회한다.
    *
    * @param user 사용자 정보
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return GoodsDetailRes
    */
    public GoodsDetailRes getGoods(int companyNo, Role role, int providerNo, int goodsNo) {
        if (RoleUtil.isAdmin(role)) {
            // 관리자: 프로바이더에 속한 상품 조회
            return goodsRepository.findGoodsDetail(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));
        } else if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 고객사에 등록 된 프로바이더에 속한 권한 있는 상품 조회

            // 프로바이더 등록 유무 체크
            customerCompanyProviderMappingRepository
                    .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            // 상품 권한 유무 체크
            customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            return goodsRepository.findGoodsDetail(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
    }

    /**
    * 상품을 수정한다.
    *
    * @param goodsNo 상품 번호
    * @param GoodsUpdateReq 상품 데이터
    * @return
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
    * @return
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
