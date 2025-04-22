package com.sk.domain.shared.repository.master.tlm.custom;

import static com.sk.domain.shared.entity.master.tlm.QEmailAccessLog.emailAccessLog;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.notification.AdminNotificationRes;
import com.sk.domain.admin.dto.response.notification.QAdminNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;
import com.sk.domain.mi.dto.response.notification.QMiNotificationRes;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomEmailAccessLogRepositoryImpl implements CustomEmailAccessLogRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AdminNotificationRes> findNotificationsByConditions(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(emailAccessLog.isDeleted.isFalse());
        if (search != null && !search.isEmpty()) {
            builder.and(user.userNm.contains(search).or(emailAccessLog.emailCntt.contains(search)));
        }

        List<AdminNotificationRes> content = queryFactory.select(new QAdminNotificationRes(
                emailAccessLog.emailSendSn,
                emailAccessLog.emailReceiveUserLoginId,
                emailAccessLog.emailTitleNm,
                emailAccessLog.emailCntt,
                emailAccessLog.emailSendStatusNm,
                emailAccessLog.emailSendErrCntt,
                emailAccessLog.emailSendDtm,
                user.userNm,
                user.userRoleCd,
                user.positionNm,
                user.jobGradeNm,
                user.onDutyNm,
                user.departmentNm,
                customerCompany.customerCompanyNo,
                customerCompany.customerCompanyNm,
                emailAccessLog.createUserId,
                emailAccessLog.createDt,
                emailAccessLog.modifyUserId,
                emailAccessLog.modifyDt))
                .from(emailAccessLog)
                .leftJoin(user).on(emailAccessLog.emailReceiveUserLoginId.eq(user.userLoginId))
                .leftJoin(user.customerCompany, customerCompany)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(builder)
                .orderBy(emailAccessLog.emailSendSn.desc())
                .groupBy(emailAccessLog.emailSendSn)
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory.select(emailAccessLog.count())
                .from(emailAccessLog)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<MiNotificationRes> findNotificationsWithinSevenDays(int page, int size) {
        // Pageable pageable = PageRequest.of(page - 1, size);

        // BooleanBuilder builder = new BooleanBuilder();
        // builder.and(emailAccessLog.isDeleted.isFalse());
        // builder.and(emailAccessLog.emailSendDtm.between(LocalDateTime.now().minusDays(7), LocalDateTime.now()));

        // List<MiNotificationRes> content = queryFactory.select(new QMiNotificationRes(
        //         emailAccessLog.emailSendSn,
        //         emailAccessLog.emailReceiveUserLoginId,
        //         emailAccessLog.emailTitleNm,
        //         emailAccessLog.emailCntt,
        //         emailAccessLog.emailSendStatusNm,
        //         emailAccessLog.emailSendErrCntt,
        //         emailAccessLog.emailSendDtm,
        //         user.userNm,
        //         user.userRoleCd,
        //         user.positionNm,
        //         user.jobGradeNm,
        //         user.onDutyNm,
        //         user.departmentNm,
        //         customerCompany.customerCompanyNo,
        //         customerCompany.customerCompanyNm,
        //         emailAccessLog.createUserId,
        //         emailAccessLog.createDt,
        //         emailAccessLog.modifyUserId,
        //         emailAccessLog.modifyDt))
        //         .from(emailAccessLog)
        //         .leftJoin(user).on(emailAccessLog.emailReceiveUserLoginId.eq(user.userLoginId))
        //         .leftJoin(user.customerCompany, customerCompany)
        //         .offset(pageable.getOffset())
        //         .limit(pageable.getPageSize())
        //         .where(builder)
        //         .orderBy(emailAccessLog.emailSendSn.desc())
        //         .groupBy(emailAccessLog.emailSendSn)
        //         .fetch();

        // JPQLQuery<Long> countQuery = queryFactory.select(emailAccessLog.count())
        //         .from(emailAccessLog)
        //         .where(builder);

        // return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        return null;
    }

}
