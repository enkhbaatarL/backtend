package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import com.sk.domain.shared.repository.master.tmm.ModelInputTableSnapshotRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputTableSnapshotRepository.Job;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 신규 Case의 durable intent만 소비한다. HTTP/Case 잠금 transaction에서 파일을 읽지 않는다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="mi.model-input.table-snapshot.mysql.enabled",havingValue="true")
final class InputTableSnapshotMysqlBuilder implements SmartLifecycle {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final int TARGET_PART=1024*1024;
    private final ModelInputTableSnapshotRepository store;
    private final ModelInputDraftService service;
    private final InputWorkbookReader reader;
    private final InputTableSnapshotReader snapshots;
    private final PlatformTransactionManager transactionManager;
    private final InputTempFiles scratch;
    private volatile boolean running;
    private ScheduledExecutorService executor;

    @Override public synchronized void start() {
        if(running)return;
        running=true;
        executor=Executors.newSingleThreadScheduledExecutor(task->{Thread thread=new Thread(task,"input-table-snapshot");thread.setDaemon(true);return thread;});
        executor.scheduleWithFixedDelay(()->{
            try { tick(); }
            catch(RuntimeException failure) { log.warn("Table Snapshot consumer unavailable: type={}",failure.getClass().getSimpleName()); }
        },0,1000,TimeUnit.MILLISECONDS);
    }
    @Override public synchronized void stop() {running=false;if(executor!=null)executor.shutdownNow();}
    @Override public boolean isRunning() {return running;}

    boolean tick() {
        Job job=tx(store::claim);if(job==null)return false;
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(120);
        try(var scope=scratch.beginTask()) {
            build(job,()->{InputTempFiles.checkpoint();if(System.nanoTime()>=deadline)throw new IllegalStateException("snapshot deadline");});
            log.info("Table Snapshot complete: case={} company={}",job.caseId(),job.company());
        } catch(Exception failure) {
            tx(()->{store.failed(job);return null;});
            log.warn("Table Snapshot failed: case={} company={} type={}",job.caseId(),job.company(),failure.getClass().getSimpleName());
        }
        return true;
    }

    void build(Job job,Runnable checkpoint) {
        var actor=new ModelInputDraftService.Actor(job.company(),job.user(),job.companyCode(),job.actor());
        var source=service.snapshotSource(actor,job.caseId());
        var tables=source.catalog().tables().stream().filter(t->InputTableSnapshotReader.TABLES.contains(t.key())).toList();
        if(tables.isEmpty())throw new IllegalStateException("unsupported snapshot source");
        List<InputTableSnapshotReader.Identity> expected=new ArrayList<>();
        for(TableRef table:tables) {
            checkpoint.run();
            var file=source.files().stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow();
            var identity=InputTableSnapshotReader.identity(actor,source,table,file);expected.add(identity);
            tx(()->{store.beginTable(job,table.id());return null;});
            reader.withLocal(file,job.companyCode(),local->{
                generate(job,identity,table,local,checkpoint);return null;
            },checkpoint);
        }
        checkpoint.run();
        var current=service.snapshotSource(actor,job.caseId());
        List<InputTableSnapshotReader.Identity> actual=current.catalog().tables().stream()
            .filter(t->InputTableSnapshotReader.TABLES.contains(t.key()))
            .map(t->InputTableSnapshotReader.identity(actor,current,t,current.files().stream().filter(f->f.role().equals(t.role())).findFirst().orElseThrow())).toList();
        if(!expected.equals(actual))throw new IllegalStateException("snapshot source changed");
        checkpoint.run();tx(()->{store.ready(job,source.version());return null;});
    }

    private void generate(Job job,InputTableSnapshotReader.Identity identity,TableRef table,Path source,Runnable checkpoint) {
        try {
            MessageDigest digest=InputTableSnapshotReader.digest();
            try(var input=Files.newInputStream(source)) {
                byte[] buffer=new byte[64*1024];for(int size;(size=input.read(buffer))!=-1;){checkpoint.run();digest.update(buffer,0,size);}
            }
            String sourceHash=java.util.HexFormat.of().formatHex(digest.digest());
            if(identity.file().sha256()!=null&&!sourceHash.equals(identity.file().sha256()))throw new IOException("snapshot original hash");
            InputReadDefinition definition=InputReadDefinition.parse(new InputDraftJson(JSON).definition(table.definition()));
            Set<Integer> columns=new HashSet<>(definition.columns()),controls=new HashSet<>();
            for(var control:definition.controls()) {
                columns.add(control.path("binding").path("columnIndex").asInt()+1);
                controls.add(control.path("binding").path("rowNumber").asInt());
            }
            java.util.function.IntPredicate selected=row->definition.headerRows().contains(row)||definition.body(row)||controls.contains(row);
            int last=Math.max(table.endRow(),controls.stream().mapToInt(Integer::intValue).max().orElse(0));
            Writer writer=new Writer(job,table.id(),checkpoint);
            List<InputWorkbookReader.Window> windows=new ArrayList<>();
            windows.add(new InputWorkbookReader.Window(table.sheet(),selected,Set.copyOf(columns),writer::row,row->{},last));
            for(int column:new TreeSet<>(columns))windows.add(new InputWorkbookReader.Window(table.sheet(),selected,Set.of(column),
                (row,cells)->{},row->writer.invalid.add(column),last));
            reader.readLocal(source,windows);writer.finish();
            var manifest=new InputTableSnapshotReader.Manifest(InputTableSnapshotReader.SCHEMA,InputTableSnapshotReader.EXTRACTOR,
                identity,sourceHash,table.sheet(),List.copyOf(writer.parts));
            byte[] payload=JSON.writeValueAsBytes(manifest);
            tx(()->{store.manifest(job,table.id(),payload,InputTableSnapshotReader.sha256(payload));return null;});
            // Verify the persisted representation using the production decoder before publication.
            snapshots.read(new InputTableSnapshotReader.Selection(null,manifest,job.id(),table.id()),
                List.of(new InputWorkbookReader.Window(table.sheet(),selected,Set.copyOf(columns),(row,cells)->checkpoint.run(),row->{},last)));
        } catch(RuntimeException ex) {throw ex;}
        catch(Exception ex) {throw new IllegalStateException("snapshot generation failed",ex);}
    }

    private final class Writer {
        private final Job job;private final long table;private final Runnable checkpoint;
        private final List<InputTableSnapshotReader.Part> parts=new ArrayList<>();
        private final Set<Integer> invalid=new TreeSet<>();
        private ByteArrayOutputStream part=new ByteArrayOutputStream();
        private int partRows,previous,row;private long total;
        private Map<Integer,InputWorkbookReader.Cell> cells;
        Writer(Job job,long table,Runnable checkpoint){this.job=job;this.table=table;this.checkpoint=checkpoint;part.write('[');}
        void row(int next,Map<Integer,InputWorkbookReader.Cell> values) {
            checkpoint.run();flushRow();if(next<=previous)throw new IllegalStateException("snapshot row order");
            previous=next;row=next;cells=values;invalid.clear();
        }
        void flushRow() {
            if(cells==null)return;
            checkpoint.run();
            try {
                BoundedRow bytes=new BoundedRow();
                try(var out=JSON.getFactory().createGenerator(bytes)) {
                    out.writeStartObject();out.writeNumberField("row",row);out.writeArrayFieldStart("cells");
                    for(var cell:new TreeMap<>(cells).entrySet()) {
                        out.writeStartObject();out.writeNumberField("column",cell.getKey());
                        out.writeObjectField("raw",cell.getValue().raw());out.writeObjectField("display",cell.getValue().display());
                        out.writeBooleanField("formula",cell.getValue().formula());out.writeEndObject();
                    }
                    out.writeEndArray();out.writeObjectField("invalidColumns",invalid);out.writeEndObject();
                }
                byte[] value=bytes.toByteArray();
                if(partRows>0&&part.size()+value.length+2>TARGET_PART)flushPart();
                if(partRows>0)part.write(',');part.write(value);partRows++;cells=null;
            } catch(IOException ex) {throw new UncheckedIOException(ex);}
        }
        void flushPart() {
            checkpoint.run();part.write(']');byte[] bytes=part.toByteArray();
            total+=bytes.length;
            if(parts.size()>=1024||total>InputTableSnapshotReader.TOTAL_LIMIT||bytes.length>ModelInputTableSnapshotRepository.PART_LIMIT)
                throw new IllegalStateException("snapshot size limit");
            int ordinal=parts.size();tx(()->{store.part(job,table,ordinal,bytes);return null;});
            parts.add(new InputTableSnapshotReader.Part(String.format(Locale.ROOT,"part-%05d.json",ordinal),bytes.length,InputTableSnapshotReader.sha256(bytes)));
            part=new ByteArrayOutputStream();part.write('[');partRows=0;
        }
        void finish(){flushRow();if(partRows>0||parts.isEmpty())flushPart();}
    }

    private static final class BoundedRow extends ByteArrayOutputStream {
        private void bound(int size){if((long)count+size>InputTableSnapshotReader.ROW_LIMIT)throw new IllegalStateException("snapshot row limit");}
        @Override public synchronized void write(int value){bound(1);super.write(value);}
        @Override public synchronized void write(byte[] bytes,int offset,int length){bound(length);super.write(bytes,offset,length);}
    }
    private <T>T tx(Supplier<T> action) {
        TransactionTemplate transaction=new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);transaction.setTimeout(10);
        return transaction.execute(ignored->action.get());
    }
}
