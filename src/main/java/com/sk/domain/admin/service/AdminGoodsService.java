package com.sk.domain.admin.service;

import com.sk.domain.admin.dto.response.provider.*;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
     * [고객사] part 에서 사용되는 프로바이더에 등록된 상품 목록을 페이지네이션 없이 조회한다.
     *  - 사용여부 Y, 삭제여부 N
     *
     * @param providerNo 프로바이더 번호
     * @return List<GoodsLiteRes>
     */
    public List<GoodsLiteRes> getGoodsNames(Integer providerNo) {
        List<Goods> goods = goodsRepository.findByProvider_ProviderNoAndIsUsedTrueAndIsDeletedFalse(providerNo);
        return GoodsLiteRes.ofList(goods);
    }

    /**
    * 상품 목록을 페이지네이션 처리하여 조회한다.
    * 
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param page 조회할 페이지 번호 (min 1)
    * @param size 조회할 요소 사이즈 (min 1)
    * @param keyword 검색 키워드
    * @param providerNo 데이타 프로바이더 번호
    * @return PaginationRes<GoodsRes>
    */
    public PaginationRes<GoodsRes> getAllGoods(int companyNo, Role role, int page, int size, String keyword, int providerNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        switch (role) {
            case ADMIN:
                return PaginationRes.toRes(goodsRepository.findGoodsByProviderNoForAdmin(providerNo, keyword, pageable));

            case OPERATOR:
                return PaginationRes.toRes(goodsRepository.findGoodsByProviderNoForOperator(companyNo, providerNo, keyword, pageable));

            case API_SUB_ADMIN:
                return PaginationRes.toRes(goodsRepository.findGoodsByProviderNoForAdmin(providerNo, keyword, pageable));

            case API_PLUS_OPERATOR:
                return PaginationRes.toRes(goodsRepository.findGoodsByProviderNoForOperator(companyNo, providerNo, keyword, pageable));

            default:
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
     * 상품을 등록한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param GoodsCreateReq 상품 데이터
     * @return Goods
     */
    @Transactional
    public GoodsCreateRes createGoods(Role role, int providerNo, GoodsCreateReq dto) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        switch (role) {
            case ADMIN:
                return GoodsCreateRes
                        .of(goodsRepository.save(Goods.of(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic(), provider)));

            case API_SUB_ADMIN:
                return GoodsCreateRes
                        .of(goodsRepository.save(Goods.of(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic(), provider)));

            default:
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
    * 상품을 상세 조회한다.
    *
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return GoodsDetailRes
    */
    public GoodsDetailRes getGoods(int companyNo, Role role, int providerNo, int goodsNo) {

        switch (role) {
            case ADMIN:
                return goodsRepository.findGoodsDetail(providerNo, goodsNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            case OPERATOR:
                customerCompanyProviderMappingRepository
                        .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "provider no: %d".formatted(providerNo)));

                customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "goods no: %d".formatted(goodsNo)));

                return goodsRepository.findGoodsDetail(providerNo, goodsNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            case API_SUB_ADMIN:
                return goodsRepository.findGoodsDetail(providerNo, goodsNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            default:
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
    * 상품을 수정한다.
    *
    * @param goodsNo 상품 번호
    * @param GoodsUpdateReq 상품 데이터
    * @return 
    */
    @Transactional
    public void updateGoods(int companyNo, Role role, int providerNo, int goodsNo, GoodsUpdateReq dto) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

        if (goods.getIsPublic()) {
            if (role == Role.ADMIN) {
                goods.update(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic());
                return;
            }
        } else {
            if (role == Role.ADMIN) {
                goods.update("", "", "", dto.isUsed(), null);
                return;
            } else if (role == Role.OPERATOR) {
                customerCompanyProviderMappingRepository
                        .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "provider no: %d".formatted(providerNo)));

                customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                        .orElseThrow(
                                () -> AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "goods no: %d".formatted(goodsNo)));

                goods.update(dto.goodsNm(), dto.goodsId(), dto.goodsExpn(), dto.isUsed(), dto.isPublic());
                return;
            }
        }

        throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
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
