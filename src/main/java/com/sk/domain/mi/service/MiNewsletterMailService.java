package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.properties.MailProperties;
import com.sk.domain.shared.client.ChartRendererClient;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterSubscriptionRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.mail.internet.MimeMessage;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MiNewsletterMailService {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MILLIS = 1000;

    private static final Pattern CID_REFERENCE_PATTERN = Pattern.compile("cid:([A-Za-z0-9._@-]+)");
    private static final Pattern CID_ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9._@-]+$");
    private static final Pattern NON_CID_ALLOWED_PATTERN = Pattern.compile("[^A-Za-z0-9._@-]");
    private static final Pattern EMAIL_ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String EDIT_STATUS_SAVED = "EDIT_SAVED";

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final NewsletterMasterRepository newsletterMasterRepository;
    private final NewsletterPublishHistoryRepository newsletterPublishHistoryRepository;
    private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;
    private final AuthorizationService authorizationService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * 오리지널 회차 메일 발송
     *
     * 정책:
     * - recipientEmails 입력 시 해당 수신자에게 발송
     * - recipientEmails 미입력 시 구독자에게 발송
     */
    @Transactional
    public void sendOriginalIssueMail(
            Integer newsletterId,
            Integer issueNo,
            String subject,
            String htmlBody,
            List<MultipartFile> images,
            List<String> recipientEmails,
            User actorUser) {

        validateSendRequest(subject, htmlBody);
        // validateOriginalIssueState(newsletterId, issueNo, actorUser);

        List<String> recipients = resolveOriginalRecipients(newsletterId, recipientEmails);
        if (recipients.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER,
                    "No recipients resolved. Provide recipientEmails or configure newsletter subscriptions.");
        }

        List<InlineResource> inlineResources = validateAndBuildInlineResources(htmlBody, images);
        sendMailWithRetry(MailRecipients.toOnly(recipients), subject, htmlBody, inlineResources);

        log.info("Newsletter original mail sent. newsletterId: {}, issueNo: {}, recipients: {}", newsletterId, issueNo, recipients.size());
    }

    /**
     * 스케줄러 자동 발송 - Playwright로 캡처한 메일 콘텐츠를 구독자에게 개별 발송
     *
     * 정책:
     * - 수신자는 각자 본인만 수신자로 보임 (개별 발송)
     * - 수신자 목록: 구독 테이블 기반 조회
     */
    @Transactional
    public void sendOriginalIssueMailFromCapture(
            Integer newsletterId,
            Integer issueNo,
            String subject,
            ChartRendererClient.MailContents mailContents) {

        validateSendRequest(subject, mailContents.htmlBody());

        List<String> recipients = resolveOriginalRecipients(newsletterId, null);
        if (recipients.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER,
                "No recipients resolved. Configure newsletter subscriptions.");
        }

        List<InlineResource> inlineResources =
            buildInlineResourcesFromCaptured(mailContents.htmlBody(), mailContents.images());

        // 수신자 개별 발송 - 각자 본인만 수신자로 보임
        for (String recipient : recipients) {
            sendIndividualMailWithRetry(recipient, subject, mailContents.htmlBody(), inlineResources);
        }

        log.info("Newsletter captured mail sent. newsletterId: {}, issueNo: {}, recipients: {}",
            newsletterId, issueNo, recipients.size());
    }

    /**
     * 리비전 메일 발송
     *
     * 정책:
     * - recipientEmails 입력 시 해당 수신자에게 발송
     * - recipientEmails 미입력 시 발송 대상 없음으로 처리
     */
    @Transactional
    public void sendRevisionMail(
            Integer newsletterId,
            Integer issueNo,
            String subject,
            String htmlBody,
            List<MultipartFile> images,
            List<String> recipientEmails,
            List<String> ccEmails,
            List<String> bccEmails,
            String scheduledAt,
            User actorUser) {

        validateSendRequest(subject, htmlBody);
        // validateScheduledAtForNowOnly(scheduledAt);
//        validateRevisionOwnershipAndState(newsletterId, issueNo, actorUser);
        if (actorUser == null || actorUser.getUserNo() == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "actor user is required");
        }

        MailRecipients recipients = resolveRevisionRecipients(recipientEmails, ccEmails, bccEmails);
        if (recipients.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "Provide at least one valid email in recipientEmails, ccEmails, or bccEmails.");
        }
        if (recipients.to().isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "recipientEmails must include at least one valid email.");
        }

        List<InlineResource> inlineResources = validateAndBuildInlineResources(htmlBody, images);
        sendMailWithRetry(recipients, subject, htmlBody, inlineResources);

        log.info("Newsletter revision mail sent. newsletterId: {}, issueNo: {}, to: {}, cc: {}, bcc: {}",
                newsletterId, issueNo, recipients.to().size(), recipients.cc().size(), recipients.bcc().size());
    }

    private void validateSendRequest(String subject, String htmlBody) {
        if (StringUtils.isBlank(subject)) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "subject is required");
        }
        if (StringUtils.isBlank(htmlBody)) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "htmlBody is required");
        }
    }

    private void validateOriginalIssueState(Integer newsletterId, Integer issueNo, User actorUser) {
        if (newsletterId == null || issueNo == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "newsletterId and issueNo are required");
        }
        if (actorUser == null || actorUser.getCustomerCompany() == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "actor user is required");
        }

        NewsletterMaster newsletterMaster = newsletterMasterRepository.findByNewsletterMasterIdAndIsDeletedFalse(
                        newsletterId,
                        actorUser.getCustomerCompany().getCustomerCompanyNo())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "newsletterId not found: " + newsletterId));

        NewsletterPublishHistory issue = newsletterPublishHistoryRepository.findByNewsletterMasterAndIssueNo(newsletterMaster, issueNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "issueNo not found: " + issueNo));

        if (Boolean.TRUE.equals(issue.getIsDeleted())) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Issue is deleted");
        }

        if (issue.getPublishStatusCode() != PublishStatusCode.PUBLISHED) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
                    "Original issue is not in PUBLISHED state");
        }
    }

    private void validateRevisionOwnershipAndState(Integer newsletterId, Integer issueNo, User actorUser) {
        if (newsletterId == null || issueNo == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "newsletterId and issueNo are required");
        }
        if (actorUser == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "actor user is required");
        }
        int userNo = authorizationService.getUserNo();

        String sql = """
                SELECT e.ownr_usr_no, e.rvsn_stat_cd
                  FROM tmd_nwsltrusrrvsnm e
                  JOIN tmd_nwsltrorgissm o
                    ON o.nws_ltr_org_iss_id = e.nws_ltr_org_iss_id
                 WHERE o.nws_ltr_mstr_id = :newsletterId
                   AND o.issno = :issueNo
                   AND e.ownr_usr_no = :userNo
                   AND e.del_yn = 'N'
                """;

        List<RevisionStateRow> rows = namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("newsletterId", newsletterId)
                        .addValue("issueNo", issueNo)
                        .addValue("userNo", userNo),
                (rs, rowNum) -> new RevisionStateRow(
                        rs.getObject("ownr_usr_no"),
                        rs.getString("rvsn_stat_cd"))
        );

        if (rows.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "revision not found. newsletterId: %d, issueNo: %d".formatted(newsletterId, issueNo));
        }

        RevisionStateRow stateRow = rows.get(0);
        Integer ownerUserNo = toInteger(stateRow.ownerUserNo());
        if (ownerUserNo == null || !ownerUserNo.equals(userNo)) {
            throw AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "No permission for this revision");
        }

        if (!EDIT_STATUS_SAVED.equalsIgnoreCase(StringUtils.defaultString(stateRow.editStatus()))) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Revision is not in EDIT_SAVED state");
        }
    }

    private List<String> resolveOriginalRecipients(Integer newsletterId, List<String> recipientEmails) {
        List<String> explicitRecipients = normalizeRecipients(recipientEmails, "recipient");
        if (!explicitRecipients.isEmpty()) {
            return explicitRecipients;
        }
        return normalizeRecipients(newsletterSubscriptionRepository.findActiveSubscriberEmails(newsletterId), "recipient");
    }

    private MailRecipients resolveRevisionRecipients(
            List<String> recipientEmails,
            List<String> ccEmails,
            List<String> bccEmails) {

        List<String> toRecipients = normalizeRecipients(recipientEmails, "recipient");
        List<String> ccRecipients = filterDuplicatesByPriority(
                normalizeRecipients(ccEmails, "cc"),
                new LinkedHashSet<>(toRecipients));
        LinkedHashSet<String> occupied = new LinkedHashSet<>(toRecipients);
        occupied.addAll(ccRecipients);
        List<String> bccRecipients = filterDuplicatesByPriority(
                normalizeRecipients(bccEmails, "bcc"),
                occupied);

        return new MailRecipients(toRecipients, ccRecipients, bccRecipients);
    }

    private List<String> normalizeRecipients(List<String> recipientEmails, String fieldName) {
        if (recipientEmails == null || recipientEmails.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String email : recipientEmails) {
            if (StringUtils.isBlank(email)) {
                continue;
            }
            // Support both repeated form-data keys and comma-separated one-field input.
            String[] tokens = email.split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (!EMAIL_ALLOWED_PATTERN.matcher(trimmed).matches()) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                            "Invalid %s email: %s".formatted(fieldName, trimmed));
                }
                normalized.add(trimmed);
            }
        }

        return new ArrayList<>(normalized);
    }

    private List<String> filterDuplicatesByPriority(List<String> recipients, Set<String> higherPriorityRecipients) {
        if (recipients.isEmpty()) {
            return List.of();
        }

        List<String> filtered = new ArrayList<>();
        for (String recipient : recipients) {
            if (higherPriorityRecipients.contains(recipient)) {
                continue;
            }
            filtered.add(recipient);
        }
        return filtered;
    }

    private List<InlineResource> validateAndBuildInlineResources(String htmlBody, List<MultipartFile> images) {
        List<MultipartFile> normalizedImages = normalizeImages(images);
        List<String> normalizedCids = deriveCidsFromImages(normalizedImages);

        Set<String> htmlCidRefs = extractCidReferences(htmlBody);

        if (normalizedImages.isEmpty()) {
            if (!htmlCidRefs.isEmpty()) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                        "htmlBody contains cid references but no inline images were provided");
            }
            return List.of();
        }

        if (normalizedImages.size() != normalizedCids.size()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "images size and cids size must be equal");
        }

        Set<String> cidSet = new LinkedHashSet<>(normalizedCids);
        if (cidSet.size() != normalizedCids.size()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "duplicate cid is not allowed");
        }

        validateCidMatchesHtml(htmlCidRefs, cidSet);

        List<InlineResource> resources = new ArrayList<>();
        for (int i = 0; i < normalizedImages.size(); i++) {
            MultipartFile file = normalizedImages.get(i);
            String cid = normalizedCids.get(i);

            String contentType = normalizeImageContentType(file);
            resources.add(new InlineResource(cid, (InputStreamSource) file, contentType));
        }

        return resources;
    }

    private List<MultipartFile> normalizeImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<MultipartFile> normalized = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            normalized.add(image);
        }
        return normalized;
    }

    private List<String> deriveCidsFromImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<String> cids = new ArrayList<>(images.size());
        Map<String, Integer> nameCounter = new HashMap<>();

        for (MultipartFile image : images) {
            String originalFilename = StringUtils.defaultIfBlank(image.getOriginalFilename(), "image");
            String base = stripExtension(originalFilename).toLowerCase();
            base = NON_CID_ALLOWED_PATTERN.matcher(base).replaceAll("-");
            base = base.replaceAll("^-+|-+$", "");
            if (base.isBlank()) {
                base = "image";
            }

            int seq = nameCounter.getOrDefault(base, 0);
            nameCounter.put(base, seq + 1);
            String candidate = seq == 0 ? base : base + "-" + seq;

            if (!CID_ALLOWED_PATTERN.matcher(candidate).matches()) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                        "Invalid cid derived from filename: " + originalFilename);
            }
            cids.add(candidate);
        }

        return cids;
    }

    private String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }

    private Set<String> extractCidReferences(String htmlBody) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        Matcher matcher = CID_REFERENCE_PATTERN.matcher(htmlBody);
        while (matcher.find()) {
            refs.add(matcher.group(1));
        }
        return refs;
    }

    private void validateCidMatchesHtml(Set<String> htmlCidRefs, Set<String> cidSet) {
        if (!htmlCidRefs.isEmpty()) {
            LinkedHashSet<String> missingInRequest = new LinkedHashSet<>(htmlCidRefs);
            missingInRequest.removeAll(cidSet);
            if (!missingInRequest.isEmpty()) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                        "Missing cids for htmlBody references: " + missingInRequest);
            }
        }

        LinkedHashSet<String> unusedCids = new LinkedHashSet<>(cidSet);
        unusedCids.removeAll(htmlCidRefs);
        if (!unusedCids.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "Provided cids not used in htmlBody: " + unusedCids);
        }
    }

    private String normalizeImageContentType(MultipartFile image) {
        String contentType = image.getContentType();
        if (StringUtils.isBlank(contentType) || !contentType.startsWith("image/")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "Only image/* content types are allowed: " + Objects.toString(contentType));
        }
        return contentType;
    }

    private void sendMailWithRetry(MailRecipients recipients, String subject, String htmlBody, List<InlineResource> inlineResources) {
        sendSingleMailWithRetry(recipients, subject, htmlBody, inlineResources);
    }

    private void sendSingleMailWithRetry(
            MailRecipients recipients,
            String subject,
            String htmlBody,
            List<InlineResource> inlineResources) {

        if (recipients == null || recipients.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                    "At least one recipient is required");
        }

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailProperties.getFrom());
                helper.setTo(recipients.to().toArray(String[]::new));
                if (!recipients.cc().isEmpty()) {
                    helper.setCc(recipients.cc().toArray(String[]::new));
                }
                if (!recipients.bcc().isEmpty()) {
                    helper.setBcc(recipients.bcc().toArray(String[]::new));
                }
                helper.setSubject(subject);

                // IMPORTANT: setText를 먼저 호출한 뒤 inline 리소스를 추가해야 메일 클라이언트 호환성이 높다.
                helper.setText(htmlBody, true);

                for (InlineResource inlineResource : inlineResources) {
                    helper.addInline(inlineResource.cid(), inlineResource.source(), inlineResource.contentType());
                }

                javaMailSender.send(message);
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_COUNT) {
                    throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                            "Mail sending failed after %d attempts: %s".formatted(MAX_RETRY_COUNT, e.getMessage()));
                }
                waitBeforeRetry(attempt, recipients, e);
            }
        }
    }

    private void waitBeforeRetry(int attempt, MailRecipients recipients, Exception e) {
        log.warn("Mail sending failed on attempt {} for recipients(count={}). Retrying after {}ms...",
                attempt, recipients.totalCount(), RETRY_DELAY_MILLIS, e);
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "Retry interrupted");
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateScheduledAtForNowOnly(String scheduledAt) {
        if (StringUtils.isBlank(scheduledAt)) {
            return;
        }

        OffsetDateTime scheduledDateTime = parseScheduledAt(scheduledAt);
        if (scheduledDateTime.isAfter(OffsetDateTime.now())) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
                    "Scheduled send is not supported by this API. send immediately without future scheduledAt.");
        }
    }

    private OffsetDateTime parseScheduledAt(String scheduledAt) {
        try {
            return OffsetDateTime.parse(scheduledAt);
        } catch (DateTimeException ignored) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(scheduledAt);
                return localDateTime.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
            } catch (DateTimeException ex) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER,
                        "scheduledAt must be ISO-8601 format (e.g. 2026-03-07T10:30:00+09:00)");
            }
        }
    }

    /**
     * base64 이미지 목록을 InlineResource 로 변환 (Playwright 캡처 경로)
     * CID는 프론트엔드가 명시적으로 지정하므로 파일명 파생 없이 그대로 사용한다.
     */
    private List<InlineResource> buildInlineResourcesFromCaptured(
            String htmlBody, List<ChartRendererClient.CapturedImage> capturedImages) {

        if (capturedImages == null || capturedImages.isEmpty()) {
            return List.of();
        }

        List<InlineResource> resources = new ArrayList<>();
        for (ChartRendererClient.CapturedImage img : capturedImages) {
            byte[] bytes = Base64.getDecoder().decode(img.data());
            resources.add(new InlineResource(img.cid(), new ByteArrayResource(bytes), img.contentType()));
        }
        return resources;
    }

    /**
     * 수신자 1명에게 개별 발송 (To 필드에 본인만 노출)
     */
    private void sendIndividualMailWithRetry(
            String recipient,
            String subject,
            String htmlBody,
            List<InlineResource> inlineResources) {

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailProperties.getFrom());
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                for (InlineResource inlineResource : inlineResources) {
                    helper.addInline(inlineResource.cid(), inlineResource.source(), inlineResource.contentType());
                }

                javaMailSender.send(message);
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_COUNT) {
                    throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                        "Mail sending failed after %d attempts for %s: %s"
                            .formatted(MAX_RETRY_COUNT, recipient, e.getMessage()));
                }
                waitBeforeRetry(attempt, MailRecipients.toOnly(List.of(recipient)), e);
            }
        }
    }

    private record RevisionStateRow(Object ownerUserNo, String editStatus) {
    }

    private record MailRecipients(List<String> to, List<String> cc, List<String> bcc) {
        private MailRecipients {
            to = List.copyOf(to);
            cc = List.copyOf(cc);
            bcc = List.copyOf(bcc);
        }

        private static MailRecipients toOnly(List<String> to) {
            return new MailRecipients(to, List.of(), List.of());
        }

        private boolean isEmpty() {
            return to.isEmpty() && cc.isEmpty() && bcc.isEmpty();
        }

        private int totalCount() {
            return to.size() + cc.size() + bcc.size();
        }
    }

    /** cid + InputStreamSource(MultipartFile 또는 ByteArrayResource) + contentType */
    private record InlineResource(String cid, InputStreamSource source, String contentType) {}
}
