package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.properties.MailProperties;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
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
        sendMailWithRetry(recipients, subject, htmlBody, inlineResources);

        log.info("Newsletter original mail sent. newsletterId: {}, issueNo: {}, recipients: {}", newsletterId, issueNo, recipients.size());
    }

    /**
     * 에디션 메일 발송
     *
     * 정책:
     * - recipientEmails 입력 시 해당 수신자에게 발송
     * - recipientEmails 미입력 시 요청 사용자 이메일 1건으로 발송
     */
    @Transactional
    public void sendEditionMail(
            Integer newsletterId,
            Integer issueNo,
            String subject,
            String htmlBody,
            List<MultipartFile> images,
            List<String> recipientEmails,
            User actorUser) {

        validateSendRequest(subject, htmlBody);
//        validateEditionOwnershipAndState(newsletterId, issueNo, actorUser);

        List<String> recipients = resolveEditionRecipients(recipientEmails, actorUser);
        if (recipients.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_MISSING_PARAMETER,
                    "No recipients resolved. Provide recipientEmails or ensure actor email exists.");
        }

        List<InlineResource> inlineResources = validateAndBuildInlineResources(htmlBody, images);
        sendMailWithRetry(recipients, subject, htmlBody, inlineResources);

        log.info("Newsletter edition mail sent. newsletterId: {}, issueNo: {}, recipients: {}",
                newsletterId, issueNo, recipients.size());
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

    private void validateEditionOwnershipAndState(Integer newsletterId, Integer issueNo, User actorUser) {
        if (newsletterId == null || issueNo == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "newsletterId and issueNo are required");
        }
        if (actorUser == null || actorUser.getUserNo() == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "actor user is required");
        }

        String sql = """
                SELECT e.owner_user_id, e.edit_status
                  FROM newsletter_user_edition e
                  JOIN newsletter_issue_original o
                    ON o.id = e.original_issue_id
                 WHERE o.newsletter_master_id = :newsletterId
                   AND o.issue_no = :issueNo
                   AND e.del_yn = 'N'
                """;

        List<EditionStateRow> rows = namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("newsletterId", newsletterId)
                        .addValue("issueNo", issueNo),
                (rs, rowNum) -> new EditionStateRow(
                        rs.getObject("owner_user_id"),
                        rs.getString("edit_status"))
        );

        if (rows.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "edition not found. newsletterId: %d, issueNo: %d".formatted(newsletterId, issueNo));
        }

        EditionStateRow stateRow = rows.get(0);
        Integer ownerUserId = toInteger(stateRow.ownerUserId());
        if (ownerUserId == null || !ownerUserId.equals(actorUser.getUserNo())) {
            throw AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "No permission for this edition");
        }

        if (!EDIT_STATUS_SAVED.equalsIgnoreCase(StringUtils.defaultString(stateRow.editStatus()))) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Edition is not in EDIT_SAVED state");
        }
    }

    private List<String> resolveOriginalRecipients(Integer newsletterId, List<String> recipientEmails) {
        List<String> explicitRecipients = normalizeRecipients(recipientEmails);
        if (!explicitRecipients.isEmpty()) {
            return explicitRecipients;
        }

        String sql = """
                SELECT DISTINCT u.usr_logn_id AS email
                  FROM newsletter_subscription ns
                  JOIN tmd_usrm u
                    ON u.usr_no = ns.user_id
                 WHERE ns.newsletter_master_id = :newsletterId
                   AND ns.is_subscribed = TRUE
                   AND u.del_yn = 'N'
                   AND u.usr_logn_id IS NOT NULL
                """;

        List<String> subscribers = namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("newsletterId", newsletterId),
                (rs, rowNum) -> rs.getString("email")
        );

        return normalizeRecipients(subscribers);
    }

    private List<String> resolveEditionRecipients(List<String> recipientEmails, User actorUser) {
        List<String> explicitRecipients = normalizeRecipients(recipientEmails);
        if (!explicitRecipients.isEmpty()) {
            return explicitRecipients;
        }

        String actorEmail = actorUser.getUserLoginId();
        if (StringUtils.isBlank(actorEmail)) {
            return List.of();
        }

        return List.of(actorEmail.trim());
    }

    private List<String> normalizeRecipients(List<String> recipientEmails) {
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
                            "Invalid recipient email: " + trimmed);
                }
                normalized.add(trimmed);
            }
        }

        return new ArrayList<>(normalized);
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
            resources.add(new InlineResource(cid, file, contentType));
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

    private void sendMailWithRetry(List<String> recipients, String subject, String htmlBody, List<InlineResource> inlineResources) {
        for (String recipient : recipients) {
            sendSingleMailWithRetry(recipient, subject, htmlBody, inlineResources);
        }
    }

    private void sendSingleMailWithRetry(String recipient, String subject, String htmlBody, List<InlineResource> inlineResources) {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(mailProperties.getFrom());
                helper.setTo(recipient);
                helper.setSubject(subject);

                // IMPORTANT: setText를 먼저 호출한 뒤 inline 리소스를 추가해야 메일 클라이언트 호환성이 높다.
                helper.setText(htmlBody, true);

                for (InlineResource inlineResource : inlineResources) {
                    helper.addInline(inlineResource.cid(), inlineResource.file(), inlineResource.contentType());
                }

                javaMailSender.send(message);
                return;
            } catch (Exception e) {
                if (attempt == MAX_RETRY_COUNT) {
                    throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR,
                            "Mail sending failed after %d attempts: %s".formatted(MAX_RETRY_COUNT, e.getMessage()));
                }
                waitBeforeRetry(attempt, recipient, e);
            }
        }
    }

    private void waitBeforeRetry(int attempt, String recipient, Exception e) {
        log.warn("Mail sending failed on attempt {} for recipient {}. Retrying after {}ms...",
                attempt, recipient, RETRY_DELAY_MILLIS, e);
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

    private record EditionStateRow(Object ownerUserId, String editStatus) {
    }

    private record InlineResource(String cid, MultipartFile file, String contentType) {
    }
}
