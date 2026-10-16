package com.sk.domain.mi.service.nextgen;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.EventKeywordSettingReq;
import com.sk.domain.mi.dto.response.nextgen.EventKeywordSettingListRes;
import com.sk.domain.mi.dto.response.nextgen.EventKeywordSettingRes;
import com.sk.domain.shared.entity.master.tmd.EventKeywordSetting;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.EventKeywordSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Next Gen MI 이벤트 키워드 알림 설정
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventKeywordSettingServiceImpl implements EventKeywordSettingService {

    static final int MAX_KEYWORD_COUNT = 20;
    static final int MIN_KEYWORD_LENGTH = 2;
    static final int MAX_KEYWORD_LENGTH = 30;

    private final EventKeywordSettingRepository eventKeywordSettingRepository;

    /**
     * [키워드 목록]
     * - 로그인 사용자의 활성(미해지) 키워드만 등록순(PK 오름차순)으로 반환
     * - keywordCount / maxKeywordCount로 팝업의 "등록된 키워드 (n/20)"과 20개 도달 시 입력 비활성화를 판단한다
     */
    @Override
    public EventKeywordSettingListRes getKeywordSettings(User user) {
        List<EventKeywordSettingRes> keywords = eventKeywordSettingRepository
            .findAllByUser_UserNoAndIsDeletedFalseOrderByKeywordSettingIdAsc(user.getUserNo())
            .stream()
            .map(EventKeywordSettingRes::of)
            .toList();
        return EventKeywordSettingListRes.of(keywords, MAX_KEYWORD_COUNT);
    }

    /**
     * [키워드 등록]
     * - 처리 순서: 정규화 → 길이 검증(2~30자) → 개수 검증(활성 20개) → 중복/복구 판정 → 신규 저장
     * - 길이는 정규화 후 문자(code point) 수로 센다 — FE 카운터는 입력 원문 기준이라 앞뒤 공백만큼 다를 수 있다
     * - 중복 판정은 해지 row까지 포함해 조회한다. 활성이면 중복 오류, 해지면 restore(알림 ON, 새 표기로 갱신)
     */
    @Transactional
    @Override
    public EventKeywordSettingRes createKeywordSetting(EventKeywordSettingReq.Insert dto, User user) {
        String keywordName = normalizeKeyword(dto.keywordName());
        validateKeywordLength(keywordName);

        if (eventKeywordSettingRepository.countByUser_UserNoAndIsDeletedFalse(user.getUserNo()) >= MAX_KEYWORD_COUNT) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "키워드는 최대 %d개까지 등록할 수 있습니다.".formatted(MAX_KEYWORD_COUNT));
        }

        Optional<EventKeywordSetting> existing = eventKeywordSettingRepository
            .findByUser_UserNoAndKeywordName(user.getUserNo(), keywordName);

        if (existing.isPresent()) {
            EventKeywordSetting setting = existing.get();
            if (Boolean.FALSE.equals(setting.getIsDeleted())) {
                throw duplicateKeyword();
            }
            setting.restore(keywordName);
            return EventKeywordSettingRes.of(setting);
        }

        try {
            return EventKeywordSettingRes.of(eventKeywordSettingRepository.saveAndFlush(EventKeywordSetting.create(user, keywordName)));
        } catch (DataIntegrityViolationException e) {
            log.warn("키워드 중복 등록 충돌: usrNo={}", user.getUserNo());
            throw duplicateKeyword();
        }
    }

    /**
     * [키워드 알림 ON/OFF]
     * - 본인 소유의 활성 키워드만 변경 가능 (아니면 RSC_NOT_FOUND — 타인 키워드 존재 여부를 드러내지 않는다)
     * - OFF는 이후 새로 발생하는 이벤트의 매칭에서만 제외한다. 이미 생성된 알림/발송 내역은 건드리지 않는다
     */
    @Transactional
    @Override
    public EventKeywordSettingRes updateNotificationUse(Long keywordSettingId, EventKeywordSettingReq.UpdateNotificationUse dto, User user) {
        EventKeywordSetting setting = getOwnedKeywordSetting(keywordSettingId, user);
        setting.changeNotificationUse(dto.isNotificationUse());
        return EventKeywordSettingRes.of(setting);
    }

    /**
     * [키워드 삭제]
     * - soft delete(del_yn=Y). 알림 내역이 keywd_set_id를 FK로 참조하고 있어 물리 삭제하지 않는다
     * - 이미 발생한 알림 내역은 키워드 스냅샷(keywd_snpsht_nm)으로 남아 My Notifications에 계속 노출된다
     * - 같은 키워드를 다시 등록하면 createKeywordSetting에서 이 row를 복구한다
     */
    @Transactional
    @Override
    public EventKeywordSettingRes deleteKeywordSetting(Long keywordSettingId, User user) {
        EventKeywordSetting setting = getOwnedKeywordSetting(keywordSettingId, user);
        setting.softDelete();
        return EventKeywordSettingRes.of(setting);
    }

    /**
     * 키워드 정규화: NFC → 앞뒤 공백 제거 → 연속 공백(탭·개행 포함) 1칸
     * - NFC: macOS 복사 등으로 들어오는 분해형(NFD) 한글을 조합형으로 맞춰, 화면상 같은 키워드가 다른 값으로 저장되지 않게 한다
     * - 매칭 단계에서도 같은 정규화를 거친 값끼리 비교해야 하므로 package-private static으로 둔다
     */
    static String normalizeKeyword(String keywordName) {
        if (keywordName == null) {
            return "";
        }
        return Normalizer.normalize(keywordName, Normalizer.Form.NFC)
            .strip()
            .replaceAll("\\s+", " ");
    }

    /**
     * 정규화된 키워드의 길이 검증 (Figma: 2자 미만 / 30자 초과 시 입력 오류)
     */
    private void validateKeywordLength(String keywordName) {
        int length = keywordName.codePointCount(0, keywordName.length());
        if (length < MIN_KEYWORD_LENGTH) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "키워드는 %d자 이상 입력하세요.".formatted(MIN_KEYWORD_LENGTH));
        }
        if (length > MAX_KEYWORD_LENGTH) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "키워드는 %d자 이하로 입력하세요.".formatted(MAX_KEYWORD_LENGTH));
        }
    }

    /**
     * 로그인 사용자 소유의 활성 키워드 조회. 없음/해지/타인 소유를 구분하지 않고 RSC_NOT_FOUND로 응답한다
     */
    private EventKeywordSetting getOwnedKeywordSetting(Long keywordSettingId, User user) {
        return eventKeywordSettingRepository
            .findByKeywordSettingIdAndUser_UserNoAndIsDeletedFalse(keywordSettingId, user.getUserNo())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "EventKeywordSetting not found: keywordSettingId=%d".formatted(keywordSettingId)));
    }

    /**
     * 중복 키워드 오류
     */
    private AppErrorException duplicateKeyword() {
        return AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS, "이미 등록된 키워드입니다.");
    }

}
