package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataReq;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataReq;
import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.dto.response.provider.ComponentInfoRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.admin.dto.response.provider.UnstructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.GoodsDataManagementSeparateCode;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ComponentInfoRepository;
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
public class AdminGoodsDataService {
    private final CommonCodeDetailRepository commonCodeDetailRepository;
    private final ProviderRepository providerRepository;
    private final GoodsRepository goodsRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final AdminFileService adminFileService;

    /**
    * 상품 데이타 목록을 페이지네이션 처리하여 조회한다.
    *
    * @param user 사용자 정보
    * @param page 조회할 페이지 번호 (min 1)
    * @param size 조회할 요소 사이즈 (min 1)
    * @param keyword 검색 키워드
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return PaginationRes<GoodsDataRes>
    */
    public PaginationRes<GoodsDataRes> getAllGoodsData(User user, int page, int size, String keyword, int providerNo, int goodsNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // 관리자
        if (RoleUtil.isAdmin(user)) {
            return PaginationRes.toRes(goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoForAdmin(providerNo, goodsNo, keyword, pageable));
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

            return PaginationRes.toRes(goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoForOperator(
                    user.getCustomerCompany().getCustomerCompanyNo(), providerNo, goodsNo, keyword, pageable));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED, "user role not found");
    }

    /**
    * 데이터 카테고리 대분류 목록을 조회한다.
    *
    * @return List<GoodsDataCatgoryRes>
    */
    public List<GoodsDataCategoryRes> getGoodsDataMainCategorys() {
        return commonCodeDetailRepository.findGoodsDataMainCategorys();
    }

    /**
    * 데이터 카테고리 중분류 목록을 조회한다.
    *
    * @param codeId
    * @return List<GoodsDataCatgoryRes>
    */
    public List<GoodsDataCategoryRes> getGoodsDataSubCategorys(String codeId) {
        return commonCodeDetailRepository.findGoodsDataSubCategorysByCodeId(codeId);
    }

    /**
    * 정형 데이타의 타입 목록을 조회한다.
    *
    * @return List<ComponentInfoRes>
    */
    public List<ComponentInfoRes> getStructuredDataType() {
        return componentInfoRepository.findComponentNm();
    }

    /**
    * 데이터명 목록을 조회합니다.
    *
    * @param user 사용자 정보
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return List<GoodsDataNameRes>
    */
    public List<GoodsDataNameRes> getAllGoodsDataNm(User user, int providerNo, int goodsNo) {

        // 관리자
        if (RoleUtil.isAdmin(user)) {
            return goodsDataRepository.findGoodsDataNamesByProviderNoAndGoodsNo(providerNo, goodsNo);
        }

        // 운영자
        if (RoleUtil.isOperator(user)) {
            //
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED, "user role not found");

    }

    /**
    * 데이터 상세 조회합니다.
    *
    * @param user 사용자 정보
    * @param goodsNo 상품 번호
    * @return GoodsDataDetailRes
    */
    public GoodsDataDetailRes getGoodsData(User user, int providerNo, int goodsNo, int goodsDataNo) {

        // 관리자
        if (RoleUtil.isAdmin(user)) {
            //
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
        }

        GoodsData goodsData = goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                "provider no: %d, goods no: %d, goodsdata no: %d".formatted(providerNo, goodsNo, goodsDataNo)));

        List<CategoryDataRes> goodsDataCategoryList = goodsDataCategoryRepository.findCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo());

        List<StructuredDataRes> structuredDataList = goodsDataCategoryRepository.findStructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

        List<UnstructuredDataRes> unstructuredDataList = goodsDataCategoryRepository.findUnstructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

        return GoodsDataDetailRes.of(goodsData, goodsDataCategoryList, structuredDataList, unstructuredDataList);
    }

    /**
    * 데이터 등록: 신규등록
    *
    * @param goodsNo 상품 번호
    * @param GoodsDataCreateReq
    * @return
    */
    @Transactional
    public void createGoodsData(User user, int providerNo, int goodsNo, GoodsDataCreateReq dto) {

        CustomerCompany customerCompany = null;

        // 관리자
        if (RoleUtil.isAdmin(user)) {
            customerCompany = null;

            providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));
        }

        // 운영자
        if (RoleUtil.isOperator(user)) {
            customerCompany = user.getCustomerCompany();

            providerRepository.findByProviderNoAndIsDeletedFalseAndIsUsedTrue(providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));
        }

        Goods goods = goodsRepository.findByGoodsNoAndIsDeletedFalseAndIsUsedTrue(goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

        // gds_data_dstg_id는 LSEG의 식별 ID로 사용하려고 만는 컬럼이라서 무시하셔도 됩니다.
        GoodsData savedGoodsData = goodsDataRepository
                .save(GoodsData.of(goods, customerCompany, dto.goodsDataNm(), dto.goodsDataExpn(), dto.goodsDataSummaryCntt()));

        // 카테고리 저장
        if (dto.categoryDataList() != null) {
            List<GoodsDataCategory> goodsDataCategoryList = dto.categoryDataList().stream()
                    .map(categoryCodeId -> GoodsDataCategory.of(savedGoodsData, categoryCodeId.CodeId(), "",
                            GoodsDataManagementSeparateCode.GDAT.getCode(), null))
                    .toList();
            goodsDataCategoryRepository.saveAll(goodsDataCategoryList);
        }

        // 정형 데이타 저장
        if (dto.structuredDataList() != null) {
            for (StructuredDataReq structuredData : dto.structuredDataList()) {

                adminFileService.updateFile(structuredData.fileNo(), savedGoodsData.getGoodsDataNo(),
                        FileRelationshipObjectSeparateCode.GDATS.getCode(), DateUtil.endYmd());

                // todo
                GoodsDataSourceDetail savedGoodsDataSourceDetail = goodsDataSourceDetailRepository
                        .save(GoodsDataSourceDetail.of(savedGoodsData, "goodsDataSourceNm", "targetDbNm",
                                "targetTableNm", "queryConditionCntt", "goodsDataSourceExpn", "goodsDataSourceUnitNm", "tableColumnListNm"));

                // gds_data_keywd_nm 명은 카테고리 정한 뒤에 임의 값을 넣을 필요가 있을때 사용하는 값입니다. 발간일자의 날자값, 산업구분의 산업상세내역
                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, structuredData.componentTypeId(), "goodsDataKeywordNm",
                        GoodsDataManagementSeparateCode.GDATS.getCode(), Long.valueOf(savedGoodsDataSourceDetail.getGoodsDataSourceNo())));
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
    * @param goodsNo 상품 번호
    * @param GoodsDataUpdateReq
    * @return
    */
    @Transactional
    public void updateGoodsData(User user, int providerNo, int goodsNo, int goodsDataNo, GoodsDataUpdateReq dto) {
        GoodsData goodsData = goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goodsdata no: %d".formatted(goodsDataNo)));

        goodsData.update(dto.goodsExpn(), dto.goodsDataSummaryCntt());

        // 카테고리 수정
        if (dto.categoryDataList() != null) {
            List<String> oldCategoryCodes = goodsDataCategoryRepository.findCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo()).stream()
                    .map(category -> category.codeId())
                    .toList();

            List<String> newCategoryCodes = dto.categoryDataList().stream().map(CategoryDataReq::CodeId).toList();

            Set<String> codesToDelete = new HashSet<>(oldCategoryCodes);
            codesToDelete.removeAll(newCategoryCodes);
            for (String codeId : codesToDelete) {
                goodsDataCategoryRepository.findByCategoryCode(goodsData.getGoodsDataNo(), codeId).ifPresent(category -> category.softDelete());
            }

            Set<String> codesToAdd = new HashSet<>(newCategoryCodes);
            codesToAdd.removeAll(oldCategoryCodes);
            for (String codeId : codesToAdd) {
                goodsDataCategoryRepository.save(GoodsDataCategory.of(goodsData, codeId, "",
                        GoodsDataManagementSeparateCode.GDAT.getCode(), null));
            }
        }

        // 정형 데이타 수정
        if (dto.structuredDataList() != null) {
            for (StructuredDataReq structuredData : dto.structuredDataList()) {
                log.info("==================> {}", structuredData);
                // todo 기존 정형 데이타 비교해서 신규 데이타 추가만 가능
                // 재현님하고 협의
            }
        }

        // 비정형데이타 수정
        if (dto.unstructuredDataList() != null) {
            List<Long> oldFileNos = adminFileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                    .stream()
                    .map(file -> file.getFileNo())
                    .toList();
            List<Long> newFileNos = dto.unstructuredDataList()
                    .stream()
                    .map(UnstructuredDataReq::fileNo)
                    .toList();

            Set<Long> oldFileSet = new HashSet<>(oldFileNos);
            Set<Long> newFileSet = new HashSet<>(newFileNos);

            Set<Long> filesToDelete = new HashSet<>(oldFileSet);
            filesToDelete.removeAll(newFileSet);
            for (Long fileNo : filesToDelete) {

                adminFileService.deleteFile(fileNo);

                goodsDataCategoryRepository.findGoodsDataCategoryByGoodsDataNo(
                        goodsData.getGoodsDataNo(),
                        GoodsDataManagementSeparateCode.GUDAT.getCode(), fileNo)
                        .ifPresent(category -> category.softDelete());
            }

            Set<Long> filesToAdd = new HashSet<>(newFileSet);
            filesToAdd.removeAll(oldFileSet);
            for (Long fileNo : filesToAdd) {

                adminFileService.updateFile(
                        fileNo,
                        goodsData.getGoodsDataNo(),
                        FileRelationshipObjectSeparateCode.GUDAT.getCode(),
                        DateUtil.endYmd());

                dto.unstructuredDataList()
                        .stream()
                        .filter(data -> fileNo.equals(data.fileNo()))
                        .findFirst()
                        .ifPresent(data -> goodsDataCategoryRepository.save(
                                GoodsDataCategory.of(goodsData, "PUBDATE", data.criteriaDt(),
                                        GoodsDataManagementSeparateCode.GUDAT.getCode(), data.fileNo())));
            }
        }
    }
}
