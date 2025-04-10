package com.sk.domain.shared.service;

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
import com.sk.domain.shared.enums.EmailSendStatusName;
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

    @Async
    @Transactional(noRollbackFor = AppErrorException.class)
    public void sendMail(String to, String subject, String data) {

        EmailAccessLog emailAccessLog = EmailAccessLog.of(to, subject, data, EmailSendStatusName.PENDING);
        emailAccessLogRepository.save(emailAccessLog);

        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(setContext(data), true);
            javaMailSender.send(message);

            emailAccessLog.successUpdate(EmailSendStatusName.SUCCESS);

            log.info("Mail sent successfully");
        } catch (Exception e) {
            emailAccessLog.failUpdate(EmailSendStatusName.FAILED, e.getMessage());
            
            log.error("Mail sending failed", e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                    String.format("Mail sending failed: %s", e.getMessage()));
        }
    }

    public String setContext(String data) {
        Context context = new Context();
        context.setVariable("data", data);
        return templateEngine.process("sample", context);
    }

}
