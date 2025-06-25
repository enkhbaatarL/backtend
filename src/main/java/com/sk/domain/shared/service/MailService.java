package com.sk.domain.shared.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.properties.MailProperties;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.EmailSendStatusName;
import com.sk.domain.shared.enums.NotiCrtvlSptCd;
import com.sk.domain.shared.repository.master.tlm.EmailAccessLogRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MailService {
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final EmailAccessLogRepository emailAccessLogRepository;
    private final String NOTIFICATION_LOGO_PATH = "templates/src/marketintelligence.png";

    @Value("${server.domain}")
    private String serverDomain;

    /**
     * 알람 메일 발송
     *
     * @param goodsCriticalValueSetting 알람 설정 정보
     * @param dataCreatedDate           데이터 발생일 (알람 발송일 X, 임계치 데이터 발생 O)
     */
    @Async
    @Transactional(noRollbackFor = AppErrorException.class)
    public void sendNotificationMail(GoodsCriticalValueSetting goodsCriticalValueSetting, LocalDate dataCreatedDate) {
        // 알람 설정 정보 조회
        Goods goods = goodsCriticalValueSetting.getGoodsDataSourceDetail().getGoodsData().getGoods();
        GoodsData goodsData = goodsCriticalValueSetting.getGoodsDataSourceDetail().getGoodsData();
        GoodsDataSourceDetail goodsDataSourceDetail = goodsCriticalValueSetting.getGoodsDataSourceDetail();
        String criticalValueSettingVol = goodsCriticalValueSetting.getCriticalValueSettingVol() != null
                ? goodsCriticalValueSetting.getCriticalValueSettingVol().setScale(2, RoundingMode.HALF_UP).toString()
                : "";
        String criticalValueSettingUnitNm = goodsCriticalValueSetting.getCriticalValueSettingUnitNm();
        String notificationCriticalValueSeparateCd = goodsCriticalValueSetting.getNotificationCriticalValueSeparateCd().getDescription();
        String to = goodsCriticalValueSetting.getUser().getUserLoginId();

        // 메일 제목 설정
        String mailSubject = setMailTitle(dataCreatedDate);

        // 임계치 설정 내용 설정
        String criticalValueSettingContent = setCriticalValueSettingContent(criticalValueSettingVol, criticalValueSettingUnitNm,
                notificationCriticalValueSeparateCd);

        // 임계치 설정 내용 설정(html)
        String criticalValueSettingHtmlContentBody = setCriticalValueSettingHtmlContentBody(criticalValueSettingUnitNm,
                notificationCriticalValueSeparateCd);

        String notificationCriticalValueSeparate = switch (goodsCriticalValueSetting.getNotificationCriticalValueSeparateCd()) {
            case DECRATE, INCRATE -> goodsCriticalValueSetting.getNotificationCriticalValueSeparateCd().getDescription();
            case UPPER, LOWER -> "";
        };

        // 메일 내용 설정(html)
        String mailContentHtml = setMailContext(goods, goodsData, goodsDataSourceDetail, criticalValueSettingVol, criticalValueSettingHtmlContentBody,
                notificationCriticalValueSeparate);

        // 메일 내용 설정(단순 텍스트)
        String mailContentText = String.format("%s 이(가) %s", goodsDataSourceDetail.getGoodsDataSourceNm(), criticalValueSettingContent);

        EmailAccessLog emailAccessLog = EmailAccessLog.of(to, mailSubject, mailContentText, EmailSendStatusName.PENDING,
                goodsCriticalValueSetting);
        emailAccessLogRepository.save(emailAccessLog);

        ClassPathResource logo = new ClassPathResource(NOTIFICATION_LOGO_PATH);

        sendMailWithInlineLogo(to, mailSubject, mailContentHtml, emailAccessLog, logo);
    }

    /**
     * 메일 발송
     * @param to 받는 사람
     * @param subject 메일 제목
     * @param content 메일 내용
     */
    public void sendMailNoAccessLog(String to, String subject, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            javaMailSender.send(message);

            log.info("Mail sent successfully");
        } catch (Exception e) {

            log.error("Mail sending failed", e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                    String.format("Mail sending failed: %s", e.getMessage()));
        }
    }

    /**
     * 메일 발송
     * @param to 받는 사람
     * @param subject 메일 제목
     * @param content 메일 내용
     * @param emailAccessLog 이메일 전송 로그
     */
    public void sendMail(String to, String subject, String content, EmailAccessLog emailAccessLog) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            javaMailSender.send(message);

            emailAccessLog.sendEmailSuccessUpdate(EmailSendStatusName.SUCCESS);

            log.info("Mail sent successfully");
        } catch (Exception e) {
            emailAccessLog.sendEmailFailUpdate(EmailSendStatusName.FAILED, e.getMessage());

            log.error("Mail sending failed", e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                    String.format("Mail sending failed: %s", e.getMessage()));
        }
    }

    /**
     * 메일 발송
     * @param to 받는 사람
     * @param subject 메일 제목
     * @param content 메일 내용
     * @param emailAccessLog 이메일 전송 로그
     * @param logo 로고 이미지
     */
    public void sendMailWithInlineLogo(String to, String subject, String content, EmailAccessLog emailAccessLog, ClassPathResource logo) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            helper.addInline("logo", logo);
            javaMailSender.send(message);

            emailAccessLog.sendEmailSuccessUpdate(EmailSendStatusName.SUCCESS);

            log.info("Mail sent successfully");
        } catch (Exception e) {
            emailAccessLog.sendEmailFailUpdate(EmailSendStatusName.FAILED, e.getMessage());

            log.error("Mail sending failed", e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                    String.format("Mail sending failed: %s", e.getMessage()));
        }
    }

    /**
     * 알람 메일 제목 설정
     * @param data 데이터 발생일
     * @return
     */
    public String setMailTitle(LocalDate data) {
        return String.format("[MI Platform] %s월 %s일 신규 알람 발생", data.getMonthValue(), data.getDayOfMonth());
    }

    /**
     * 알람 메일 내용 설정(html)
     *
     * 파라미터는 엔티티명 기준
     * @param goodsDataNm 데이터 명
     * @param goodsDataSourceNm 정형 데이터 상세명
     * @param criticalValueSettingVol 알람 임계치 값
     * @param criticalValueSettingUnitNm 알람 임계치 단위
     * @param notificationCriticalValueSeparateCd 알람 임계치 구분
     * @return
     */
    public String setMailContext(Goods goods, GoodsData goodsData, GoodsDataSourceDetail goodsDataSourceDetail, String criticalValueSettingVol,
            String criticalValueSettingContentBody,
            String notificationCriticalValueSeparateCd) {
        Context context = new Context();
        context.setVariable("goodsDataNm", goodsData.getGoodsDataNm()); // {데이터 명}
        context.setVariable("goodsDataSourceNm", goodsDataSourceDetail.getGoodsDataSourceNm()); // {정형 데이터 상세명}

        context.setVariable("settingSeparate", notificationCriticalValueSeparateCd); // {알람 임계치 구분}
        context.setVariable("settingVolume", criticalValueSettingVol); // {알람 임계치 값}
        context.setVariable("settingUnit", criticalValueSettingContentBody); // {알람 임계치 단위}

        // 데이터 상세보기 링크
        String detailLink = String.format("%s/mi/data-detail/%s/%s", serverDomain, goods.getGoodsNo(), goodsData.getGoodsDataNo());
        context.setVariable("detailLink", detailLink);
        return templateEngine.process("notification", context);
    }

    /**
     * 알람 메일 내용 설정 DB 저장용 (단순 텍스트)
     * @param dataName
     * @param calculationResult
     * @param percentage
     * @param detailLink
     * @return
     */
    public String setCriticalValueSettingContent(String criticalValueSettingVol, String criticalValueSettingUnitNm,
            String notificationCriticalValueSeparateCd) {

        Map<String, String[]> settings = Map.of(
                NotiCrtvlSptCd.DECRATE.getDescription(), new String[] {"%", notificationCriticalValueSeparateCd + " 초과"},
                NotiCrtvlSptCd.INCRATE.getDescription(), new String[] {"%", notificationCriticalValueSeparateCd + " 초과"},
                NotiCrtvlSptCd.UPPER.getDescription(), new String[] {criticalValueSettingUnitNm, "이상으로 " + notificationCriticalValueSeparateCd},
                NotiCrtvlSptCd.LOWER.getDescription(), new String[] {criticalValueSettingUnitNm, "이하로 " + notificationCriticalValueSeparateCd});

        // 기본값: 빈 단위와 변경 유형
        String[] setting = settings.getOrDefault(notificationCriticalValueSeparateCd, new String[] {"", ""});
        String valueUnit = setting[0];
        String changeType = setting[1];

        // 출력 형식: {값}{단위} {변경유형}
        return valueUnit != null ? String.format("%s%s %s", criticalValueSettingVol, valueUnit, changeType)
                : String.format("%s %s", criticalValueSettingVol, changeType);
    }

    /**
    * 알람 메일 내용 설정 HTML 설정용 (단순 텍스트)
    * @param dataName
    * @param calculationResult
    * @param percentage
    * @param detailLink
    * @return
    */
    public String setCriticalValueSettingHtmlContentBody(String criticalValueSettingUnitNm,
            String notificationCriticalValueSeparateCd) {

        Map<String, String[]> settings = Map.of(
                NotiCrtvlSptCd.DECRATE.getDescription(), new String[] {"%", " 초과"},
                NotiCrtvlSptCd.INCRATE.getDescription(), new String[] {"%", " 초과"},
                NotiCrtvlSptCd.UPPER.getDescription(), new String[] {criticalValueSettingUnitNm, "이상으로 " + notificationCriticalValueSeparateCd},
                NotiCrtvlSptCd.LOWER.getDescription(), new String[] {criticalValueSettingUnitNm, "이하로 " + notificationCriticalValueSeparateCd});

        // 기본값: 빈 단위와 변경 유형
        String[] setting = settings.getOrDefault(notificationCriticalValueSeparateCd, new String[] {"", ""});
        String valueUnit = setting[0];
        String changeType = setting[1];

        // 출력 형식: {값}{단위} {변경유형}
        return valueUnit != null ? String.format("%s %s", valueUnit, changeType) : String.format("%s", changeType);
    }
}
