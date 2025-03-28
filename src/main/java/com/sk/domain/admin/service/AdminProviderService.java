package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.provider.GoodsDataCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataReq;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataReq;
import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.admin.dto.response.provider.GoodsDeleteRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.GoodsUpdateRes;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDeleteRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.dto.response.provider.ProviderUpdateRes;
import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminProviderService {
    private final CommonCodeDetailRepository commonCodeDetailRepository;
    private final ProviderRepository providerRepository;
    private final GoodsRepository goodsRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final AdminFileService adminFileService;

    /**
     * 데이타 프로바이더 목록을 조회한다.
     */
    public List<ProviderNameRes> getAllProviders() {
        return providerRepository.findNameByIsDeletedFalseOrderByProviderNmAsc();
    }

    /**
     * 데이타 프로바이더 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @return PaginationRes<ProviderRes>
     */
    public PaginationRes<ProviderRes> getAllProviders(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<ProviderRes> providerPage = providerRepository.searchProviders(pageable);

        return PaginationRes.toRes(providerPage);
    }

    /**
     * 데이타 프로바이더를 등록한다.
     *
     * @param ProviderCreateReq 데이타 프로바이더 등록 데이터
     * @return ProviderCreateRes
     */
    @Transactional
    public ProviderCreateRes createProvider(ProviderCreateReq dto) {
        Provider provider = providerRepository.save(Provider.of(dto));

        return ProviderCreateRes.of(provider);
    }

    /**
     * 데이타 프로바이더를 상세 조회한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderDetailRes
     */
    public ProviderDetailRes getPrivider(int providerNo) {
        return providerRepository.findByProviderNoAndIsDeletedFalseWithUser(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
    }

    /**
     * 데이타 프로바이더를 수정한다.
     *
     * @param ProviderUpdateReq 데이타 프로바이더 등록 데이터
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderUpdateRes
     */
    @Transactional
    public ProviderUpdateRes updateProvider(ProviderUpdateReq dto, int providerNo) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

        provider.update(dto);

        return ProviderUpdateRes.of(provider);
    }

    /**
     * 데이타 프로바이더를 삭제한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderDeleteRes
     */
    @Transactional
    public ProviderDeleteRes deleteProvider(int providerNo) {
        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

        provider.softDelete();

        return ProviderDeleteRes.of(provider);
    }

    /**
     * 해당 프로바이더의 최신 수정일 노출
     * 프로바이더 정보 수정 업데이트 범위 : 프로바이더 정 보 등록/수정, 상품 등록/수정/삭제
     *
     * @param providerNo 데이타 프로바이더 번호
     * @return ProviderLatestUpdatedRes
     */
    public ProviderLatestUpdatedRes getProviderLatestUpdated(int providerNo) {
        return providerRepository.findByProviderNoAndIsDeletedFalseWithGoodsLatestModifyInfo(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
    }

    /**
     * 상품 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page       조회할 페이지 번호 (min 1)
     * @param size       조회할 요소 사이즈 (min 1)
     * @param keyword 검색 키워드
     * @param providerNo 데이타 프로바이더 번호
     * @return PaginationRes<GoodsRes>
     */
    public PaginationRes<GoodsRes> getAllGoods(int page, int size, String keyword, int providerNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<GoodsRes> goodsPage = goodsRepository.searchGoods(providerNo, keyword, pageable);

        return PaginationRes.toRes(goodsPage);
    }

    /**
     * 상품을 상세 조회한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param goodsNo 상품 번호
     * @return GoodsDetailRes
     */
    public GoodsDetailRes getGoods(int providerNo, int goodsNo) {

        return goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalseWithUser(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));
    }

    /**
     * 상품을 수정한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param goodsNo         상품 번호
     * @param GoodsUpdateReq 상품 등록 데이터
     * @return GoodsUpdateRes
     */
    @Transactional
    public GoodsUpdateRes updateGoods(int providerNo, int goodsNo, GoodsUpdateReq dto) {

        Goods goods = goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalse(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));

        goods.update(dto);

        return GoodsUpdateRes.of(goods);
    }

    /**
     * 상품을 삭제한다.
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param goodsNo 상품 번호
     * @return GoodsDeleteRes
     */
    @Transactional
    public GoodsDeleteRes deleteGoods(int providerNo, int goodsNo) {

        Goods goods = goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalse(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));

        goods.softDelete();

        return GoodsDeleteRes.of(goods);
    }

    /**
     * 해당 상품의 최신 수정일 노출
     * 상품 정보 수정 업데이트 범위 : 
     *
     * @param providerNo 데이타 프로바이더 번호
     * @param goodsNo 상품 번호
     * @return GoodsLatestUpdatedRes
     */
    public GoodsLatestUpdatedRes getGoodsLatestUpdated(int providerNo, int goodsNo) {
        return goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalseWithLatestModifyInfo(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));
    }

    /**
     * 상품 데이타 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page       조회할 페이지 번호 (min 1)
     * @param size       조회할 요소 사이즈 (min 1)
     * @param keyword 검색 키워드
     * @param providerNo 데이타 프로바이더 번호
     * @param goodsNo 상품 번호
     * @return PaginationRes<GoodsDataRes>
     */
    public PaginationRes<GoodsDataRes> getAllGoodsData(int page, int size, String keyword, int providerNo, int goodsNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<GoodsDataRes> goodsDataPage = goodsDataRepository.searchGoodsData(providerNo, goodsNo, keyword, pageable);

        return PaginationRes.toRes(goodsDataPage);
    }

    /**
     * 데이터 카테고리 대분류 목록을 조회한다.
     *
     * @return List<GoodsDataCatgoryRes>
     */
    public List<GoodsDataCategoryRes> getGoodsDataMainCategorys() {
        return commonCodeDetailRepository.getGoodsDataMainCategorys();
    }

    /**
    * 데이터 카테고리 중분류 목록을 조회한다.
    *
    * @param codeId
    * @return List<GoodsDataCatgoryRes> 
    */
    public List<GoodsDataCategoryRes> getGoodsDataSubCategorys(String codeId) {
        return commonCodeDetailRepository.getGoodsDataSubCategorys(codeId);
    }

    /**
    * 데이터명 목록을 조회합니다.
    *
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return List<GoodsDataNameRes>
    */
    public List<GoodsDataNameRes> getAllGoodsDataNm(Integer providerNo, Integer goodsNo) {
        return goodsDataRepository.getAllGoodsDataNm(providerNo, goodsNo);
    }

    public GoodsDataDetailRes getGoodsData(Integer providerNo, Integer goodsNo, Integer goodsDataNo) {
        GoodsData goodsData = goodsDataRepository.getGoodsData(providerNo, goodsNo, goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("provider no: %d, goods no: %d, goodsdata no: %d", providerNo, goodsNo, goodsDataNo)));

        List<CategoryDataRes> goodsDataCategoryList = null; // goodsDataCategoryRepository.findGDAT(goodsData.getGoodsDataNo());

        List<StructuredDataRes> StructuredDataList = null; // goodsDataCategoryRepository.findGDATS(goodsData.getGoodsDataNo());

        List<UnStructuredDataRes> unStructuredDataList = null; // goodsDataCategoryRepository.findGUDAT(goodsData.getGoodsDataNo());

        return GoodsDataDetailRes.of(goodsData, goodsDataCategoryList, StructuredDataList, unStructuredDataList);
    }

    /**
    * 데이터 등록: 신규등록
    *
    * @param 
    * @return 
    */
    @Transactional
    public void createGoodsData(Integer providerNo, Integer goodsNo, GoodsDataCreateReq dto) {
        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

        Goods goods = goodsRepository.findByProviderNoAndGoodsNoAndIsDeletedFalse(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("provider no: %d, goods no: %d", providerNo, goodsNo)));

        GoodsData savedGoodsData = goodsDataRepository.save(GoodsData.of(goods, dto));

        // 카테고리 저장
        List<GoodsDataCategory> goodsDataCategoryList = dto.categoryCodeIdList().stream()
                .map(categoryCodeId -> GoodsDataCategory.of(savedGoodsData, categoryCodeId))
                .toList();
        goodsDataCategoryRepository.saveAll(goodsDataCategoryList);

        // 정형 데이타 저장
        if (dto.structuredDataList() != null) {
            for (StructuredDataReq structuredData : dto.structuredDataList()) {

                adminFileService.updateFile(structuredData.fileNo(), savedGoodsData.getGoodsDataNo(),
                        FileRelationshipObjectSeparateCode.GDATS.getCode(), DateUtil.endYmd());

                // todo : 정형 데이타는 카테고리를 생성하지 않는다(3/28 승호님과 결정)

                goodsDataSourceDetailRepository.save(GoodsDataSourceDetail.of(savedGoodsData.getGoodsDataNo(), structuredData.dataNm(), "targetDbNm",
                        "targetTableNm", "queryConditionCntt", "goodsDataSourceExpn", "goodsDataSourceUnitNm"));
            }
        }
        // 비정형 데이타 저장
        if (dto.unStructuredDataList() != null) {
            for (UnstructuredDataReq unStructuredData : dto.unStructuredDataList()) {
                adminFileService.updateFile(unStructuredData.fileNo(), savedGoodsData.getGoodsDataNo(),
                        FileRelationshipObjectSeparateCode.GUDAT.getCode(), DateUtil.endYmd());

                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, unStructuredData));
            }
        }

        return;
    }

    /**
    * 데이터 등록: 기존 데이타 업데이트
    *
    * @param 
    * @return 
    */
    @Transactional
    public void updateGoodsData(Integer providerNo, Integer goodsNo, GoodsDataUpdateReq dto) {

        // if (dto.fileNo() == null) {
        // adminFileService.deleteFiles(bulletinBoard.getBulletinNo(), FileRelationshipObjectSeparateCode.BBS.getCode());
        // } else {
        // adminFileService.updateFile(dto.fileNo(), bulletinBoard.getBulletinNo(), FileRelationshipObjectSeparateCode.BBS.getCode(),
        // DateUtil.endYmd());
        // }

        return;
    }

}
