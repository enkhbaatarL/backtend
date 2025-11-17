package com.sk.domain.mi.service;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.activitylog.ActivityLogReq;
import com.sk.domain.mi.dto.response.activitylog.ActivityLogListRes;
import com.sk.domain.shared.entity.master.tmd.ActivityLog;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.ActivityLogType;
import com.sk.domain.shared.repository.master.tmd.ActivityLogRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final CustomerCompanyRepository customerCompanyRepository;

    private final AuthorizationService authorizationService;

    /**
     * ActivityLog 목록 조회 (페이징)
     */
    public PaginationRes<ActivityLogListRes> getActivityLogList(
            int page,
            int size,
            Integer customerCompanyNo,
            String keyword,
            String eventTypes) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "evntDt", "actLogId"));

        List<ActivityLogType> aList = eventTypes != null ? Arrays.stream(eventTypes.split(","))
            .map(String::trim)
            .map(ActivityLogType::valueOf)
            .toList() : null;

        Page<ActivityLog> activityLogPage = activityLogRepository.findByCustomerCompanyNoAndEventTypes(
                customerCompanyNo,
                keyword,
                aList,
                pageable
        );

        Page<ActivityLogListRes> resultPage = activityLogPage.map(this::toActivityLogListRes);

        return PaginationRes.toRes(resultPage);
    }

    /**
     * ActivityLog 추가
     */
    @Transactional
    public Long createActivityLog(ActivityLogReq.Insert request) {

        Integer customerCompanyNo = authorizationService.getCompanyNoWithPriority();

        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalseAndIsUsedTrue(customerCompanyNo).orElse(null);

        // ActivityLog 생성
        ActivityLog activityLog = ActivityLog.of(
                customerCompany,
                customerCompanyNo,
                request.eventDate(),
                request.eventType(),
                request.accountName(),
                request.eventDetailExpression(),
                request.significanceLevel(),
                request.sourceName(),
                request.departmentName()
        );

        // 저장
        ActivityLog savedActivityLog = activityLogRepository.save(activityLog);

        log.info("ActivityLog 생성 완료. actLogId: {}", savedActivityLog.getActLogId());

        return savedActivityLog.getActLogId();
    }

    /**
     * ActivityLog -> ActivityLogListRes 변환
     */
    private ActivityLogListRes toActivityLogListRes(ActivityLog activityLog) {
        return new ActivityLogListRes(
                activityLog.getActLogId(),
                activityLog.getEvntDt(),
                activityLog.getEvntType() != null ? activityLog.getEvntType().getName() : null,
                activityLog.getAcctNm(),
                activityLog.getEvntDtlExpn(),
                activityLog.getSigLvl() != null ? activityLog.getSigLvl().getName() : null,
                activityLog.getSrcNm(),
                activityLog.getCreateUser() != null ? activityLog.getCreateUser().getUserNm() : null,
                activityLog.getDeptNm()
        );
    }
}
