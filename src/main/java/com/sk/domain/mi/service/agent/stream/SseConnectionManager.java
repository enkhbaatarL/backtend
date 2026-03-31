package com.sk.domain.mi.service.agent.stream;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

/**
 * SSE 스트림 연결을 관리하는 서비스
 * <p>
 * 동시성 제어: - ConcurrentHashMap으로 스레드 안전성 보장 - 같은 chatRoomId에 대한 중복 스트림 방지 - 취소 요청 중복 처리 방지
 */
@Slf4j
@Service
public class SseConnectionManager {

    private final Map<UUID, StreamContext> activeStreams = new ConcurrentHashMap<>();

    /**
     * 새로운 스트림 작업을 등록하고 이전 작업을 중단합니다.
     * <p>
     * 동시성 처리: - 같은 chatRoomId에 대한 이전 스트림이 있으면 자동 중단 - ConcurrentHashMap의 원자적 연산으로 경쟁 상태 방지
     *
     * @param chatRoomId 채팅룸 ID
     * @param qnaNo      질의응답 번호
     * @param disposable Flux.subscribe()가 반환하는 Disposable 객체
     */
    public void registerActiveStream(UUID chatRoomId, Integer qnaNo, Disposable disposable) {
        if (chatRoomId == null || qnaNo == null || disposable == null) {
            log.error("Invalid parameters for stream registration - chatRoomId: {}, qnaNo: {}",
                chatRoomId, qnaNo);
            return;
        }

        StreamContext newContext = new StreamContext(qnaNo, disposable);

        StreamContext previous = activeStreams.put(chatRoomId, newContext);

        if (previous != null && !previous.getDisposable().isDisposed()) {
            log.warn(
                "Disposing previous stream for chatRoomId: {}, previous qnaNo: {}, new qnaNo: {}",
                chatRoomId, previous.getQnaNo(), qnaNo);
            previous.getDisposable().dispose();
        }

        log.info("Registered new stream - chatRoomId: {}, qnaNo: {}", chatRoomId, qnaNo);
    }

    /**
     * 특정 채팅룸의 활성 스트림 작업을 중단합니다.
     * <p>
     * 동시성 처리: - 이미 중단된 스트림에 대한 중복 중단 방지 - 취소 메시지 설정 후 dispose 호출
     *
     * @param chatRoomId 채팅룸 ID
     * @param message    취소 메시지 (선택적)
     * @return StreamContext (중단 성공 시), null (중단 실패 또는 스트림 없음)
     */
    public StreamContext cancelActiveStream(UUID chatRoomId, String message) {
        if (chatRoomId == null) {
            log.error("chatRoomId is null in cancelActiveStream");
            return null;
        }

        StreamContext context = activeStreams.get(chatRoomId);

        if (context == null) {
            log.warn("No active stream found for cancellation - chatRoomId: {}", chatRoomId);
            return null;
        }

        // 이미 중단된 스트림인지 확인
        if (context.getDisposable().isDisposed()) {
            log.warn("Stream already disposed - chatRoomId: {}, qnaNo: {}",
                chatRoomId, context.getQnaNo());
            return null;
        }

        // 취소 메시지 설정 (doFinally에서 사용)
        if (message != null) {
            context.setCancelMessage(message);
        }

        // 스트림 중단
        try {
            context.getDisposable().dispose();
            log.info("Successfully cancelled stream - chatRoomId: {}, qnaNo: {}, message: {}",
                chatRoomId, context.getQnaNo(), message);
            return context;
        } catch (Exception e) {
            log.error("Error while disposing stream - chatRoomId: {}, qnaNo: {}",
                chatRoomId, context.getQnaNo(), e);
            return null;
        }
    }

    /**
     * 특정 채팅룸의 StreamContext를 조회합니다.
     *
     * @param chatRoomId 채팅룸 ID
     * @return StreamContext (존재 시), null (미존재 시)
     */
    public StreamContext getStreamContext(UUID chatRoomId) {
        if (chatRoomId == null) {
            log.error("chatRoomId is null in getStreamContext");
            return null;
        }
        return activeStreams.get(chatRoomId);
    }

    /**
     * 스트림 완료/에러/취소 시 등록된 객체를 제거합니다.
     * <p>
     * doFinally에서 호출되어 리소스 정리
     *
     * @param chatRoomId 채팅룸 ID
     */
    public void removeActiveStream(UUID chatRoomId) {
        if (chatRoomId == null) {
            log.error("chatRoomId is null in removeActiveStream");
            return;
        }

        StreamContext removed = activeStreams.remove(chatRoomId);
        if (removed != null) {
            log.info("Removed stream context - chatRoomId: {}, qnaNo: {}",
                chatRoomId, removed.getQnaNo());
        }
    }

    /**
     * 현재 활성화된 모든 스트림의 개수를 반환합니다.
     *
     * @return 활성 스트림 개수
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }

    /**
     * 특정 채팅룸에 활성 스트림이 있는지 확인합니다.
     *
     * @param chatRoomId 채팅룸 ID
     * @return 활성 스트림 존재 여부
     */
    public boolean hasActiveStream(UUID chatRoomId) {
        if (chatRoomId == null) {
            return false;
        }

        StreamContext context = activeStreams.get(chatRoomId);
        return context != null && !context.getDisposable().isDisposed();
    }

    /**
     * 스트림 Context를 담는 클래스
     * <p>
     * - qnaNo: 질의응답 번호 (불변) - disposable: 스트림 제어 객체 (불변) - cancelMessage: 취소 메시지 (가변, thread-safe)
     */
    @Getter
    public static class StreamContext {

        private final Integer qnaNo;
        private final Disposable disposable;

        /**
         * volatile: 멀티스레드 환경에서 가시성 보장 - Controller 스레드에서 설정 - Reactor 이벤트 루프 스레드에서 읽기
         */
        @Setter
        private volatile String cancelMessage;

        public StreamContext(Integer qnaNo, Disposable disposable) {
            this.qnaNo = qnaNo;
            this.disposable = disposable;
        }

        /**
         * 스트림이 중단되었는지 확인합니다.
         *
         * @return 중단 여부
         */
        public boolean isDisposed() {
            return disposable.isDisposed();
        }
    }
}
