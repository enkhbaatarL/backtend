package com.sk.domain.admin.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataFileReq;
import com.sk.domain.admin.dto.request.provider.StructuredDataReq;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataReq;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.dto.response.file.GoodsDataFileUploadRes;
import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.dto.response.provider.ComponentInfoRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.admin.dto.response.provider.UnstructuredDataRes;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;
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
    private final AdminFileService adminFileService;
    private final ExcelService excelService;
    private final UnitConvertRepository unitConvertRepository;
    private final GoodsDataUnitConvertRepository goodsDataUnitConvertRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager masterTransactionManager;
    private final PlatformTransactionManager myBatisTransactionManager;

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
    public PaginationRes<GoodsDataRes> getAllGoodsData(int companyNo, Role role, int page, int size, String keyword, int providerNo, int goodsNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 프로바이더 -> 상품 -> 상품 데이타 목록 조회
            return PaginationRes.toRes(goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoForAdmin(providerNo, goodsNo, keyword, pageable));
        } else if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 프로바이더(등록 O) -> 상품(권한 O) -> 상품 데이타 목록 조회
            return PaginationRes
                    .toRes(goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoForOperator(companyNo, providerNo, goodsNo, keyword, pageable));
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
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
    public List<GoodsDataNameRes> getAllGoodsDataNm(int companyNo, Role role, int providerNo, int goodsNo) {

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 프로바이더 -> 상품 -> 상품 데이타명 목록 조회
            return goodsDataRepository.findGoodsDataNamesByProviderNoAndGoodsNoForAdmin(providerNo, goodsNo);
        } else if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 프로바이더(등록 O) -> 상품(권한 O) -> 상품 데이타명 목록 조회
            return goodsDataRepository.findGoodsDataNamesByProviderNoAndGoodsNoForOperator(companyNo, providerNo, goodsNo);
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
    }

    /**
    * 데이터 상세 조회합니다.
    *
    * @param user 사용자 정보
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @param goodsDataNo 상품 데이타 번호
    * @return GoodsDataDetailRes
    */
    public GoodsDataDetailRes getGoodsData(int companyNo, Role role, int providerNo, int goodsNo, int goodsDataNo) {

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 프로바이더 -> 상품 -> 상품 데이타 조회
            GoodsData goodsData = goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    "provider no: %d, goods no: %d, goodsdata no: %d".formatted(providerNo, goodsNo, goodsDataNo)));

            List<CategoryDataRes> goodsDataCategoryList = goodsDataCategoryRepository.findCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo());

            List<StructuredDataRes> structuredDataList = goodsDataCategoryRepository.findStructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

            List<UnstructuredDataRes> unstructuredDataList = goodsDataCategoryRepository
                    .findUnstructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

            return GoodsDataDetailRes.of(goodsData, goodsDataCategoryList, structuredDataList, unstructuredDataList);
        } else if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 프로바이더(등록 O) -> 상품(권한 O) -> 상품 데이타 조회

            // 프로바이더 등록 유무 체크
            customerCompanyProviderMappingRepository
                    .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            // 상품 권한 유무 체크
            customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            GoodsData goodsData = goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    "provider no: %d, goods no: %d, goodsdata no: %d".formatted(providerNo, goodsNo, goodsDataNo)));

            List<CategoryDataRes> goodsDataCategoryList = goodsDataCategoryRepository.findCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo());

            List<StructuredDataRes> structuredDataList = goodsDataCategoryRepository.findStructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

            List<UnstructuredDataRes> unstructuredDataList = goodsDataCategoryRepository
                    .findUnstructuredDataByGoodsDataNo(goodsData.getGoodsDataNo());

            return GoodsDataDetailRes.of(goodsData, goodsDataCategoryList, structuredDataList, unstructuredDataList);
        }

        throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
    }

    /**
    * 데이터 등록: 신규등록
    *
    * @param user 사용자 정보
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @param GoodsDataCreateReq
    * @return
    */
    @Transactional
    public void createGoodsData(int companyNo, Role role, int providerNo, int goodsNo, GoodsDataCreateReq dto) {

        if (RoleUtil.isAdmin(role)) {
            // 관리자: 상품이 퍼블릭일 경우 등록 가능
            Goods goods = goodsRepository.findByGoodsNo(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    "provider no: %d, goods no: %d".formatted(providerNo, goodsNo)));

            if (!goods.getIsPublic()) {
                throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
            }

            internalCreateGoodsData(goods, null, dto);

        } else if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 상품이 프라이빗일 경우 등록 가능
            customerCompanyProviderMappingRepository
                    .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            Goods goods = goodsRepository.findByGoodsNo(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    "provider no: %d, goods no: %d".formatted(providerNo, goodsNo)));

            if (goods.getIsPublic()) {
                throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
            }

            CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));

            internalCreateGoodsData(goods, customerCompany, dto);
        }
    }

    private void internalCreateGoodsData(Goods goods, CustomerCompany customerCompany, GoodsDataCreateReq dto) {
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

                // 컴포넌트 타입 조회
                ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(structuredData.componentTypeId())
                        .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                String.format("Component information could not be found. componentTypeId: %s", structuredData.componentTypeId())));

                GoodsDataSourceDetail savedGoodsDataSourceDetail = goodsDataSourceDetailRepository
                        .save(GoodsDataSourceDetail.of(savedGoodsData, componentInfo.getGoodsDataSourceNm(), componentInfo.getTargetDbNm(),
                                componentInfo.getTargetTableNm(), componentInfo.getQueryConditionCntt(), "goodsDataSourceExpn",
                                componentInfo.getGoodsDataSourceUnitNm(), componentInfo.getTableColumnListNm(), componentInfo, null, "backend"));

                List<UnitConvert> unitConverts = unitConvertRepository.findByConvertPreviousUnitNmAndIsDeletedFalse(savedGoodsDataSourceDetail.getGoodsDataSourceUnitNm());

                goodsDataUnitConvertRepository.saveAll(unitConverts.stream()
                        .map(unitConvert -> GoodsDataUnitConvert.of(savedGoodsDataSourceDetail, unitConvert))
                        .toList());

                // gds_data_keywd_nm 명은 카테고리 정한 뒤에 임의 값을 넣을 필요가 있을때 사용하는 값입니다. 발간일자의 날자값, 산업구분의 산업상세내역
                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, structuredData.componentTypeId(), null,
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
    * @param user 사용자 정보
    * @param providerNo 데이타 프로바이더 번호
    * @param goodsNo 상품 번호
    * @param GoodsDataUpdateReq
    * @return
    */
    @Transactional
    public void updateGoodsData(int companyNo, Role role, int providerNo, int goodsNo, int goodsDataNo, GoodsDataUpdateReq dto) {
        if (RoleUtil.isAdmin(role)) {
            // 관리자: 상품이 퍼블릭일 경우 수정 가능
            Goods goods = goodsRepository.findByGoodsNo(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    "provider no: %d, goods no: %d".formatted(providerNo, goodsNo)));

            if (!goods.getIsPublic()) {
                throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
            }

            GoodsData goodsData = goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goodsdata no: %d".formatted(goodsDataNo)));

            internalUpdateGoodsData(goodsData, dto);
        }

        if (RoleUtil.isOperator(role) || RoleUtil.isPlusOperator(role)) {
            // 운영자: 상품이 프라이빗일 경우 수정 가능
            customerCompanyProviderMappingRepository
                    .findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(companyNo, providerNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "provider no: %d".formatted(providerNo)));

            customerCompanyGoodsMappingRepository.findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(companyNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goods no: %d".formatted(goodsNo)));

            Goods goods = goodsRepository.findByGoodsNo(providerNo, goodsNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                                    "provider no: %d, goods no: %d".formatted(providerNo, goodsNo)));

            if (goods.getIsPublic()) {
                throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
            }

            GoodsData goodsData = goodsDataRepository.findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(providerNo, goodsNo, goodsDataNo)
                    .orElseThrow(
                            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "goodsdata no: %d".formatted(goodsDataNo)));

            internalUpdateGoodsData(goodsData, dto);
        }

    }

    private void internalUpdateGoodsData(GoodsData goodsData, GoodsDataUpdateReq dto) {
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
                List<Long> oldFileNos = adminFileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GDATS.getCode())
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
                    adminFileService.updateFile(
                            fileNo,
                            goodsData.getGoodsDataNo(),
                            FileRelationshipObjectSeparateCode.GDATS.getCode(),
                            DateUtil.endYmd());

					ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(structuredData.componentTypeId())
							.orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
									String.format("Component information could not be found. componentTypeId: %s", structuredData.componentTypeId())));

					GoodsDataSourceDetail savedGoodsDataSourceDetail = goodsDataSourceDetailRepository
					.save(GoodsDataSourceDetail.of(goodsData, componentInfo.getGoodsDataSourceNm(), componentInfo.getTargetDbNm(),
							componentInfo.getTargetTableNm(), componentInfo.getQueryConditionCntt(), null,
							componentInfo.getGoodsDataSourceUnitNm(), componentInfo.getTableColumnListNm(), componentInfo, null, "backend"));

                    List<UnitConvert> unitConverts = unitConvertRepository.findByConvertPreviousUnitNmAndIsDeletedFalse(savedGoodsDataSourceDetail.getGoodsDataSourceUnitNm());

                    goodsDataUnitConvertRepository.saveAll(unitConverts.stream()
                            .map(unitConvert -> GoodsDataUnitConvert.of(savedGoodsDataSourceDetail, unitConvert))
                            .toList());

                    // gds_data_keywd_nm 명은 카테고리 정한 뒤에 임의 값을 넣을 필요가 있을때 사용하는 값입니다. 발간일자의 날자값, 산업구분의 산업상세내역
                    goodsDataCategoryRepository.save(GoodsDataCategory.of(goodsData, structuredData.componentTypeId(), null,
                            GoodsDataManagementSeparateCode.GDATS.getCode(), Long.valueOf(savedGoodsDataSourceDetail.getGoodsDataSourceNo())));
                }
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
    public GoodsDataFileUploadRes uploadStructuredDataFile(int userNo, String companyCd, Role role, MultipartFile file, boolean isPublic,
            StructuredDataFileReq structuredDataFileReq) throws IOException {

        // TODO: JPA, MyBatis 트랜잭션 통합 관리 필요(XA 도입 또는 데이터 소스 분리 등 방법 필요)

        // JPA, MyBatis 트랜잭션 각각 관리
        // TransactionTemplate jpaTxTemplate = new TransactionTemplate(masterTransactionManager);
        // jpaTxTemplate.setReadOnly(false);
        // TransactionTemplate myBatisTxTemplate = new TransactionTemplate(myBatisTransactionManager);

        // 1. JPA: 조회 (Select는 )
        ComponentInfo componentInfo = componentInfoRepository
                .findByComponentTypeIdAndIsDeletedFalse(structuredDataFileReq.componentTypeId())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        String.format("Component information could not be found. componentTypeId: %s",
                                structuredDataFileReq.componentTypeId())));

        // User 조회 (외부 트랜잭션에서 가져온 user는 새로 만든 트랜잭션의 영속성 컨텍스트로 관리하지 않아서 Lazy 조회 불가)
        // User loadedUser = jpaTxTemplate.execute(status -> {
        // return userRepository.findByUserNoAndIsDeletedFalse(user.getUserNo())
        // .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "User not found"));
        // });

        // JPA / Mybatis 둘 중 하나라도 문제 생긴 경우 전체 롤백
        // try {
        // // 2. MyBatis: 데이터 업로드
        // myBatisTxTemplate.execute(status -> {
        // try {
        // excelService.uploadStructuredData(file, componentInfo);
        // return true;
        // } catch (IOException e) {
        // throw new RuntimeException("MyBatis upload failed", e);
        // }
        // });

        // // 3. JPA + S3: 파일 업로드
        // FileUploadRes fileUploadRes = jpaTxTemplate.execute(status -> {
        // try {
        // return adminFileService.uploadFile(loadedUser, file, isPublic);
        // } catch (Exception e) {
        // throw new RuntimeException("JPA/S3 upload failed", e);
        // }
        // });

        // return GoodsDataFileUploadRes.of(fileUploadRes.fileNo(), structuredDataFileReq.componentTypeId(),
        // fileUploadRes.fileNm(), fileUploadRes.fileSize(), fileUploadRes.fileUrl());
        // } catch (Exception e) {
        // log.error("Upload failed: {}", e.getMessage(), e);
        // throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        // }

        excelService.uploadStructuredData(file, componentInfo);

        FileUploadRes fileUploadRes = adminFileService.uploadFile(companyCd, role, file, isPublic);

        return GoodsDataFileUploadRes.of(fileUploadRes.fileNo(), structuredDataFileReq.componentTypeId(), fileUploadRes.fileNm(),
                fileUploadRes.fileSize(), fileUploadRes.fileUrl());
    }
}
