package com.sk.domain.admin.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
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
import com.sk.domain.admin.dto.response.provider.UnstructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.GoodsDataManagementSeparateCode;
import com.sk.domain.shared.enums.StructuredDataType;
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
    public List<ProviderNameRes> getAllProviderNm() {
        return providerRepository.getAllProviderNm();
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

        Page<ProviderRes> providerPage = providerRepository.search(pageable);

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
        return providerRepository.getProviderDetail(providerNo)
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
    public ProviderUpdateRes updateProvider(int providerNo, ProviderUpdateReq dto) {

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
        return providerRepository.getProviderLatestUpdated(providerNo)
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

        Page<GoodsRes> goodsPage = goodsRepository.search(providerNo, keyword, pageable);

        return PaginationRes.toRes(goodsPage);
    }

    /**
     * 상품을 상세 조회한다.
     *
     * @param goodsNo 상품 번호
     * @return GoodsDetailRes
     */
    public GoodsDetailRes getGoodsDetail(int goodsNo) {

        return goodsRepository.getGoodsDetail(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));
    }

    /**
     * 상품을 수정한다.
     *
     * @param goodsNo         상품 번호
     * @param GoodsUpdateReq 상품 등록 데이터
     * @return GoodsUpdateRes
     */
    @Transactional
    public GoodsUpdateRes updateGoods(int goodsNo, GoodsUpdateReq dto) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));

        goods.update(dto);

        return GoodsUpdateRes.of(goods);
    }

    /**
     * 상품을 삭제한다.
     *
     * @param goodsNo 상품 번호
     * @return GoodsDeleteRes
     */
    @Transactional
    public GoodsDeleteRes deleteGoods(int goodsNo) {

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));

        goods.softDelete();

        return GoodsDeleteRes.of(goods);
    }

    /**
     * 해당 상품의 최신 수정일 노출
     * 상품 정보 수정 업데이트 범위 : 
     *
     * @param goodsNo 상품 번호
     * @return GoodsLatestUpdatedRes
     */
    public GoodsLatestUpdatedRes getGoodsLatestUpdated(int goodsNo) {
        return goodsRepository.getGoodsLatestUpdated(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("goods no: %d", goodsNo)));
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

        Page<GoodsDataRes> goodsDataPage = goodsDataRepository.search(providerNo, goodsNo, keyword, pageable);

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

    /**
    * 정형 데이터 타입 목록을 조회합니다.
    *
    * @return List<StructuredDataType>
    */
   //TODO: 호영: 정형 데이터 타입 DB로 동적 처리 필요
   public List<StructuredDataType> getAllStructuredDataTypes() {
       return Arrays.stream(StructuredDataType.values())
               .collect(Collectors.toList());
   }
   
   //TODO: 호영: 정형 데이터 검증

    /**
    * 데이터 상세 조회합니다.
    *
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return List<GoodsDataNameRes>
    */
    public GoodsDataDetailRes getGoodsData(Integer goodsDataNo) {
        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("goodsdata no: %d", goodsDataNo)));

        List<CategoryDataRes> goodsDataCategoryList = goodsDataCategoryRepository.findGDAT(goodsData.getGoodsDataNo());

        List<StructuredDataRes> structuredDataList = goodsDataSourceDetailRepository.findGDATS(goodsData.getGoodsDataNo());

        List<UnstructuredDataRes> unstructuredDataList = goodsDataCategoryRepository.findGUDAT(goodsData.getGoodsDataNo());

        return GoodsDataDetailRes.of(goodsData, goodsDataCategoryList, structuredDataList, unstructuredDataList);
    }

    /**
    * 데이터 등록: 신규등록
    *
    * @param 
    * @return 
    */
    @Transactional
    public void createGoodsData(Integer goodsNo, GoodsDataCreateReq dto) {
        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalse(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("goods no: %d", goodsNo)));

        GoodsData savedGoodsData = goodsDataRepository.save(GoodsData.of(goods, dto));

        // 카테고리 저장
        if (dto.categoryDataList() != null) {
            List<GoodsDataCategory> goodsDataCategoryList = dto.categoryDataList().stream()
                    .map(categoryCodeId -> GoodsDataCategory.of(savedGoodsData, categoryCodeId.CodeId(), "",
                            GoodsDataManagementSeparateCode.GDAT.getCode(), null))
                    .toList();
            goodsDataCategoryRepository.saveAll(goodsDataCategoryList);
        } else {
            // todo error
        }

        // 정형 데이타 저장
        if (dto.structuredDataList() != null) {
            for (StructuredDataReq structuredData : dto.structuredDataList()) {

                adminFileService.updateFile(structuredData.fileNo(), savedGoodsData.getGoodsDataNo(),
                        FileRelationshipObjectSeparateCode.GDATS.getCode(), DateUtil.endYmd());

                GoodsDataSourceDetail savedGoodsDataSourceDetail = goodsDataSourceDetailRepository
                        .save(GoodsDataSourceDetail.of(savedGoodsData, structuredData.dataNm(), "targetDbNm",
                                "targetTableNm", "queryConditionCntt", "goodsDataSourceExpn", "goodsDataSourceUnitNm", "tableColumnListNm"));

                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, "DATATYPE", structuredData.dataType(),
                        GoodsDataManagementSeparateCode.GUDAT.getCode(), Long.valueOf(savedGoodsDataSourceDetail.getGoodsDataSourceNo())));
            }
        }

        // 비정형 데이타 저장
        if (dto.unstructuredDataList() != null) {
            for (UnstructuredDataReq unstructuredData : dto.unstructuredDataList()) {

                adminFileService.updateFile(unstructuredData.fileNo(), savedGoodsData.getGoodsDataNo(),
                        FileRelationshipObjectSeparateCode.GUDAT.getCode(), DateUtil.endYmd());

                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, "PUBDATE", unstructuredData.criteriaDt(),
                        GoodsDataManagementSeparateCode.GUDAT.getCode(), unstructuredData.fileNo()));
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
    public void updateGoodsData(Integer goodsDataNo, GoodsDataUpdateReq dto) {
        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("goodsdata no: %d", goodsDataNo)));

        goodsData.update(dto);

        // 기존 카테고리와 비교해서 정리
        if (dto.categoryDataList() != null) {
            List<String> oldCategoryCodes = goodsDataCategoryRepository.findGDAT(goodsData.getGoodsDataNo()).stream()
                    .map(category -> category.codeId())
                    .toList();

            List<String> newCategoryCodes = dto.categoryDataList().stream().map(CategoryDataReq::CodeId).toList();

            log.info("==================> OLD: {}", oldCategoryCodes);
            log.info("==================> NEW: {}", newCategoryCodes);

            Set<String> codesToDelete = new HashSet<>(oldCategoryCodes);
            codesToDelete.removeAll(newCategoryCodes);
            for (String codeId : codesToDelete) {
                log.info("==================> DEL: {}", codeId);

                // goodsDataCategoryRepository.f
            }

            // 추가할 파일: 새 리스트 중 기존 파일에 없는 파일들
            Set<String> codesToAdd = new HashSet<>(newCategoryCodes);
            codesToAdd.removeAll(oldCategoryCodes);
            for (String codeId : codesToAdd) {
                log.info("==================> ADD: {}", codeId);

                //
            }
        }

        // todo 기존 정형 데이타 비교해서 신규 데이타 추가만 가능
        if (dto.structuredDataList() != null) {
            for (StructuredDataReq structuredData : dto.structuredDataList()) {
                log.info("==================> {}", structuredData);
            }
        }

        // todo 기존 비정형 데이타 비교해서 삭제 및 추가
        if (dto.unstructuredDataList() != null) {
            // 기존 파일 번호 리스트 추출
            List<Long> oldFileNos = adminFileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                    .stream()
                    .map(file -> file.getFileNo())
                    .toList();
            // 전달받은 새 파일 번호 리스트 추출
            List<Long> newFileNos = dto.unstructuredDataList()
                    .stream()
                    .map(UnstructuredDataReq::fileNo)
                    .toList();

            log.info("==================> OLD: {}", oldFileNos);
            log.info("==================> NEW: {}", newFileNos);

            Set<Long> oldFileSet = new HashSet<>(oldFileNos);
            Set<Long> newFileSet = new HashSet<>(newFileNos);

            // 삭제할 파일: 기존 파일 중 새 리스트에 없는 파일들
            Set<Long> filesToDelete = new HashSet<>(oldFileSet);
            filesToDelete.removeAll(newFileSet);
            for (Long fileNo : filesToDelete) {
                log.info("==================> DEL: {}", fileNo);

                // adminFileService.deleteFile(fileNo);

                // goodsDataCategoryRepository.getGoodsDataCategory(
                // goodsData.getGoodsDataNo(),
                // GoodsDataManagementSeparateCode.GUDAT.getCode(), fileNo)
                // .ifPresent(category -> category.softDelete());
            }

            // 추가할 파일: 새 리스트 중 기존 파일에 없는 파일들
            Set<Long> filesToAdd = new HashSet<>(newFileSet);
            filesToAdd.removeAll(oldFileSet);
            for (Long fileNo : filesToAdd) {
                log.info("==================> ADD: {}", fileNo);

                // adminFileService.updateFile(
                // fileNo,
                // goodsData.getGoodsDataNo(),
                // FileRelationshipObjectSeparateCode.GUDAT.getCode(),
                // DateUtil.endYmd());

                // // 새로 추가할 비정형 데이터와 매칭
                // dto.unstructuredDataList()
                // .stream()
                // .filter(data -> fileNo.equals(data.fileNo()))
                // .findFirst()
                // .ifPresent(data -> goodsDataCategoryRepository.save(
                // GoodsDataCategory.of(goodsData, "PUBDATE", data.criteriaDt(),
                // GoodsDataManagementSeparateCode.GUDAT.getCode(), data.fileNo())));
            }
        }

    }
}
