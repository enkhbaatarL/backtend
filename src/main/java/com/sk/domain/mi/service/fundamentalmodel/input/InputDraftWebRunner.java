package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.Operation;
import java.time.Duration;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

/** 단일 처리 스레드와 독립 감시. interrupt는 요청일 뿐 종료 증거로 사용하지 않는다. */
@Slf4j
final class InputDraftWebRunner implements SmartLifecycle {
    private final InputDraftRunnerLease.Factory locks;
    private final InputDraftRunnerRepository state;
    private final InputDraftWebTasks tasks;
    private final InputTempFiles scratch;
    private final String instance = UUID.randomUUID().toString();
    private final String host = logValue(System.getenv("HOSTNAME"));
    private final long pid = ProcessHandle.current().pid();
    private final Object control = new Object();
    private volatile InputDraftRunnerLease lease;
    private volatile Thread worker;
    private volatile boolean running;
    private volatile boolean stopping;
    private volatile boolean lost;
    private volatile boolean blocked;
    private boolean started;
    private Active active;
    private volatile Runnable onStop = () -> {};
    private ScheduledExecutorService monitors;

    private record Failure(String reason, List<Object> issues) {}

    private static final class Active {
        final long deadline;
        String reason;
        Active(long deadline) { this.deadline = deadline; }
    }

    InputDraftWebRunner(InputDraftRunnerLease.Factory locks, InputDraftRunnerRepository state,
            InputDraftWebTasks tasks, InputTempFiles scratch) {
        this.locks = locks; this.state = state; this.tasks = tasks; this.scratch = scratch;
    }

    @Override public synchronized void start() {
        if (started) return;
        started = true;
        running = true;
        worker = new Thread(() -> run(null, null), "input-draft-web");
        worker.setDaemon(true);
        worker.start();
    }

    /** 운영자가 실제 이전 실행/전송 종료를 확인한 별도 headless 명령에서만 호출한다. */
    void recover(InputDraftRunnerRepository.State expected, String actor) {
        synchronized (this) {
            if (started) throw new IllegalStateException("runner already started");
            started = true; running = true; worker = Thread.currentThread();
        }
        run(expected, actor);
        if (blocked) throw new IllegalStateException("recovery incomplete; inspect persisted runner state before new approval");
    }

    private void run(InputDraftRunnerRepository.State recovery, String actor) {
        String execution = null;
        String waitingReason = null;
        monitors = Executors.newScheduledThreadPool(2, work -> {
            Thread thread = new Thread(work, "input-draft-web-watch"); thread.setDaemon(true); return thread;
        });
        // 잠금 확인 IO가 지연돼도 기한 interrupt는 별도 감시 스레드에서 실행한다.
        monitors.scheduleWithFixedDelay(this::checkLease, 50, 200, TimeUnit.MILLISECONDS);
        monitors.scheduleWithFixedDelay(this::checkDeadline, 10, 20, TimeUnit.MILLISECONDS);
        try {
            while (!stopping && execution == null) {
                lease = locks.acquire();
                if (lease == null && !"QUEUE_LOCK_HELD".equals(waitingReason)) {
                    logWaiting("QUEUE_LOCK_HELD");
                    waitingReason = "QUEUE_LOCK_HELD";
                }
                if (lease != null) {
                    requireLease();
                    execution = recovery == null ? state.activate() : state.approveRecovery(recovery, actor);
                    if (execution == null) {
                        if (!"PREVIOUS_CONSUMER_NOT_STOPPED".equals(waitingReason)) {
                            logWaiting("PREVIOUS_CONSUMER_NOT_STOPPED");
                            waitingReason = "PREVIOUS_CONSUMER_NOT_STOPPED";
                        }
                        releaseLease();
                    }
                }
                if (execution == null) {
                    if (recovery != null) throw new IllegalStateException("another runner holds the queue lock");
                    pause(500);
                }
            }
            if (execution == null) return;
            var observed = diagnostics();
            log.info("Model input consumer started: execution={} instance={} host={} pid={} database={} lockOwnerConnectionId={} operations={}",
                    execution, instance, host, pid, logValue(observed.database()), ownerConnection(observed), tasks.enabledOperations());
            recoverStopped(execution);
            if (recovery == null) {
                while (!stopping) {
                    requireOwner(execution);
                    while (!tasks.cleanupUploads()) { requireOwner(execution); pause(250); }
                    if (stopping) break;
                    Operation next = tasks.next();
                    if (next == null) { pause(500); continue; }
                    requireOwner(execution);
                    if (stopping) break;
                    String token = UUID.randomUUID().toString();
                    state.bind(execution, next.id(), token);
                    // bind 응답 유실/잠금 상실이면 본문을 시작하지 않고 영속 상태를 남긴다.
                    requireOwner(execution);
                    Failure failure = process(next, token);
                    settle(execution, next.id(), token, failure);
                }
            }
            requireOwner(execution);
            state.stopped(execution);
            log.info("Model input consumer stopped: execution={} instance={} host={} pid={}", execution, instance, host, pid);
        } catch (Throwable failure) {
            blocked = true;
            log.error("Model input runner held; actual termination and persisted state must be checked before recovery: execution={} instance={} host={} pid={} failureType={}",
                    execution, instance, host, pid, failure.getClass().getSimpleName());
        } finally {
            monitors.shutdownNow();
            try { releaseLease(); }
            catch (Exception failure) { blocked = true; log.error("Model input queue lock release unconfirmed"); }
            running = false;
            onStop.run();
        }
    }

    private void logWaiting(String reason) {
        var observed = diagnostics();
        var event = reason.equals("QUEUE_LOCK_HELD") ? log.atInfo() : log.atWarn();
        event.log("Model input runner waiting: reason={} instance={} host={} pid={} database={} lockOwnerConnectionId={} persistedExecution={} persistedStatus={}",
                reason, instance, host, pid, logValue(observed.database()), ownerConnection(observed),
                logValue(observed.persistedExecution()), logValue(observed.persistedStatus()));
    }

    private InputDraftRunnerRepository.Diagnostics diagnostics() {
        try { return state.diagnostics(); }
        catch (RuntimeException failure) {
            // 진단 실패의 메시지/SQL/자격값은 기록하지 않고 원래 소비 흐름을 계속한다.
            log.warn("Model input runner diagnostics unavailable: instance={} host={} pid={} failureType={}",
                    instance, host, pid, failure.getClass().getSimpleName());
            return new InputDraftRunnerRepository.Diagnostics(null, null, null, null);
        }
    }

    private static Object ownerConnection(InputDraftRunnerRepository.Diagnostics observed) {
        return observed.lockOwnerConnectionId() == null ? "unknown" : observed.lockOwnerConnectionId();
    }

    private static String logValue(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String bounded = value.substring(0, Math.min(value.length(), 128));
        return bounded.replaceAll("[^a-zA-Z0-9_.:-]", "_");
    }

    private void recoverStopped(String execution) throws Exception {
        requireOwner(execution);
        var current = state.read();
        if (current.operation() != null) settle(execution, current.operation(), current.token(), "PERSISTENCE_FAILED");
        while (true) {
            requireOwner(execution);
            var previous = tasks.stoppedOperations();
            if (previous.isEmpty()) break;
            for (Operation operation : previous) {
                requireOwner(execution);
                state.bind(execution, operation.id(), operation.token());
                settle(execution, operation.id(), operation.token(), "PERSISTENCE_FAILED");
            }
        }
        while (!tasks.cleanupUploads()) { requireOwner(execution); pause(250); }
    }

    private Failure process(Operation operation, String token) throws Exception {
        long before = System.nanoTime();
        long remaining = Duration.between(tasks.databaseTime(), operation.expires()).toNanos();
        Active work = new Active(before + Math.max(0, remaining));
        synchronized (control) { active = work; }
        String reason = "PERSISTENCE_FAILED";
        List<Object> issues = List.of();
        boolean cleaned = false;
        try {
            var scope = scratch.beginTask();
            try {
                if (stopping) throw new IllegalStateException("runner stopping");
                tasks.process(operation, token);
            } catch (Throwable failure) {
                if (failure instanceof InputDraftPreserveChange.Failure specific) reason = InputDraftWebTasks.safeReason(specific.reason);
                else if (failure instanceof InputDraftException specific) {
                    reason = InputDraftWebTasks.safeReason(specific.getReason());
                    issues = specific.getIssues();
                }
                // Error/OOME도 이 작업 경계에서 실패 확정·정리를 시도한다. JVM 종료는 호출하지 않는다.
            } finally {
                scope.close();
                cleaned = true;
            }
        } finally {
            synchronized (control) {
                active = null;
                if (work.reason != null) { reason = work.reason; issues = List.of(); }
                Thread.interrupted(); // 종료 확인 후 DB 복구를 위한 스레드 interrupt만 해제한다.
            }
            if (!cleaned) throw new IllegalStateException("task plaintext cleanup unconfirmed");
        }
        // 여기까지 돌아온 경우에만 본문·동기 전송·finally 종료 증거가 있다.
        return new Failure(reason, issues);
    }

    private void settle(String execution, String operation, String token, String reason) throws Exception {
        settle(execution, operation, token, new Failure(reason, List.of()));
    }

    private void settle(String execution, String operation, String token, Failure failure) throws Exception {
        while (true) {
            requireOwner(execution);
            if (tasks.settle(operation, token, failure.reason(), failure.issues())) break;
            pause(250);
        }
        requireOwner(execution);
        state.releaseWork(execution, operation, token);
    }

    private void requireOwner(String execution) throws Exception { requireLease(); state.requireOwner(execution); }

    private void requireLease() throws Exception {
        if (lost || lease == null || !lease.held()) throw new IllegalStateException("queue lock not held");
    }

    private void checkLease() {
        InputDraftRunnerLease observed = lease;
        if (observed == null) return;
        try { if (observed.held()) return; }
        catch (Throwable failure) { /* 소유 확인 불가도 잠금 상실로 처리한다. */ }
        synchronized (control) {
            if (lease != observed) return;
            lost = true; stopping = true;
            worker.interrupt();
        }
    }

    private void checkDeadline() {
        synchronized (control) {
            if (active == null || active.reason != null || System.nanoTime() - active.deadline < 0) return;
            active.reason = "TIMEOUT";
            worker.interrupt();
            log.warn("Model input deadline reached; waiting for actual task termination");
        }
    }

    private void releaseLease() throws Exception {
        InputDraftRunnerLease previous;
        synchronized (control) { previous = lease; lease = null; }
        if (previous != null) previous.close();
    }

    private static void pause(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException ignored) { /* stopping/lost는 별도 상태로 유지한다. */ }
    }

    @Override public void stop(Runnable callback) {
        synchronized (control) {
            onStop = callback; stopping = true;
            if (!running) { callback.run(); return; }
            worker.interrupt();
        }
    }

    @Override public void stop() {
        stop(() -> {});
        Thread current = worker;
        if (current != null && current != Thread.currentThread()) {
            try { current.join(30000); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }
    }

    boolean blocked() { return blocked; }
    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
}
