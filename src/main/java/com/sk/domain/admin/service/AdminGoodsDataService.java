package com.sk.domain.admin.service;

import com.sk.domain.admin.dto.request.provider.StructuredDataJsonReq;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.sk.common.properties.ProviderDefaultProperties;
import com.sk.domain.admin.dto.response.provider.*;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.enums.ResultCode;
import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataFileReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataReq;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataReq;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.dto.response.file.GoodsDataFileUploadRes;
import com.sk.domain.shared.client.dto.AXMetaFileDto;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.GoodsDataManagementSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tam.CustomerCompanyProviderMappingRepository;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ComponentInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataUnitConvertRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.FileService;

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
    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerCompanyProviderMappingRepository customerCompanyProviderMappingRepository;
    private final CustomerCompanyGoodsMappingRepository customerCompanyGoodsMappingRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final FileService fileService;
    private final ExcelService excelService;
    private final UnitConvertRepository unitConvertRepository;
    private final GoodsDataUnitConvertRepository goodsDataUnitConvertRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager masterTransactionManager;
    private final PlatformTransactionManager myBatisTransactionManager;
    private final ApplicationEventPublisher publisher;
    private final ProviderDefaultProperties providerDefaultProperties;

    /**
    * 상품 데이타 목록을 페이지네이션 처리하여 조회한다.
    *
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param page 조회할 페이지 번호 (min 1)
    * @param size 조회할 요소 사이즈 (min 1)
    * @param keyword 검색 키워드
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return PaginationRes<GoodsDataRes>
    */
    public PaginationRes<GoodsDataRes> getAllGoodsData(int companyNo, Role role, int page, int size, String keyword, int providerNo, int goodsNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        return PaginationRes.toRes(goodsDataRepository.findGoodsDataByProviderNoAndGoodsNo(providerNo, goodsNo, keyword, pageable));
    }

    /**
     * 권한 상세 목록 조회 API
     *
     * @param page
     * @param size
     * @param providerNo
     * @param goodsNo
     * @param companyNm
     * @return
     */
    public PaginationRes<GoodsCompanyPermissionRes> getCompanyPermissions(int page, int size, Integer providerNo, Integer goodsNo, String companyNm) {
        return PaginationRes
                .toRes(customerCompanyGoodsMappingRepository.findByCompanyPermissionsByConditions(page, size, providerNo, goodsNo, companyNm));
    }

    /**
    * 문서 타입 카테고리 목록을 조회한다.
    *
    * @return List<GoodsDataCatgoryRes>
    */
    public List<GoodsDataCategoryRes> getDocTypeCategorys() {
        return commonCodeDetailRepository.findDocTypeCategory();
    }

    /**
    * 테마 타입 카테고리 목록을 조회한다.
    *
    * @return List<GoodsDataCatgoryRes>
    */
    public List<GoodsDataCategoryRes> getThemeTypeCategorys() {
        return commonCodeDetailRepository.findThemeTypeCategory();
    }

    /**
    * 언어 타입 카테고리 목록을 조회한다.
    *
    * @return List<GoodsDataCatgoryRes>
    */
    public List<GoodsDataCategoryRes> getLangTypeCategorys() {
        return commonCodeDetailRepository.findLangTypeCategory();
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
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @return List<GoodsDataNameRes>
    */
    public List<GoodsDataNameRes> getAllGoodsDataNm(int companyNo, Role role, int providerNo, int goodsNo) {

        return goodsDataRepository.findGoodsDataNamesByProviderNoAndGoodsNo(providerNo, goodsNo);
        // switch (role) {
        // case ADMIN, API_SUB_ADMIN:
        // return goodsDataRepository.findGoodsDataNamesByProviderNoAndGoodsNoForAdmin(providerNo, goodsNo);

        // case OPERATOR, API_PLUS_OPERATOR:
        // return goodsDataRepository.findGoodsDataNamesByProviderNoAndGoodsNoForOperator(companyNo, providerNo, goodsNo);

        // default:
        // throw CustomAuthorizationException.of(Error.AUTH_INSUFFICIENT_PERMISSIONS);
        // }
    }

    /**
    * 데이터 상세 조회합니다.
    *
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @param goodsDataNo 상품 데이타 번호
    * @return GoodsDataDetailRes
    */
    public GoodsDataDetailRes getGoodsData(int companyNo, Role role, int providerNo, int goodsNo, int goodsDataNo) {
        if (role == Role.OPERATOR || role == Role.API_PLUS_OPERATOR) {
            validateOperatorAccess(companyNo, providerNo, goodsNo);
        }

        GoodsData goodsData = goodsDataRepository
                .findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                Error.RSC_NOT_FOUND,
                                "provider no: %d, goods no: %d, goodsdata no: %d"
                                        .formatted(providerNo, goodsNo, goodsDataNo)));

        List<CategoryDataRes> docCategoryList = goodsDataCategoryRepository.findDocCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo());

        List<CategoryDataRes> langCategoryList = goodsDataCategoryRepository.findLangCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo());

        List<UnstructuredDataRes> unstructuredDataList = goodsDataCategoryRepository.findUnstructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

        List<StructuredDataRes> structuredDataList = goodsDataSourceDetailRepository.findGoodsDataSourceStructuredDatasByConditions(goodsData);

        return GoodsDataDetailRes.of(
                goodsData, docCategoryList, langCategoryList, structuredDataList, unstructuredDataList);
    }

    private void validateOperatorAccess(int companyNo, int providerNo, int goodsNo) {
        customerCompanyProviderMappingRepository
                .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

        customerCompanyGoodsMappingRepository
                .findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));
    }

    /**
    * 데이터 등록: 신규등록
    *
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @param dto
    * @return
    */
    @Transactional
    public void createGoodsData(int companyNo, Role role, int providerNo, int goodsNo, GoodsDataCreateReq dto) {
        if (role == Role.OPERATOR || role == Role.API_PLUS_OPERATOR) {
            validateOperatorAccess(companyNo, providerNo, goodsNo);
        }

        Goods goods = goodsRepository
                .findByGoodsNo(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                Error.RSC_NOT_FOUND,
                                "provider no: %d, goods no: %d".formatted(providerNo, goodsNo)));

        switch (role) {
            case ADMIN, API_SUB_ADMIN:
                if (!goods.getIsPublic()) {
                    throw AppErrorException.of(Error.AUTH_INSUFFICIENT_PERMISSIONS);
                }

                internalCreateGoodsData(goods, null, providerNo, dto);
                break;

            case OPERATOR, API_PLUS_OPERATOR:
                if (goods.getIsPublic()) {
                    throw AppErrorException.of(Error.AUTH_INSUFFICIENT_PERMISSIONS);
                }

                CustomerCompany customerCompany = customerCompanyRepository
                        .findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                        .orElseThrow(
                                () -> AppErrorException.of(
                                        Error.RSC_NOT_FOUND,
                                        "customer company no: %d".formatted(companyNo)));

                internalCreateGoodsData(goods, customerCompany, providerNo, dto);
                break;

            default:
                throw CustomAuthorizationException.of(Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
     * 데이터 저장
     *
     * @param goods 상품
     * @param customerCompany 고객사
     * @param providerNo 프로바이더 번호
     * @param dto 데이터 등록 요청 정보
     */
    private void internalCreateGoodsData(Goods goods, CustomerCompany customerCompany, int providerNo, GoodsDataCreateReq dto) {
        List<GoodsData> goodsDatas = goodsDataRepository.findByGoodsDataNmAndGodsDataExpnAndGoodsDataSummaryCntt(
                dto.goodsDataNm(),
                dto.goodsDataExpn(),
                dto.goodsDataSummaryCntt(),
                PageRequest.of(0, 1));
        if (goodsDatas.size() > 0) {
            GoodsData goodsData = goodsDatas.get(0);

            // 비정형 데이타
            if (dto.unstructuredDataList() != null) {
                List<Long> oldFileNos = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                        .stream()
                        .map(file -> file.getFileNo())
                        .toList();

                List<Long> newFileNos = dto.unstructuredDataList()
                        .stream()
                        .map(UnstructuredDataReq::fileNo)
                        .toList();

                Set<Long> oldFileSet = new HashSet<>(oldFileNos);
                Set<Long> newFileSet = new HashSet<>(newFileNos);

                Set<Long> filesToAdd = new HashSet<>(newFileSet);
                filesToAdd.removeAll(oldFileSet);
                if (filesToAdd.isEmpty()) {
                    return;
                }

                for (Long fileNo : filesToAdd) {
                    fileService.updateFile(
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

                // A.X Platform 에 파일 업로드 연동
                List<String> fileNameList = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                        .stream()
                        .map(file -> file.getOriginalFileNm())
                        .toList();

                String documentId = goodsData.getGoodsDataDistinguishId();

                AXMetaFileDto axMetaFileDto = AXMetaFileDto.builder()
                        .gdsNo(goodsData.getGoods().getGoodsNo())
                        .documentId(documentId)
                        .documentReleaseDate(nowOffsetDateTime())
                        .headline(goodsData.getGoodsDataNm())
                        .synopsis(goodsData.getGoodsDataExpn() != null ? goodsData.getGoodsDataExpn()
                                : "" + goodsData.getGoodsDataSummaryCntt() != null ? goodsData.getGoodsDataSummaryCntt() : "")
                        .contributor(goodsData.getDataContributorNm())
                        .authors(List.of(""))
                        .documentFileName(fileNameList)
                        .language("")
                        .countries(List.of(""))
                        .build();

                goodsData.updateMetaDataUploadStatus(false);
                publisher.publishEvent(new AXUploadFileEvent(goodsData.getGoodsDataNo(), axMetaFileDto, false));
            } else {
                return;
            }

            // 카테고리 기존거 삭제 후 새로 등록
            goodsDataCategoryRepository.softDeleteByGoodsDataNo(goodsData.getGoodsDataNo());

            // 프로바이더 타입 카테고리 등록
            Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            goodsDataCategoryRepository.save(GoodsDataCategory.of(goodsData, "A_".concat(provider.getProviderId()), "",
                    GoodsDataManagementSeparateCode.GDAT.getCode(), null));

            // 문서 타입 카테고리 등록
            if (dto.docCategoryList() != null) {
                List<GoodsDataCategory> goodsDataCategoryList = dto.docCategoryList().stream()
                        .map(categoryCodeId -> GoodsDataCategory.of(goodsData, categoryCodeId.codeId(), "",
                                GoodsDataManagementSeparateCode.GDAT.getCode(), null))
                        .toList();

                goodsDataCategoryRepository.saveAll(goodsDataCategoryList);
            }

            // 언어 타입 카테고리 Default 추가
            List<CategoryDataReq> langCategoryList = setDefaultLangCategoryList(providerNo, dto.langCategoryList());

            // 언어 타입 카테고리 등록
            if (langCategoryList != null) {
                List<GoodsDataCategory> goodsDataCategoryList = langCategoryList.stream()
                        .map(categoryCodeId -> GoodsDataCategory.of(goodsData, categoryCodeId.codeId(), "",
                                GoodsDataManagementSeparateCode.GDAT.getCode(), null))
                        .toList();

                goodsDataCategoryRepository.saveAll(goodsDataCategoryList);
            }

            goodsData.updateModifyDt();
        } else {
            // gds_data_dstg_id는 LSEG의 식별 ID로 사용하려고 만는 컬럼이라서 무시하셔도 됩니다.
            GoodsData savedGoodsData = goodsDataRepository
                    .save(GoodsData.of(goods, customerCompany, dto.goodsDataNm(), dto.goodsDataExpn(), dto.goodsDataSummaryCntt(),
                            dto.dataContributorNm(), false, true));

            // 프로바이더 타입 카테고리 등록
            Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, "A_".concat(provider.getProviderId()), "",
                    GoodsDataManagementSeparateCode.GDAT.getCode(), null));

            // 문서 타입 카테고리 등록
            if (dto.docCategoryList() != null) {
                List<GoodsDataCategory> goodsDataCategoryList = dto.docCategoryList().stream()
                        .map(categoryCodeId -> GoodsDataCategory.of(savedGoodsData, categoryCodeId.codeId(), "",
                                GoodsDataManagementSeparateCode.GDAT.getCode(), null))
                        .toList();
                goodsDataCategoryRepository.saveAll(goodsDataCategoryList);
            }

            // 언어 타입 카테고리 Default 추가
            List<CategoryDataReq> langCategoryList = setDefaultLangCategoryList(providerNo, dto.langCategoryList());
            // 언어 타입 카테고리 등록
            if (langCategoryList != null) {
                List<GoodsDataCategory> goodsDataCategoryList = langCategoryList.stream()
                        .map(categoryCodeId -> GoodsDataCategory.of(savedGoodsData, categoryCodeId.codeId(), "",
                                GoodsDataManagementSeparateCode.GDAT.getCode(), null))
                        .toList();
                goodsDataCategoryRepository.saveAll(goodsDataCategoryList);
            }

            // 정형 데이타 저장
            if (dto.structuredDataList() != null) {
                for (StructuredDataReq structuredData : dto.structuredDataList()) {
                    // 컴포넌트 타입 조회
                    ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(structuredData.componentTypeId())
                            .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND,
                                    String.format("Component information could not be found. componentTypeId: %s",
                                            structuredData.componentTypeId())));

                    /**
                    * 정책 : 컴포넌트타입 + 상품데이터 => Unique Key 로서의 역할 (1 데이터소스상세 => 1 정형 그래프) 250424
                    * 그래프 하나를 그리기 위한 데이터는 엑셀 컴포넌트 하나와 매핑(하나의 그래프에서 엑셀 컴포넌트가 2개 이상일 수 없다)
                    */
                    GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                            .findByGoodsDataAndComponentInfoAndIsDeletedFalse(savedGoodsData, componentInfo)
                            .orElseGet(() -> goodsDataSourceDetailRepository
                                    .save(GoodsDataSourceDetail.of(savedGoodsData, componentInfo.getGoodsDataSourceNm(),
                                            componentInfo.getTargetDbNm(),
                                            componentInfo.getTargetTableNm(), componentInfo.getQueryConditionCntt(), null,
                                            componentInfo.getGoodsDataSourceUnitNm(),
                                            componentInfo.getGoodsDataSourceCurrencyUnitNm(), componentInfo.getTableColumnListNm(), componentInfo,
                                            componentInfo.getIsCandleChartPossible(), componentInfo.getConvertTargetDetailTypeNm())));

                    fileService.updateFile(structuredData.fileNo(), goodsDataSourceDetail.getGoodsDataSourceNo(),
                            FileRelationshipObjectSeparateCode.GDATS.getCode(), DateUtil.endYmd());

                    /**
                    * 데이터소스상세 조회 후 데이터소스상세 없는 경우 생성
                    */

                    /**
                    * 단위 변환 기본 매핑 기준
                    * 1. 데이터소스상세 convertTargetDetailTypeNm 값이 Null 아닌 경우
                    * : 데이터소스상세에서 goodsDataSourceUnitNm 값과 단위변환기본에서 convertPreviousUnitNm 값이 일치하는 데이터 중
                    * 단위변환기본에서 convertTargetDetailTypeNm 값이 Null 이거나 convertTargetDetailTypeNm 값이 데이터소스상세 convertTargetDetailTypeNm 값과 일치하는 데이터 조회
                    * 2. 데이터소스상세 convertTargetDetailTypeNm 값이 Null 경우
                    * : 데이터소스상세에서 goodsDataSourceUnitNm 값과 단위변환기본에서 convertPreviousUnitNm 값이 일치하는 데이터 조회
                    */
                    List<UnitConvert> unitConverts = unitConvertRepository
                            .findUnitConvertsByConditions(goodsDataSourceDetail);

                    if (!unitConverts.isEmpty()) {
                        goodsDataUnitConvertRepository.saveAll(unitConverts.stream()
                                .map(unitConvert -> GoodsDataUnitConvert.of(goodsDataSourceDetail, unitConvert))
                                .toList());
                    }

                    // gds_data_keywd_nm 명은 카테고리 정한 뒤에 임의 값을 넣을 필요가 있을때 사용하는 값입니다. 발간일자의 날자값, 산업구분의 산업상세내역
                    goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, structuredData.componentTypeId(), null,
                            GoodsDataManagementSeparateCode.GDATS.getCode(), Long.valueOf(goodsDataSourceDetail.getGoodsDataSourceNo())));
                }
            }

            // 비정형 데이타 저장
            if (dto.unstructuredDataList() != null) {
                for (UnstructuredDataReq unstructuredData : dto.unstructuredDataList()) {

                    fileService.updateFile(unstructuredData.fileNo(), savedGoodsData.getGoodsDataNo(),
                            FileRelationshipObjectSeparateCode.GUDAT.getCode(), DateUtil.endYmd());

                    goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, "PUBDATE", unstructuredData.criteriaDt(),
                            GoodsDataManagementSeparateCode.GUDAT.getCode(), unstructuredData.fileNo()));
                }

                // A.X Platform 에 파일 업로드 연동
                List<String> fileNameList = fileService.getFiles(savedGoodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                        .stream()
                        .map(file -> file.getOriginalFileNm())
                        .toList();

                String documentId = UUID.randomUUID().toString();

                AXMetaFileDto axMetaFileDto = AXMetaFileDto.builder()
                        .gdsNo(goods.getGoodsNo())
                        .documentId(documentId)
                        .documentReleaseDate(nowOffsetDateTime())
                        .headline(savedGoodsData.getGoodsDataNm())
                        .synopsis(savedGoodsData.getGoodsDataExpn() != null ? savedGoodsData.getGoodsDataExpn()
                                : "" + savedGoodsData.getGoodsDataSummaryCntt() != null ? savedGoodsData.getGoodsDataSummaryCntt() : "")
                        .contributor(savedGoodsData.getDataContributorNm())
                        .authors(List.of(""))
                        .documentFileName(fileNameList)
                        .language("")
                        .countries(List.of(""))
                        .build();

                savedGoodsData.updateMetaDataUploadStatus(false);
                publisher.publishEvent(new AXUploadFileEvent(savedGoodsData.getGoodsDataNo(), axMetaFileDto, true));

                savedGoodsData.updateGoodsDataDistinguishId(documentId);
            }
        }

        return;
    }

    /**
    * 데이터 등록: 기존 데이타 업데이트
    *
    * @param companyNo 사용자가 속한 고객사 번호
    * @param role 사용자 역할
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @param dto
    * @return
    */
    @Transactional
    public void updateGoodsData(int companyNo, Role role, int providerNo, int goodsNo, int goodsDataNo, GoodsDataUpdateReq dto) {
        if (role == Role.OPERATOR || role == Role.API_PLUS_OPERATOR) {
            validateOperatorAccess(companyNo, providerNo, goodsNo);
        }

        Goods goods = goodsRepository
                .findByGoodsNo(providerNo, goodsNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                Error.RSC_NOT_FOUND,
                                "provider no: %d, goods no: %d".formatted(providerNo, goodsNo)));

        switch (role) {
            case ADMIN, API_SUB_ADMIN:
                if (!goods.getIsPublic()) {
                    throw AppErrorException.of(Error.AUTH_INSUFFICIENT_PERMISSIONS);
                }
                break;

            case OPERATOR, API_PLUS_OPERATOR:
                if (goods.getIsPublic()) {
                    throw AppErrorException.of(Error.AUTH_INSUFFICIENT_PERMISSIONS);
                }
                break;

            default:
                break;
        }

        GoodsData goodsData = goodsDataRepository
                .findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                Error.RSC_NOT_FOUND, "goodsdata no: %d".formatted(goodsDataNo)));

        internalUpdateGoodsData(providerNo, goodsData, dto);
    }

    private List<CategoryDataReq> setDefaultLangCategoryList(int providerNo, List<CategoryDataReq> sourceLangCategoryData){
        List<CategoryDataReq> langCategoryList = sourceLangCategoryData;
        if(providerDefaultProperties.getLanguages() == null) return langCategoryList;
        if(providerDefaultProperties.getLanguages().containsKey(providerNo)){
            CategoryDataReq defaultLangCategoryData = new CategoryDataReq(providerDefaultProperties.getLanguages().get(providerNo));
            if(langCategoryList == null){
                langCategoryList = new ArrayList<>(1);
                langCategoryList.add(defaultLangCategoryData);
            }else{
                if(!langCategoryList.contains(defaultLangCategoryData)){
                    langCategoryList.add(defaultLangCategoryData);
                }
            }
        }
        return langCategoryList;
    }

    private void internalUpdateGoodsData(int providerNo, GoodsData goodsData, GoodsDataUpdateReq dto) {
        goodsData.update(dto.goodsDataExpn(), dto.goodsDataSummaryCntt(), dto.dataContributorNm());

        // 문서 타입 카테고리 수정
        if (dto.docCategoryList() != null) {
            List<String> oldCategoryCodes = goodsDataCategoryRepository.findDocCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo()).stream()
                    .map(category -> category.codeId())
                    .toList();

            List<String> newCategoryCodes = dto.docCategoryList().stream().map(CategoryDataReq::codeId).toList();

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

        // 언어 타입 카테고리 Default 추가
        List<CategoryDataReq> langCategoryList = setDefaultLangCategoryList(providerNo, dto.langCategoryList());

        // 언어 타입 카테고리 수정
        if (langCategoryList != null) {
            List<String> oldCategoryCodes = goodsDataCategoryRepository.findLangCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo()).stream()
                    .map(category -> category.codeId())
                    .toList();

            List<String> newCategoryCodes = langCategoryList.stream().map(CategoryDataReq::codeId).toList();

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
                List<Long> oldFileNos = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GDATS.getCode())
                        .stream()
                        .map(file -> file.getFileNo())
                        .toList();
                List<Long> newFileNos = dto.structuredDataList()
                        .stream()
                        .map(StructuredDataReq::fileNo)
                        .toList();

                Set<Long> oldFileSet = new HashSet<>(oldFileNos);
                Set<Long> newFileSet = new HashSet<>(newFileNos);
                Set<Long> filesToAdd = new HashSet<>(newFileSet);
                filesToAdd.removeAll(oldFileSet);
                for (Long fileNo : filesToAdd) {
                    fileService.updateFile(
                            fileNo,
                            goodsData.getGoodsDataNo(),
                            FileRelationshipObjectSeparateCode.GDATS.getCode(),
                            DateUtil.endYmd());

                    ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(structuredData.componentTypeId())
                            .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND,
                                    String.format("Component information could not be found. componentTypeId: %s",
                                            structuredData.componentTypeId())));

                    /**
                     * 정책 : 컴포넌트타입 + 상품데이터 => Unique Key 로서의 역할 (1 데이터소스상세 => 1 정형 그래프) 250424
                     * 그래프 하나를 그리기 위한 데이터는 엑셀 컴포넌트 하나와 매핑(하나의 그래프에서 엑셀 컴포넌트가 2개 이상일 수 없다)
                     */
                    GoodsDataSourceDetail goodsDataSourceDetail = goodsDataSourceDetailRepository
                            .findByGoodsDataAndComponentInfoAndIsDeletedFalse(goodsData, componentInfo)
                            .orElseGet(() -> goodsDataSourceDetailRepository
                                    .save(GoodsDataSourceDetail.of(goodsData, componentInfo.getGoodsDataSourceNm(), componentInfo.getTargetDbNm(),
                                            componentInfo.getTargetTableNm(), componentInfo.getQueryConditionCntt(), null,
                                            componentInfo.getGoodsDataSourceUnitNm(),
                                            componentInfo.getGoodsDataSourceCurrencyUnitNm(), componentInfo.getTableColumnListNm(), componentInfo,
                                            componentInfo.getIsCandleChartPossible(), componentInfo.getConvertTargetDetailTypeNm())));

                    /**
                     * 단위 변환 기본 매핑 기준
                     * 1. 데이터소스상세 convertTargetDetailTypeNm 값이 Null 아닌 경우
                     *      : 데이터소스상세에서 goodsDataSourceUnitNm 값과 단위변환기본에서 convertPreviousUnitNm 값이 일치하는 데이터 중
                     *        단위변환기본에서 convertTargetDetailTypeNm 값이 Null 이거나 convertTargetDetailTypeNm 값이 데이터소스상세 convertTargetDetailTypeNm 값과 일치하는 데이터 조회
                     * 2. 데이터소스상세 convertTargetDetailTypeNm 값이 Null 경우
                     *      : 데이터소스상세에서 goodsDataSourceUnitNm 값과 단위변환기본에서 convertPreviousUnitNm 값이 일치하는 데이터 조회
                     */
                    List<UnitConvert> unitConverts = unitConvertRepository
                            .findUnitConvertsByConditions(goodsDataSourceDetail);

                    goodsDataUnitConvertRepository.saveAll(unitConverts.stream()
                            .map(unitConvert -> GoodsDataUnitConvert.of(goodsDataSourceDetail, unitConvert))
                            .toList());

                    // gds_data_keywd_nm 명은 카테고리 정한 뒤에 임의 값을 넣을 필요가 있을때 사용하는 값입니다. 발간일자의 날자값, 산업구분의 산업상세내역
                    goodsDataCategoryRepository.save(GoodsDataCategory.of(goodsData, structuredData.componentTypeId(), null,
                            GoodsDataManagementSeparateCode.GDATS.getCode(), Long.valueOf(goodsDataSourceDetail.getGoodsDataSourceNo())));
                }
            }
        }

        // 비정형데이타 수정
        if (dto.unstructuredDataList() != null) {
            List<Long> oldFileNos = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
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
                fileService.deleteFile(fileNo);

                goodsDataCategoryRepository.findGoodsDataCategoryByGoodsDataNo(
                        goodsData.getGoodsDataNo(),
                        GoodsDataManagementSeparateCode.GUDAT.getCode(), fileNo)
                        .ifPresent(category -> category.softDelete());
            }

            Set<Long> filesToAdd = new HashSet<>(newFileSet);
            filesToAdd.removeAll(oldFileSet);
            for (Long fileNo : filesToAdd) {
                fileService.updateFile(
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

            // A.X Platform 에 파일 업로드 연동
            List<String> fileNameList = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                    .stream()
                    .map(file -> file.getOriginalFileNm())
                    .toList();

            String documentId = goodsData.getGoodsDataDistinguishId();

            AXMetaFileDto axMetaFileDto = AXMetaFileDto.builder()
                    .gdsNo(goodsData.getGoods().getGoodsNo())
                    .documentId(documentId)
                    .documentReleaseDate(nowOffsetDateTime())
                    .headline(goodsData.getGoodsDataNm())
                    .synopsis(goodsData.getGoodsDataExpn() != null ? goodsData.getGoodsDataExpn()
                            : "" + goodsData.getGoodsDataSummaryCntt() != null ? goodsData.getGoodsDataSummaryCntt() : "")
                    .contributor(goodsData.getDataContributorNm())
                    .authors(List.of(""))
                    .documentFileName(fileNameList)
                    .language("")
                    .countries(List.of(""))
                    .build();

            goodsData.updateMetaDataUploadStatus(false);
            publisher.publishEvent(new AXUploadFileEvent(goodsData.getGoodsDataNo(), axMetaFileDto, false));
        }
    }

    /**
     * 정형 데이타 파일 업로드 (엑셀 파일 업로드와 함께 엑셀 파싱하여 DB에 데이터 저장)
     *
     * @param userNo
     * @param companyCd
     * @param role
     * @param file
     * @param isPublic
     * @param structuredDataFileReq
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GoodsDataFileUploadRes uploadStructuredDataFile(String companyCd, Role role, MultipartFile file, boolean isPublic,
            StructuredDataFileReq structuredDataFileReq) throws IOException {

        ComponentInfo componentInfo = componentInfoRepository
                .findByComponentTypeIdAndIsDeletedFalse(structuredDataFileReq.componentTypeId())
                .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND,
                        String.format("Component information could not be found. componentTypeId: %s",
                                structuredDataFileReq.componentTypeId())));

        excelService.uploadStructuredData(file, componentInfo, structuredDataFileReq.criteriaDt(), structuredDataFileReq.category());

        FileUploadRes fileUploadRes = fileService.uploadStructuredFile(companyCd, role, file, isPublic);

        return GoodsDataFileUploadRes.of(fileUploadRes.fileNo(), structuredDataFileReq.componentTypeId(), fileUploadRes.fileNm(),
                fileUploadRes.fileSize(), fileUploadRes.fileUrl());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GoodsDataFileUploadRes uploadStructuredDataJson(String companyCd, Role role, StructuredDataJsonReq structuredDataJsonReq){

        ComponentInfo componentInfo = componentInfoRepository
            .findByComponentTypeIdAndIsDeletedFalse(structuredDataJsonReq.metadata().componentTypeId())
            .orElseThrow(() -> AppErrorException.of(Error.RSC_NOT_FOUND,
                String.format("Component information could not be found. componentTypeId: %s",
                    structuredDataJsonReq.metadata().componentTypeId())));

        excelService.uploadStructuredData(file, componentInfo, structuredDataFileReq.criteriaDt(), structuredDataFileReq.category());

        return GoodsDataFileUploadRes.of(fileUploadRes.fileNo(), structuredDataFileReq.componentTypeId(), fileUploadRes.fileNm(),
            fileUploadRes.fileSize(), fileUploadRes.fileUrl());
    }

    private String nowOffsetDateTime() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    public List<GoodsData> getGoodsData(String goodsDataNm) {
        return goodsDataRepository.findByGoodsDataNm(goodsDataNm);
    }

    public void uploadMetaFile(int goodsDataNo) {
        log.info("uploadMetaFile: " + goodsDataNo);

        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo).orElseThrow(
                () -> AppErrorException.of(
                        Error.RSC_NOT_FOUND, "goodsdata no: %d".formatted(goodsDataNo)));

        // A.X Platform 에 파일 업로드 연동
        List<String> fileNameList = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
                .stream()
                .map(file -> file.getOriginalFileNm())
                .toList();

        String documentId = UUID.randomUUID().toString();

        AXMetaFileDto axMetaFileDto = AXMetaFileDto.builder()
                .gdsNo(goodsData.getGoods().getGoodsNo())
                .documentId(documentId)
                .documentReleaseDate(nowOffsetDateTime())
                .headline(goodsData.getGoodsDataNm())
                .synopsis(goodsData.getGoodsDataExpn() != null ? goodsData.getGoodsDataExpn()
                        : "" + goodsData.getGoodsDataSummaryCntt() != null ? goodsData.getGoodsDataSummaryCntt() : "")
                .contributor(goodsData.getDataContributorNm())
                .authors(List.of(""))
                .documentFileName(fileNameList)
                .language("")
                .countries(List.of(""))
                .build();

        goodsData.updateMetaDataUploadStatus(false);
        goodsData.updateGoodsDataDistinguishId(documentId);

        publisher.publishEvent(new AXUploadFileEvent(goodsData.getGoodsDataNo(), axMetaFileDto, false));
    }
}
