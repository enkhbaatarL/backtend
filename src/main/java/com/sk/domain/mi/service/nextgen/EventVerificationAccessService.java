package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventVerificationAccessService {
    private final MiEventRepository eventRepository;

    // Controller의 회사/사용자 권한 검사 이후 실행. 이 트랜잭션은 Agent 호출 전에 끝난다.
    @Transactional(readOnly = true)
    public void checkAccessible(List<Long> eventIds) {
        if (!new HashSet<>(eventRepository.findPublishedIdsForVerification(eventIds)).equals(new HashSet<>(eventIds))) {
            throw AppHttpStatusException.of(HttpStatus.NOT_FOUND, ResultCode.Error.RSC_NOT_FOUND,
                "선택한 Event가 없거나 삭제·미게시 상태입니다. 목록을 새로고침해 주세요.");
        }
    }
}
