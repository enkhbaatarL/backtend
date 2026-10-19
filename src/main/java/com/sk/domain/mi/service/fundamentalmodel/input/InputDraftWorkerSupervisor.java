package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** container PID1. 자식 종료를 wait한 뒤에만 평문을 지우며 작업을 자동 재실행하지 않는다. */
public final class InputDraftWorkerSupervisor {
    public static void main(String[] args)throws Exception {
        if(ProcessHandle.current().pid()!=1)throw new IllegalStateException("dedicated container PID1 required");
        verifyBudget(Path.of("/sys/fs/cgroup"));
        Path root=privateDirectory(Path.of("/work")),plain=privateDirectory(Path.of("/scratch"));
        if(!Files.getFileStore(plain).type().equals("tmpfs")||Files.getFileStore(plain).getTotalSpace()!=512L*1024*1024)
            throw new IllegalStateException("worker requires private 512MiB scratch tmpfs");
        AtomicReference<Process> active=new AtomicReference<>();AtomicBoolean stopping=new AtomicBoolean();
        try(FileChannel channel=FileChannel.open(root.resolve(".supervisor.lock"),Set.of(StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            FileLock lock=channel.tryLock()) {
            if(lock==null)throw new IllegalStateException("worker volume already active");
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                stopping.set(true);
                synchronized(active) {
                    Process child=active.get();
                    try {if(child!=null)stop(child);cleanPlaintext(plain);}catch(Exception ex){System.err.println("input-draft-worker cleanup incomplete");}
                }
            },"input-draft-stop"));
            try {
            // 이 PID namespace의 이전 PID1/자식들은 새 container 시작 전에 종료됐다.
            cleanPlaintext(plain);
            while(!stopping.get()) {
                List<String> command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin/java").toString(),
                    "-Xmx768m","-XX:ActiveProcessorCount=2","-XX:-HeapDumpOnOutOfMemoryError","-XX:ErrorFile=/dev/null",
                    "-XX:-UsePerfData","-XX:+ExitOnOutOfMemoryError","-Djava.io.tmpdir=/scratch","-cp",System.getProperty("java.class.path"),InputDraftWorkerMain.class.getName()));
                command.addAll(List.of(args));
                ProcessBuilder builder=new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
                builder.environment().put("MI_INPUT_DRAFT_SUPERVISED","true");builder.environment().put("MI_INPUT_DRAFT_WORK_DIR",root.toString());
                Process child;
                synchronized(active) {
                    if(stopping.get())break;
                    child=builder.start();active.set(child);
                }
                int exit=child.waitFor();
                synchronized(active) {active.compareAndSet(child,null);cleanPlaintext(plain);}
                if(!stopping.get()) {
                    System.err.println("input-draft-worker child stopped; exit="+exit);
                    // DB/설정 오류는 container 재시작의 backoff에 맡긴다. 작업 자체를 재시도하지 않는다.
                    if(exit!=124)System.exit(exit==0?1:exit);
                    Thread.sleep(1000);
                }
            }
            } finally {
                stopping.set(true);
                synchronized(active) {Process child=active.get();if(child!=null)stop(child);cleanPlaintext(plain);}
            }
        }
    }
    static void verifyBudget(Path cgroup)throws Exception {
        String[] cpu=Files.readString(cgroup.resolve("cpu.max")).trim().split("\\s+");
        if(cpu.length!=2||"max".equals(cpu[0])||Long.parseLong(cpu[1])<=0||Long.parseLong(cpu[0])!=2*Long.parseLong(cpu[1])
            ||Long.parseLong(Files.readString(cgroup.resolve("memory.max")).trim())!=2147483648L
            ||Long.parseLong(Files.readString(cgroup.resolve("memory.swap.max")).trim())!=0)
            throw new IllegalStateException("worker requires whole cgroup 2vCPU/2GiB/swap0");
    }
    static Path privateDirectory(Path path)throws Exception {
        if(!Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(path))throw new IllegalStateException("worker directory");
        if(!Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rwx------")))throw new IllegalStateException("worker directory permissions");
        return path;
    }
    static void stop(Process child)throws Exception {
        List<ProcessHandle> descendants=child.descendants().toList();descendants.forEach(ProcessHandle::destroyForcibly);
        child.destroyForcibly();
        if(!child.waitFor(10,TimeUnit.SECONDS)||child.isAlive())throw new IllegalStateException("worker termination unconfirmed");
        for(ProcessHandle descendant:descendants)if(descendant.isAlive())descendant.onExit().get(10,TimeUnit.SECONDS);
    }
    static void cleanPlaintext(Path directory)throws Exception {
        try(var entries=Files.walk(directory)) {
            for(Path entry:entries.sorted(Comparator.reverseOrder()).toList())if(!entry.equals(directory))Files.delete(entry);
        }
        try(var entries=Files.list(directory)){if(entries.findAny().isPresent())throw new IllegalStateException("worker plaintext remains");}
    }
}
