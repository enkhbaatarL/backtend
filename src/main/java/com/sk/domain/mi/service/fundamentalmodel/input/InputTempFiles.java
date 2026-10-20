package com.sk.domain.mi.service.fundamentalmodel.input;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 소유한 작업 디렉터리만 정리한다. 다른 살아 있는 JVM의 lock은 건드리지 않는다. */
@Component
public class InputTempFiles {
    static void checkpoint() {
        if (Thread.currentThread().isInterrupted()) throw new InputDraftPreserveChange.Failure("TIMEOUT");
    }
    private final ThreadLocal<Scope> taskScope = new ThreadLocal<>();
    private final java.util.Set<Scope> activeScopes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 처리 본문/finally가 끝난 호출 스레드만 닫는다. 웹 요청의 다른 파일은 포함하지 않는다. */
    final class Scope implements AutoCloseable {
        private final Path directory;
        private Scope(Path directory) { this.directory = directory; }
        Path directory() { return directory; }
        @Override public void close() throws IOException {
            if (taskScope.get() != this) throw new IllegalStateException("temporary scope owner mismatch");
            try {
                deleteContents(directory);
                Files.delete(directory);
                activeScopes.remove(this);
            } finally { taskScope.remove(); }
        }
    }

    Scope beginTask() throws IOException {
        if (taskScope.get() != null) throw new IllegalStateException("temporary scope already active");
        Path dir = workspace.resolve("task-" + UUID.randomUUID());
        Scope scope = new Scope(posix ? Files.createDirectory(dir,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))) : Files.createDirectory(dir));
        activeScopes.add(scope);
        taskScope.set(scope);
        return scope;
    }
    private final java.util.Set<String> stoppedUploads=java.util.concurrent.ConcurrentHashMap.newKeySet();
    Path registerUpload(String id)throws IOException {
        if(!id.matches("[0-9a-f-]{36}"))throw InputDraftException.internal();
        Path marker=workspace.resolve(".upload-"+id+".marker");
        try(FileChannel record=posix?FileChannel.open(marker,java.util.Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))):FileChannel.open(marker,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){record.force(true);}
        // 비POSIX(Windows)는 디렉터리를 FileChannel로 열 수 없어 디렉터리 fsync를 건너뛴다.
        if(posix)try(FileChannel directory=FileChannel.open(workspace,StandardOpenOption.READ)){directory.force(true);}
        return marker;
    }
    java.util.Set<String> stoppedUploads(){return java.util.Set.copyOf(stoppedUploads);}
    void acknowledgedUpload(String id)throws IOException {Files.deleteIfExists(workspace.resolve(".upload-"+id+".marker"));stoppedUploads.remove(id);}
    private static final String PREFIX="mi-input-read-";
    private final Path root;
    private final Path workspace;
    private final boolean posix;
    private final FileChannel channel;
    private final FileLock lock;
    public InputTempFiles() { this(Path.of(System.getProperty("java.io.tmpdir"))); }
    InputTempFiles(Path root) {
        try {
            this.root=root;
            this.posix=root.getFileSystem().supportedFileAttributeViews().contains("posix");
            Path dir=root.resolve(PREFIX+UUID.randomUUID());
            workspace=posix?Files.createDirectory(dir,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))):Files.createDirectory(dir);
            channel=FileChannel.open(workspace.resolve(".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            lock=channel.lock();
        } catch(IOException ex) { throw InputDraftException.internal(); }
    }
    public Path create() throws IOException {
        Path directory=taskScope.get()==null?workspace:taskScope.get().directory;
        return posix?Files.createTempFile(directory,"source-",".xlsm",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))):Files.createTempFile(directory,"source-",".xlsm");
    }
    boolean plaintextRemains()throws IOException {
        try(var files=Files.list(workspace)){return files.anyMatch(path->!path.getFileName().toString().equals(".lock")
            &&!path.getFileName().toString().matches("\\.upload-[0-9a-f-]{36}\\.marker"));}
    }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        try(var dirs=Files.list(root)) {
            for(Path dir:dirs.filter(p->p.getFileName().toString().matches(PREFIX+"[0-9a-f-]{36}")).toList()) {
                if(dir.equals(workspace)||!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(dir)) continue;
                Path marker=dir.resolve(".lock"); if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)) continue;
                try(FileChannel old=FileChannel.open(marker,StandardOpenOption.WRITE);FileLock acquired=old.tryLock()) {
                    if(acquired!=null) {
                        try(var records=Files.list(dir)) {
                            for(Path record:records.filter(p->p.getFileName().toString().matches("\\.upload-[0-9a-f-]{36}\\.marker")).toList()) {
                                if(!Files.isRegularFile(record,LinkOption.NOFOLLOW_LINKS)||Files.size(record)!=0)throw new IOException("invalid intake marker");
                                String name=record.getFileName().toString();
                                Path durable=workspace.resolve(name);
                                if(!Files.exists(durable))Files.copy(record,durable);
                                try(FileChannel copied=FileChannel.open(durable,StandardOpenOption.WRITE)){copied.force(true);}
                                stoppedUploads.add(name.substring(8,name.length()-7));
                            }
                        }
                        if(posix)try(FileChannel directory=FileChannel.open(workspace,StandardOpenOption.READ)){directory.force(true);}
                        deleteContents(dir);
                    }
                } catch(OverlappingFileLockException ignored) { /* 같은 JVM의 다른 활성 workspace */ }
                catch(IOException ex) { throw InputDraftException.internal(); }
                if(Files.isDirectory(dir)&&!Files.exists(marker)) Files.delete(dir);
            }
        } catch(IOException ex) { throw InputDraftException.internal(); }
    }
    private static void deleteContents(Path directory)throws IOException {
        try(var paths=Files.walk(directory)) {
            for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) if(!path.equals(directory)) Files.delete(path);
        }
    }
    @PreDestroy
    public void close() {
        // Spring shutdown 제한시간 뒤에도 작업이 남으면 잠금/평문을 보존한다. 종료 증거를 만들지 않는다.
        if (!activeScopes.isEmpty()) return;
        try {
            boolean markers;
            try(var files=Files.list(workspace)){markers=files.anyMatch(p->p.getFileName().toString().matches("\\.upload-[0-9a-f-]{36}\\.marker"));}
            if(markers) {
                try(var files=Files.list(workspace)){for(Path p:files.toList())if(!p.getFileName().toString().equals(".lock")&&!p.getFileName().toString().matches("\\.upload-[0-9a-f-]{36}\\.marker"))Files.deleteIfExists(p);}
                lock.release();channel.close(); // 값 없는 종료 증거만 남긴다. 다음 시작에서 잠금 해제를 확인한다.
            } else {deleteContents(workspace);lock.release();channel.close();Files.deleteIfExists(workspace);}
        }
        catch(IOException ex) { throw InputDraftException.internal(); }
    }
}
