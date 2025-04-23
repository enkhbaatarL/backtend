package com.sk.domain.shared.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.Properties.MailProperties;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
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

    private final String DATA_DETAIL_LINK = "https://mi.sk.com/data/detail";

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
        String goodsDataNm = goodsCriticalValueSetting.getGoodsDataSourceDetail().getGoodsData().getGoodsDataNm();
        String goodsDataSourceNm = goodsCriticalValueSetting.getGoodsDataSourceDetail().getGoodsDataSourceNm();
        String criticalValueSettingVol = goodsCriticalValueSetting.getCriticalValueSettingVol().toString();
        String criticalValueSettingUnitNm = goodsCriticalValueSetting.getCriticalValueSettingUnitNm();
        String notificationCriticalValueSeparateCd = goodsCriticalValueSetting.getNotificationCriticalValueSeparateCd().getDescription();
        String to = goodsCriticalValueSetting.getUser().getUserLoginId();

        // 메일 제목 설정
        String mailSubject = setMailTitle(dataCreatedDate);

        // 임계치 설정 내용 설정
        String criticalValueSettingContent = setCriticalValueSettingContent(criticalValueSettingVol, criticalValueSettingUnitNm,
                notificationCriticalValueSeparateCd);

        // 메일 내용 설정(html)
        String mailContentHtml = setMailContext(goodsDataNm, goodsDataSourceNm, criticalValueSettingContent);

        // 메일 내용 설정(단순 텍스트)
        String mailContentText = String.format("%s 이(가) %s", goodsDataSourceNm, criticalValueSettingContent);

        EmailAccessLog emailAccessLog = EmailAccessLog.of(to, mailSubject, mailContentText, EmailSendStatusName.PENDING,
                goodsCriticalValueSetting);
        emailAccessLogRepository.save(emailAccessLog);

        sendMail(to, mailSubject, mailContentHtml, emailAccessLog);
    }

    /**
     * 메일 발송
     * @param to
     * @param subject
     * @param content
     * @param emailAccessLog
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
     * 알람 메일 제목 설정
     * @param data
     * @return
     */
    public String setMailTitle(LocalDate data) {
        return String.format("[MI Platform] %s월 %s일 신규 알람 발생", data.getMonthValue(), data.getDayOfMonth());
    }

    /**
     * 알람 메일 내용 설정(html)
     * 
     * 파라미터는 엔티티명 기준
     * @param goodsDataNm
     * @param goodsDataSourceNm
     * @param criticalValueSettingVol
     * @param criticalValueSettingUnitNm
     * @param notificationCriticalValueSeparateCd
     * @return
     */
    public String setMailContext(String goodsDataNm, String goodsDataSourceNm, String criticalValueSettingContent) {
        Context context = new Context();
        context.setVariable("goodsDataNm", goodsDataNm); // {데이터 명}
        context.setVariable("goodsDataSourceNm", goodsDataSourceNm); // {정형 데이터 상세명}
        context.setVariable("criticalValueSettingContent", criticalValueSettingContent); // {정형 데이터 상세명}
        context.setVariable("detailLink", DATA_DETAIL_LINK); // 데이터 상세보기 링크
        return templateEngine.process("notification", context);
    }

    /**
     * 알람 메일 내용 설정(단순 텍스트)
     * @param dataName
     * @param calculationResult
     * @param percentage
     * @param detailLink
     * @return
     */
    public String setCriticalValueSettingContent(String criticalValueSettingVol, String criticalValueSettingUnitNm,
            String notificationCriticalValueSeparateCd) {
        // 조건에 따른 단위와 변경 유형 매핑
        Map<String, String[]> settings = Map.of(
                NotiCrtvlSptCd.DECRATE.getDescription(), new String[] { "%", notificationCriticalValueSeparateCd + " 초과" },
                NotiCrtvlSptCd.INCRATE.getDescription(), new String[] { "%", notificationCriticalValueSeparateCd + " 초과" },
                NotiCrtvlSptCd.UPPER.getDescription(), new String[] { criticalValueSettingUnitNm, "이상으로 " + notificationCriticalValueSeparateCd },
                NotiCrtvlSptCd.LOWER.getDescription(), new String[] { criticalValueSettingUnitNm, "이하로 " + notificationCriticalValueSeparateCd });

        // 기본값: 빈 단위와 변경 유형
        String[] setting = settings.getOrDefault(notificationCriticalValueSeparateCd, new String[] { "", "" });
        String valueUnit = setting[0];
        String changeType = setting[1];

        // 출력 형식: {값}{단위} {변경유형}
        return String.format("%s%s %s", criticalValueSettingVol, valueUnit, changeType);
    }
}
