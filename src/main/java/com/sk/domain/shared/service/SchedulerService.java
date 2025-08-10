package com.sk.domain.shared.service;

import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.shared.dto.request.file.PdgImageReq;
import com.sk.domain.shared.dto.response.rpa.CrackSpreadDataRes;
import com.sk.domain.shared.entity.compositeKey.tmd.UserHistoryId;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tif.Futures;
import com.sk.domain.shared.entity.master.tif.FuturesSpread;
import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.entity.master.tmd.UserHistory;
import com.sk.domain.shared.enums.Extension;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tif.FuturesRepository;
import com.sk.domain.shared.repository.master.tif.FuturesSpreadRepository;
import com.sk.domain.shared.repository.master.tif.UserInfoRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.repository.master.tmd.UserHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.template.query.MybatisQueryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 스케쥴러용 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulerService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final MybatisQueryTemplate mybatisQueryTemplate;
    private final MailService mailService;
    private final FuturesRepository futuresRepository;
    private final FuturesSpreadRepository futuresSpreadRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final PdfToImagesProcessor pdfToImagesProcessor;

    private final String ALARM_PREV_DATA_ALIAS = "prev_data";
    private final int ALARM_BATCH_DAYS = 1;

    // brentTimeSpreadCalculate 계산용
    private static final String LLCC_01 = "LLCC.01";
    private static final String LLCC_03 = "LLCC.03";
    private static final String TIME_SPREAD_CODE = "LLCC.01-LLCC.03";
    private static final String CRACK_SPREAD_CODE = "WTI3-2-1SP";
    private static final int RB_WEIGHT = 2; // CRACK SPREAD 계산 시 RB 가중치
    private static final int HO_WEIGHT = 1; // CRACK SPREAD 계산 시 HO 가중치
    private static final int CL_WEIGHT = 3; // CRACK SPREAD 계산 시 CL 가중치
    private static final int BARREL_CONVERSION_FACTOR = 42; // 배럴 변환 계수
    private static final int BATCH_SIZE = 10000;

    /**
     * GDI 탈퇴 유저 soft delete 처리
     */
    @Transactional
    public Boolean processGdiUserSoftDelete() {
        // 인터페이스 테이블에서 매번 삭제된 유저 조회 하는데 User 테이블에서 삭제된 유저 상관 없이 조회 하기 때문에 비효율적
        List<UserInfo> deletedUserInfos = userInfoRepository.findByIsDeletedTrue();

        if (deletedUserInfos.isEmpty()) {
            return Boolean.TRUE;
        }

        List<User> users = userRepository.findByUserInfosAndIsDeletedFalse(deletedUserInfos);

        if (!users.isEmpty()) {
            users.stream()
                .peek(User::userDelete)
                .collect(Collectors.toList());

            LocalDateTime now = LocalDateTime.now();
            List<UserHistory> histories = users.stream()
                .map(user -> UserHistory.builder()
                    .id(new UserHistoryId(user.getUserNo(), now))
                    .isOtherCompanyServiceAccessPossible(user.getIsOtherServiceAccessPossible())
                    .user(user)
                    .build())
                .toList();
            userHistoryRepository.saveAll(histories);
        }

        return Boolean.TRUE;
    }

    /**
     * 임계치를 확인하고 알림을 발송한다.
     *
     * @return 알림 발송 결과
     */
    @Transactional
    public Boolean createNotification() {
        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = goodsCriticalValueSettingRepository
            .findAllByIsDeletedFalse();

        /**
         * 배치는 24시간 기준으로 작동하며 조회 데이터역시 현재 기준 최근 24시간 이내에 등록된 데이터만을 조회 비교한다.
         * 임계치 설정별 데이터 리스트 조회
         */
        for (GoodsCriticalValueSetting goodsCriticalValueSettingData : goodsCriticalValueSettings) {
            // 알림 설정이 되어있는 경우에만 알림 발송
            if (goodsCriticalValueSettingData.getUser().getIsUserNotificationSetting()) {
                String targetDatabaseNm = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getTargetDbNm();
                String targetTableNm = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getTargetTableNm();
                String dbQueryConditionCntt = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getQueryConditionCntt();

                String selectColumns = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getTableColumnListNm();
                String[] selectQueryArray = selectColumns.split(",");

                /**
                 * 현재 정책으로 TableColumnListNm의 칼럼 순서는
                 * 메트릭 : 날짜(date), 기본 컬럼(x), 컬럼1(y1), 컬럼2(y2), 컬럼3(y3), ...
                 * 캔들 : 날짜(date), 종가(close), 시가(open), 고가(high), 저가(low)
                 */
                String dateColumn = selectQueryArray[0].trim(); // 날짜 컬럼(eg: 'date')
                String defaultSelectColumn = selectQueryArray[1].trim(); // 기본 컬럼(알람 기준 컬럼)(eg: 'trdprc_1')

                // 정규 표현식 패턴을 이용해서 getTableColumnList 에서 칼럼, 데이터 추출
                String pattern = "([a-zA-Z_]+)\\s*=\\s*'([^']*)'";
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(dbQueryConditionCntt);

                StringJoiner whereKeyJoiner = new StringJoiner(",");

                // 모든 매칭된 key 수집
                while (matcher.find()) {
                    String key = matcher.group(1); // 첫 번째 그룹: key
                    whereKeyJoiner.add(key);
                }

                String whereKey = whereKeyJoiner.length() > 0 ? whereKeyJoiner.toString() : null;

                String columns = mybatisQueryTemplate.getSelectQueryForAlarm(selectColumns, targetTableNm, dateColumn, defaultSelectColumn,
                    ALARM_PREV_DATA_ALIAS, whereKey);

                String orderByQuery = mybatisQueryTemplate.getOrderByQueryForAlarm(dateColumn, defaultSelectColumn);

                // 날짜 범위 설정
                LocalDate startDate = LocalDate.now().minusDays(ALARM_BATCH_DAYS);
                LocalDate endDate = LocalDate.now();

                LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIN);
                LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.MAX);

                String startDateStr = DateUtil.toYyyyMmDdHms(startDateTime);
                String endDateStr = DateUtil.toYyyyMmDdHms(endDateTime);

                String whereQuery = mybatisQueryTemplate.getWhereQueryForAlarm(startDateStr, endDateStr, dbQueryConditionCntt);

                String targetTableAlias = mybatisQueryTemplate.getTableAliasForAlarm(targetTableNm);

                // 임계치 설정 1개에 대한 데이터 리스트
                List<Map<String, Object>> rowDatas = mybatisDynamicQueryService
                    .getDynamicRawQuery(DynamicDataDetailReq.ofOrderBy(targetDatabaseNm, targetTableAlias, columns, whereQuery, orderByQuery));

                for (Map<String, Object> rowData : rowDatas) {
                    Object createdTimeData = rowData.get(dateColumn); // 데이터 발생일 (date 칼럼)
                    Object prevData = rowData.get(ALARM_PREV_DATA_ALIAS); // 임계치 비교를 위한 이전 데이터(Default 데이터)
                    Object currentData = rowData.get(defaultSelectColumn); // 임계치 비교를 위한 기준 데이터(Default 데이터)

                    // currentData가 null인 경우 스킵 (첫번째 row 데이터는 preData가 null)
                    if (currentData == null) {
                        log.warn("Skipping row due to null data: currentData={}", currentData);
                        continue;
                    }

                    // Object를 BigDecimal로 변환 (int 또는 decimal 처리)
                    BigDecimal prevValue = null;
                    BigDecimal currentValue = null;
                    // currentData는 null이면 알림 발송 안함
                    if (prevData == null) {
                        continue;
                    }

                    // prevData는 null일 수 있음 (첫번째 row 데이터는 prevData가 null 또는 데이터상 null일 수 있음, null 이여도 로직상 문제 없음)
                    if (prevData != null) {
                        // prevData를 BigDecimal로 변환
                        try {
                            prevValue = new BigDecimal(prevData.toString());
                        } catch (NumberFormatException e) {
                            log.error("Failed to convert data to BigDecimal: prevData={}", prevData, e);
                            continue;
                        }
                    }

                    // currentData를 BigDecimal로 변환
                    try {
                        currentValue = new BigDecimal(currentData.toString());
                    } catch (NumberFormatException e) {
                        log.error("Failed to convert data to BigDecimal: currentData={}", currentData, e);
                        continue;
                    }

                    BigDecimal criticalValueSettingVol = goodsCriticalValueSettingData.getCriticalValueSettingVol();
                    boolean isMatch = false;

                    // 데이터, 임계치 설정 비교
                    switch (goodsCriticalValueSettingData.getNotificationCriticalValueSeparateCd()) {
                        case UPPER:
                            /**
                             * 절대값 비교 currentData가(절대값) criticalValueSettingVol(기준치)보다 상승
                             * 바로 직전 데이터가 임계치룰 넘었을 경우에는 알림 발송 안함
                             *
                             * 알림 발송 조건
                             * isPrevValueValue 값이 null이 아니면서 임계치 미만일 경우
                             * isCurrentValueValue 값이 null이 아니면서 임계치 이상일 경우
                             */
                            boolean isUpperPrevValueValue = false;
                            boolean isUpperCurrentValueValue = false;
                            if(prevValue != null){
                                // 바로 직전 데이터가 임계치 미만일 경우
                                isUpperPrevValueValue = prevValue.compareTo(criticalValueSettingVol) < 0;
                            }
                            if(currentValue != null){
                                // 현재 데이터가 임계치 이상일 경우
                                isUpperCurrentValueValue = currentValue.compareTo(criticalValueSettingVol) >= 0;
                            }

                            isMatch = isUpperPrevValueValue && isUpperCurrentValueValue;
                            break;
                        case LOWER:
                            /**
                             * 절대값 비교 currentData가(절대값) criticalValueSettingVol(기준치)보다 하락
                             * 바로 직전 데이터가 임계치룰 넘었을 경우에는 알림 발송 안함
                             *
                             * 알림 발송 조건
                             * isPrevValueValue 값이 null이 아니면서 임계치 초과일 경우
                             * isCurrentValueValue 값이 null이 아니면서 임계치 이하일 경우
                             */
                            boolean isLowerPrevValueValue = false;
                            boolean isLowerCurrentValueValue = false;
                            if(prevValue != null){
                                // 바로 직전 데이터가 임계치 초과일 경우
                                isLowerPrevValueValue = prevValue.compareTo(criticalValueSettingVol) > 0;
                            }
                            if(currentValue != null){
                                // 현재 데이터가 임계치 이하일 경우
                                isLowerCurrentValueValue = currentValue.compareTo(criticalValueSettingVol) <= 0;
                            }

                            isMatch = isLowerPrevValueValue && isLowerCurrentValueValue;
                            break;
                        case INCRATE:
                            /**
                             * 퍼센트 변화 비교 currentData가(절대값) prevData보다 criticalValueSettingVol% 상승
                             * 바로 직전 데이터가 null일 경우 알림 발송 안함
                             */
                            if (prevValue == null) {
                                log.warn("Skipping row due to null data: prevData={}", prevData);
                                continue;
                            }

                            // currentData가 prevData보다 criticalValueSettingVol% 상승
                            BigDecimal percentIncrease = calculatePercentChange(currentValue, prevValue);
                            isMatch = percentIncrease.compareTo(criticalValueSettingVol) >= 0;
                            break;
                        case DECRATE:
                            /**
                             * 퍼센트 변화 비교 currentData가(절대값) prevData보다 criticalValueSettingVol% 하락
                             * 바로 직전 데이터가 null일 경우 알림 발송 안함
                             */
                            if (prevValue == null) {
                                log.warn("Skipping row due to null data: prevData={}", prevData);
                                continue;
                            }

                            // currentData가 prevData보다 criticalValueSettingVol% 하락
                            BigDecimal percentDecrease = calculatePercentChange(currentValue, prevValue);
                            isMatch = percentDecrease.compareTo(criticalValueSettingVol.negate()) <= 0;
                            break;
                        default:
                            log.warn("Unknown notification critical value separate code: {}",
                                goodsCriticalValueSettingData.getNotificationCriticalValueSeparateCd());
                            break;
                    }

                    // 조건에 맞는 데이터 추가
                    if (isMatch) {
                        // 이메일 발송
                        mailService.sendNotificationMail(goodsCriticalValueSettingData, DateUtil.parseDate(createdTimeData));
                    }
                }
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 퍼센트 변화 계산
     *
     * @param current
     * @param previous
     * @return
     */
    private BigDecimal calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Previous value is zero, returning zero for percent change");
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * PDF 파일을 이미지로 변환하여 S3에 업로드 한다.
     *
     * @return Boolean
     */
    @Transactional
    public Boolean processPdfToImages(PdgImageReq pdgImageReq) {

        log.info("[PDF-TO-IMAGES] Start processing target PDFs...");

        // 타켓 비정형데이터 조회
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findPdfToImagesTarget(Extension.PDF.getCode(), FileRelationshipObjectSeparateCode.GUDAT.getCode());

        List<UploadedFile> filtered = uploadedFiles.stream()
            .filter(file -> CollectionUtils.isEmpty(pdgImageReq.fileNos()) || pdgImageReq.fileNos().contains(file.getFileNo()))
            .filter(file -> pdgImageReq.createdAfter() == null || !file.getCreateDt().toLocalDate().isBefore(pdgImageReq.createdAfter()))
            .toList();

        log.info("[PDF-TO-IMAGES] Total target PDF files: {}", filtered.size());

        filtered.forEach(uploadedFile -> {

            try {
                // 트랜잭션이 분리된 별도 메소드 호출
                pdfToImagesProcessor.process(uploadedFile);
            } catch (Exception e) {
                log.error("[PDF-TO-IMAGES] Failed to process fileNo: {}. Skipping to next file.", uploadedFile.getFileNo(), e);
            }

        });

        log.info("[PDF-TO-IMAGES] All files processed successfully.");
        return Boolean.TRUE;
    }

    /**
     * TIF_Futures Spread 계산 후 업로드
     *
     * @return Boolean
     */
    @Transactional
    public Boolean processTifFuturesSpread() {

        crackSpreadCalculate();
        brentTimeSpreadCalculate();

        return Boolean.TRUE;
    }

    /*
     * SK 에너지 Crack Spread 계산 후 저장 (비동기 처리)
     */
    @Async
    @Transactional
    public void crackSpreadCalculate() {
        // 마지막 날짜 조회
        LocalDateTime lastDate = futuresSpreadRepository.findLastDateByCode(CRACK_SPREAD_CODE);
        log.info("Last date for code {}: {}", CRACK_SPREAD_CODE, lastDate);

        // 데이터 조회
        List<CrackSpreadDataRes> dataList = futuresSpreadRepository.calculateCrackSpreadData(lastDate);

        // 기존 FuturesSpread 레코드 배치 조회 (중복 체크)
        List<LocalDateTime> dates = dataList.stream().map(CrackSpreadDataRes::date).collect(Collectors.toList());
        List<FuturesSpread> existingSpreads = futuresSpreadRepository.findByCodeAndDateIn(CRACK_SPREAD_CODE, dates);
        Map<LocalDateTime, FuturesSpread> existingByDate = existingSpreads.stream()
            .collect(Collectors.toMap(FuturesSpread::getDate, s -> s));
        log.debug("Existing spreads by date: {}", existingByDate);

        List<FuturesSpread> spreadsToSave = new ArrayList<>();

        // 계산 및 UPSERT 로직
        for (CrackSpreadDataRes dto : dataList) {
            // 계산: ps = (RB_WEIGHT * BARREL_CONVERSION_FACTOR * RB) + (HO_WEIGHT * BARREL_CONVERSION_FACTOR * HO) - (CL_WEIGHT * CL)
            BigDecimal ps = null;
            if (dto.clPs() != null && dto.hoPs() != null && dto.rbSettle() != null) {
                ps = BigDecimal.valueOf(RB_WEIGHT * BARREL_CONVERSION_FACTOR).multiply(dto.rbSettle())
                    .add(BigDecimal.valueOf(HO_WEIGHT * BARREL_CONVERSION_FACTOR).multiply(dto.hoPs()))
                    .subtract(BigDecimal.valueOf(CL_WEIGHT).multiply(dto.clPs()));
            }

            // 타입 변환: vm, oi
            Integer vm = (dto.clVm() != null && dto.rbTotCntrVol() != null) ? dto.rbTotCntrVol().intValue() : null;
            Integer oi = (dto.clOi() != null && dto.rbOpint1() != null) ? dto.rbOpint1().intValue() : null;

            // 매핑: po, ph, pl
            BigDecimal po = (dto.clPo() != null && dto.rbOpenPrc() != null) ? dto.rbOpenPrc() : null;
            BigDecimal ph = (dto.clPh() != null && dto.rbHigh1() != null) ? dto.rbHigh1() : null;
            BigDecimal pl = (dto.clPl() != null && dto.rbLow1() != null) ? dto.rbLow1() : null;

            // UPSERT 로직
            FuturesSpread target;
            if (existingByDate.containsKey(dto.date())) {
                // 기존 레코드 업데이트
                FuturesSpread existing = existingByDate.get(dto.date());
                target = FuturesSpread.builder()
                    .dataRegistrationSn(existing.getDataRegistrationSn())
                    .code(existing.getCode())
                    .date(existing.getDate())
                    .ps(ps)
                    .vm(vm)
                    .oi(oi)
                    .po(po)
                    .ph(ph)
                    .pl(pl)
                    .build();
            } else {
                // 새 레코드 생성
                target = FuturesSpread.builder()
                    .code(CRACK_SPREAD_CODE)
                    .date(dto.date())
                    .ps(ps)
                    .vm(vm)
                    .oi(oi)
                    .po(po)
                    .ph(ph)
                    .pl(pl)
                    .build();
            }
            spreadsToSave.add(target);
        }

        log.debug("Prepared {} spreads to save, first date: {}, last date: {}",
            spreadsToSave.size(),
            spreadsToSave.isEmpty() ? null : spreadsToSave.get(0).getDate(),
            spreadsToSave.isEmpty() ? null : spreadsToSave.get(spreadsToSave.size() - 1).getDate());

        // 배치 저장
        saveInBatches(spreadsToSave);
    }

    /*
     * Brent 1-3 Time Spread 계산 후 저장 (비동기 처리)
     */
    @Async
    @Transactional
    public void brentTimeSpreadCalculate() {
        // 마지막 날짜 조회
        LocalDateTime lastDate = futuresSpreadRepository.findLastDateByCode(TIME_SPREAD_CODE);
        log.info("Last date for code {}: {}", TIME_SPREAD_CODE, lastDate);
        // LLCC.01, LLCC.03 데이터 조회
        Map<LocalDateTime, List<Futures>> futuresByDate = futuresRepository.findByCodesAfterDate(
            Arrays.asList(LLCC_01, LLCC_03), lastDate);
        log.debug("Fetched {} date groups, first date: {}, last date: {}",
            futuresByDate.size(),
            futuresByDate.keySet().stream().min(LocalDateTime::compareTo).orElse(null),
            futuresByDate.keySet().stream().max(LocalDateTime::compareTo).orElse(null));

        List<FuturesSpread> spreadsToSave = new ArrayList<>();

        // 기존 FuturesSpread 레코드 배치 조회 (중복 체크)
        List<LocalDateTime> dates = new ArrayList<>(futuresByDate.keySet());
        List<FuturesSpread> existingSpreads = futuresSpreadRepository.findByCodeAndDateIn(TIME_SPREAD_CODE, dates);
        Map<LocalDateTime, FuturesSpread> existingByDate = existingSpreads.stream()
            .collect(Collectors.toMap(FuturesSpread::getDate, s -> s));

        // 동일 date의 LLCC.01과 LLCC.03 데이터 매칭 및 계산
        for (Map.Entry<LocalDateTime, List<Futures>> entry : futuresByDate.entrySet()) {
            LocalDateTime date = entry.getKey();
            List<Futures> futuresForDate = entry.getValue();

            // LLCC.01과 LLCC.03 찾기
            Futures llcc01 = futuresForDate.stream()
                .filter(f -> LLCC_01.equals(f.getCode()))
                .findFirst()
                .orElse(null);
            Futures llcc03 = futuresForDate.stream()
                .filter(f -> LLCC_03.equals(f.getCode()))
                .findFirst()
                .orElse(null);

            // 둘 다 존재해야 계산
            if (llcc01 != null && llcc03 != null) {
                // 계산
                BigDecimal ps = safeSubtract(llcc01.getPs(), llcc03.getPs());
                Integer vm = safeSubtract(llcc01.getVm(), llcc03.getVm());
                Integer oi = safeSubtract(llcc01.getOi(), llcc03.getOi());
                BigDecimal po = safeSubtract(llcc01.getPo(), llcc03.getPo());
                BigDecimal ph = safeSubtract(llcc01.getPh(), llcc03.getPh());
                BigDecimal pl = safeSubtract(llcc01.getPl(), llcc03.getPl());

                // Upsert 로직: 기존 레코드 있으면 builder로 업데이트, 없으면 새 레코드
                FuturesSpread spread;
                if (existingByDate.containsKey(date)) {
                    // 기존 레코드 업데이트
                    FuturesSpread existing = existingByDate.get(date);
                    spread = FuturesSpread.builder()
                        .dataRegistrationSn(existing.getDataRegistrationSn())
                        .code(existing.getCode())
                        .date(existing.getDate())
                        .ps(ps)
                        .vm(vm)
                        .oi(oi)
                        .po(po)
                        .ph(ph)
                        .pl(pl)
                        .build();
                } else {
                    // 새 레코드 생성
                    spread = FuturesSpread.builder()
                        .code(TIME_SPREAD_CODE)
                        .date(date)
                        .ps(ps)
                        .vm(vm)
                        .oi(oi)
                        .po(po)
                        .ph(ph)
                        .pl(pl)
                        .build();
                }

                spreadsToSave.add(spread);
            }
        }

        log.debug("Prepared {} spreads to save, first date: {}, last date: {}",
            spreadsToSave.size(),
            spreadsToSave.isEmpty() ? null : spreadsToSave.get(0).getDate(),
            spreadsToSave.isEmpty() ? null : spreadsToSave.get(spreadsToSave.size() - 1).getDate());

        // 배치 저장
        saveInBatches(spreadsToSave);
    }

    /**
     * 배치 저장
     *
     * @param spreads
     */
    private void saveInBatches(List<FuturesSpread> spreads) {
        for (int i = 0; i < spreads.size(); i += BATCH_SIZE) {
            List<FuturesSpread> batch = spreads.subList(i, Math.min(i + BATCH_SIZE, spreads.size()));
            futuresSpreadRepository.saveAll(batch);
            futuresSpreadRepository.flush();
        }
    }

    /**
     * BigDecimal 빼기 (null 처리)
     *
     * @param a
     * @param b
     * @return
     */
    private BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return null;
        }
        return a.subtract(b);
    }

    /**
     * 정수 빼기 (null 처리)
     *
     * @param a
     * @param b
     * @return
     */
    private Integer safeSubtract(Integer a, Integer b) {
        if (a == null || b == null) {
            return null;
        }
        return a - b;
    }
}
