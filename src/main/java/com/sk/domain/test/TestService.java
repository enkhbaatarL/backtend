package com.sk.domain.test;

import com.sk.common.utils.BcryptHashUtil;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.io.FileOutputStream;
import java.io.File;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.RedisTemplate;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.service.ExcelService;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tcm.ComponentInfoRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.AuthService;
import com.sk.domain.shared.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final ComponentInfoRepository componentInfoRepository;
    private final ExcelService excelService;
    private final AuthService authService;
    private final MailService mailService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final BcryptHashUtil bcryptHashUtil;

    private final GoodsDataRepository goodsDataRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;

    public TestRes.GetUserInfo getUserInfo(TestReq.getUserInfo testReq) {
        return TestRes.GetUserInfo.of(testReq);
    }

    /**
     * 사용자 로그인
     *
     * @param testLoginReq
     * @return
     */
    @Transactional
    public GenerateTokenRes userLogin(TestReq.userLogin testLoginReq, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                UserRoleCd.USER)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "User information could not be found. userLoginId: " + testLoginReq.loginUserId()));

        return authService.generateToken(user, request, response);
    }

    /**
     * 관리자 로그인
     *
     * @param testLoginReq
     * @return
     */
    @Transactional
    public GenerateTokenRes adminLogin(TestReq.adminLogin testLoginReq, HttpServletResponse response) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 26.07.29 취약점 진단 수정, '표준 라이브러리의 널 반환 값 역참조-102275' -> getRequestAttributes()는 요청 스레드 밖에서 null 반환
        if (attributes == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "Request context is not available.");
        }

        HttpServletRequest request = attributes.getRequest();
        String clientIp = request.getRemoteAddr();

        log.info("clientIp: {}", clientIp);

        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(testLoginReq.loginUserId(),
                testLoginReq.userRoleCd())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format(
                        "User information could not be found. userLoginId: %s, userRoleCd: %s", testLoginReq.loginUserId(),
                        testLoginReq.userRoleCd())));

        adminPermissionRepository.findByIdUserNoAndIdIpAddressAndIsDeletedFalse(user.getUserNo(), clientIp)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("The IP address is not allowed: %s", clientIp)));

        return authService.generateToken(user, request, response);
    }

    /**
     * 토큰 검증
     *
     * @param testVerifyTokenReq
     * @return
     */
    public Boolean checkToken(TestReq.verifyToken testVerifyTokenReq) {
        return jwtUtil.isTokenValid(testVerifyTokenReq.token());
    }

    @Transactional
    public String getToken(String loginUserId, UserRoleCd role, HttpServletRequest request) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(loginUserId, role)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        return jwtUtil.generateAccessToken(user, request.getServerName());
    }

    public String delete(Integer id) {
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(id)
                .orElseThrow();
        customerCompany.softDelete();
        return "ok";
    }

    // public List<Map<String, Object>> getDynamicData(TestReq.uploadStructuredDataFile testReq) {
    // ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(testReq.componentTypeId())
    // .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
    // String.format("Component information could not be found. componentTypeId: %s", testReq.componentTypeId())));

    // return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(componentInfo));
    // }

    /**
     * 정형 데이터 파일 검수 후 업로드
     *
     * @param file
     * @param testReq
     * @return
     */
    public FileUploadRes uploadStructuredDataFile(MultipartFile file, TestReq.uploadStructuredDataFile testReq) {
        try {
            // 컴포넌트 타입 조회
            ComponentInfo componentInfo = componentInfoRepository.findByComponentTypeIdAndIsDeletedFalse(testReq.componentTypeId())
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                            String.format("Component information could not be found. componentTypeId: %s", testReq.componentTypeId())));

            // 엑셀 파일 검수 후 업로드
            boolean isUploaded = excelService.uploadStructuredData(file, componentInfo, testReq.criteriaDt(), null);

            return null;
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, e.getMessage());
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    public FileUploadRes uploadStructuredDataFileTest(MultipartFile file) {
        return null;
    }

    public String encodeToBase64(byte[] bytesToEncode) {
        return Base64.getEncoder().encodeToString(bytesToEncode);
    }

    public byte[] decodeFromBase64(String stringToDecode) {
        return Base64.getDecoder().decode(stringToDecode);
    }

    public Boolean sendEmail(TestReq.sendEmail testReq) {
        String to = testReq.email();
        String subject = "TEST MAIL";
        String content = """
                <h1>TEST MAIL</h1>
                <p>TEST MAIL</p>
                """;;
        mailService.sendMailNoAccessLog(to, subject, content);
        return true;
    }


    /**
     * BCrypt 해시 생성
     *
     * @param password 평문 비밀번호
     * @return BCrypt 해시된 비밀번호
     */
    public String generateBcryptHash(String password) {
        try {
            return bcryptHashUtil.generateBcryptHash(password);
        } catch (Exception e) {
            log.error("BCrypt 해시 생성 실패: {}", e.getMessage());
            throw AppErrorException.of(ResultCode.Error.SYS_UNKNOWN, "BCrypt 해시 생성에 실패했습니다.");
        }
    }

    /**
     * BCrypt 해시 검증
     *
     * @param password 평문 비밀번호
     * @param hashedPassword BCrypt 해시된 비밀번호
     * @return 검증 결과
     */
    public Boolean verifyBcryptPassword(String password, String hashedPassword) {
        try {
            return bcryptHashUtil.verifyBcryptPassword(password, hashedPassword);
        } catch (Exception e) {
            log.error("BCrypt 해시 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 비밀번호 강도 검증
     *
     * @param password 검증할 비밀번호
     * @return 검증 결과
     */
    public Boolean validatePasswordStrength(String password) {
        try {
            return bcryptHashUtil.validatePasswordStrength(password);
        } catch (Exception e) {
            log.error("비밀번호 강도 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 상품 데이터와 연결된 파일 정보의 일치 여부를 검증합니다.
     */
    public void validateGoodsDataFiles(boolean exportToFile, String countStr, Integer gdsNo) {
        int pageSize = 1000;
        int pageNumber = 0;
        long totalProcessedCount = 0;
        Page<GoodsData> goodsDataPage;
        List<Map<String, Object>> inconsistentDataList = new ArrayList<>();

        Long limit = null;
        if (!"all".equalsIgnoreCase(countStr)) {
            try {
                limit = Long.parseLong(countStr);
            } catch (NumberFormatException e) {
                log.warn("[VALIDATION_PARAM] Invalid count parameter: {}. Falling back to 'all'.", countStr);
            }
        }

        log.info("[START_VALIDATION] validateGoodsDataFiles started. Export to file: {}, Limit: {}, GdsNo: {}",
                exportToFile, limit == null ? "all" : limit, gdsNo == null ? "all" : gdsNo);

        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            if (gdsNo != null) {
                goodsDataPage = goodsDataRepository.findByGoods_GoodsNoAndIsDeletedFalse(gdsNo, pageable);
            } else {
                goodsDataPage = goodsDataRepository.findAll(pageable);
            }

            for (GoodsData goodsData : goodsDataPage.getContent()) {
                if (limit != null && totalProcessedCount >= limit) {
                    break;
                }

                totalProcessedCount++;

                if (totalProcessedCount % 10000 == 0) {
                    log.info("[VALIDATION_PROGRESS] Processed {} records...", totalProcessedCount);
                }

                if (Boolean.TRUE.equals(goodsData.getIsDeleted())) {
                    continue;
                }

                Integer goodsDataNo = goodsData.getGoodsDataNo();

                // 1. 직접 연결된 파일들 (file_rel_obj_dstg_no = goodsDataNo, file_rel_obj_spt_cd = 'GUDAT')
                Set<Long> directFileNos = uploadedFileRepository.findByRelationshipObjectInfo(goodsDataNo, "GUDAT")
                        .stream()
                        .map(UploadedFile::getFileNo)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // 2. 카테고리 내역을 통한 연결들 (gds_data_mnagt_spt_cd = 'GUDAT')
                Set<Long> categoryFileNos = goodsDataCategoryRepository.findGoodsDataCategoryByGoodsDataNo(goodsDataNo)
                        .stream()
                        .filter(c -> "GUDAT".equals(c.getGoodsDataManagementSeparateCd()) && !Boolean.TRUE.equals(c.getIsDeleted()))
                        .map(GoodsDataCategory::getGoodsDataManagementNo)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // 검증 로직: 둘다 비어있거나 같으면 성공(스킵), 다르면 로그 출력
                if (directFileNos.equals(categoryFileNos)) {
                    continue;
                }

                // 다르면 로그 출력
                String providerNm = goodsData.getGoods() != null && goodsData.getGoods().getProvider() != null
                        ? goodsData.getGoods().getProvider().getProviderNm() : "N/A";
                String goodsNm = goodsData.getGoods() != null ? goodsData.getGoods().getGoodsNm() : "N/A";
                String goodsDataNm = goodsData.getGoodsDataNm();

                log.error("[DATA_INCONSISTENCY] GoodsDataNo: {}, DirectFileNos: {}, CategoryFileNos: {}, Provider: {}, Goods: {}, GoodsData: {}",
                        goodsDataNo, directFileNos, categoryFileNos, providerNm, goodsNm, goodsDataNm);

                if (exportToFile) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("GoodsDataNo", goodsDataNo);
                    data.put("DirectFileNos", directFileNos.toString());
                    data.put("CategoryFileNos", categoryFileNos.toString());
                    data.put("Provider", providerNm);
                    data.put("Goods", goodsNm);
                    data.put("GoodsData", goodsDataNm);
                    inconsistentDataList.add(data);
                }
            }

            if (limit != null && totalProcessedCount >= limit) {
                break;
            }

            pageNumber++;
        } while (goodsDataPage.hasNext());

        if (exportToFile && !inconsistentDataList.isEmpty()) {
            writeInconsistentDataToExcel(inconsistentDataList);
        }

        log.info("[END_VALIDATION] validateGoodsDataFiles completed. Total processed: {}", totalProcessedCount);
    }

    private void writeInconsistentDataToExcel(List<Map<String, Object>> dataList) {
        String logDir = "log";
        File dir = new File(logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "inconsistent_goods_data_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        File file = new File(dir, fileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inconsistent Data");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"GoodsDataNo", "DirectFileNos", "CategoryFileNos", "Provider", "Goods", "GoodsData"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Data
            int rowNum = 1;
            for (Map<String, Object> data : dataList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data.get("GoodsDataNo").toString());
                row.createCell(1).setCellValue(data.get("DirectFileNos").toString());
                row.createCell(2).setCellValue(data.get("CategoryFileNos").toString());
                row.createCell(3).setCellValue(data.get("Provider").toString());
                row.createCell(4).setCellValue(data.get("Goods").toString());
                row.createCell(5).setCellValue(data.get("GoodsData").toString());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            log.info("[VALIDATION_EXPORT] Inconsistent data exported to: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("[VALIDATION_EXPORT_ERROR] Failed to write Excel file: {}", e.getMessage(), e);
        }
    }
    // public Boolean deleteRedisCache(TestReq.deleteRedisCache testReq) {
    //     // redisTemplate.delete(testReq.key());
    //     return true;
    // }

    // public Map<String, Object> getRedisCache(String key, int size) {
    //     List<String> keys = new ArrayList<>();

    //     // SCAN 옵션 설정
    //     ScanOptions scanOptions = ScanOptions.scanOptions()
    //             .match(key)
    //             .count(size)
    //             .build();

    //     // Redis 연결을 통해 SCAN 실행
    //     try (var cursor = redisTemplate.getConnectionFactory().getConnection().scan(scanOptions)) {
    //         while (cursor.hasNext()) {
    //             keys.add(new String(cursor.next()));
    //         }
    //     }

    //     // 키에 해당하는 값 조회
    //     Map<String, Object> result = keys.stream()
    //             .collect(Collectors.toMap(
    //                     k -> k,
    //                     k -> redisTemplate.opsForValue().get(k)
    //             ));

    //     return result;
    // }
}
