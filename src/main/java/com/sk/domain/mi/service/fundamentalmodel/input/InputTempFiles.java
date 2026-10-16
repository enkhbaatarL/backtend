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
    private static final String PREFIX="mi-input-read-";
    private final Path root;
    private final Path workspace;
    private final FileChannel channel;
    private final FileLock lock;
    public InputTempFiles() { this(Path.of(System.getProperty("java.io.tmpdir"))); }
    InputTempFiles(Path root) {
        try {
            this.root=root;
            workspace=Files.createDirectory(root.resolve(PREFIX+UUID.randomUUID()),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            channel=FileChannel.open(workspace.resolve(".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            lock=channel.lock();
        } catch(IOException ex) { throw InputDraftException.internal(); }
    }
    public Path create() throws IOException { return Files.createTempFile(workspace,"source-",".xlsm",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))); }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        try(var dirs=Files.list(root)) {
            for(Path dir:dirs.filter(p->p.getFileName().toString().matches(PREFIX+"[0-9a-f-]{36}")).toList()) {
                if(dir.equals(workspace)||!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(dir)) continue;
                Path marker=dir.resolve(".lock"); if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS)) continue;
                try(FileChannel old=FileChannel.open(marker,StandardOpenOption.WRITE);FileLock acquired=old.tryLock()) {
                    if(acquired!=null) deleteContents(dir);
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
        try { deleteContents(workspace);lock.release();channel.close();Files.deleteIfExists(workspace); }
        catch(IOException ex) { throw InputDraftException.internal(); }
    }
}
