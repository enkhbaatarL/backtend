package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.FileRef;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 서버가 고정한 시험 입력만 읽는다. 요청 중 생성/등록하거나 일반 Case를 자동 캐시하지 않는다. */
@Component
class InputTableSnapshotReader {
    static final int SCHEMA = 1;
    static final String EXTRACTOR = "input-workbook-sax-v1";
    static final long PART_LIMIT = 64L * 1024 * 1024;
    static final long TOTAL_LIMIT = 512L * 1024 * 1024;
    static final long ROW_LIMIT = 8L * 1024 * 1024;
    static final Set<String> TABLES = Set.of("TRADE/ShippingCostInput/Technical Parameters",
        "DEMAND/BCInput/RE Base Case GDP and Population Data");
    private static final JsonFactory FACTORY = JsonFactory.builder().streamReadConstraints(
        StreamReadConstraints.builder().maxNestingDepth(12).maxStringLength(1_048_576).maxNumberLength(10000).build()).build();
    private static final ObjectMapper METADATA = new ObjectMapper(FACTORY);
    @Value("${mi.model-input.table-snapshot.enabled:false}") private boolean enabled;
    /** 각 항목은 서버가 검증/동결한 manifest의 절대 경로#SHA-256. 빈 목록이 기본이다. */
    @Value("${mi.model-input.table-snapshot.fixed-inputs:}") private List<String> fixedInputs = List.of();

    record Identity(int company,String companyCode,String sourceKey,String sourceVersion,long templateId,
        int templateVersion,FileRef file,String tableKey,String definitionSha256) {}
    record Part(String name,long bytes,String sha256) {}
    record Manifest(int schema,String extractor,Identity identity,String sourceSha256,String sheet,List<Part> parts) {}
    record Selection(Path directory,Manifest manifest,String databaseId,long tableId) {
        Selection(Path directory,Manifest manifest) { this(directory,manifest,null,0); }
    }
    private com.sk.domain.shared.repository.master.tmm.ModelInputTableSnapshotRepository mysql;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    void mysql(com.sk.domain.shared.repository.master.tmm.ModelInputTableSnapshotRepository repository) { mysql=repository; }

    /** Decoder 소유 실패만 이 형식으로 전달한다. callback/권한/revision 오류는 감싸지 않는다. */
    static final class Unavailable extends RuntimeException {
        Unavailable() { super("Snapshot unavailable",null,false,false); }
    }

    static Identity identity(ModelInputDraftService.Actor actor,ModelInputDraftService.Source source,TableRef original,FileRef file) {
        try {
            return new Identity(actor.company(),actor.companyCode(),source.key(),source.version(),source.catalog().templateId(),
                source.catalog().version(),file,original.key(),sha256(METADATA.writeValueAsBytes(original)));
        } catch (IOException ex) { throw InputDraftException.internal(); }
    }

    Selection select(ModelInputDraftService.Actor actor,ModelInputDraftService.Source source,TableRef original,FileRef file) {
        if(!enabled||!TABLES.contains(original.key())||file.baseline()!=0)return null;
        Identity expected=identity(actor,source,original,file);
        if(mysql!=null) {
            if(source.caseId()==null)return null;
            try {
                var stored=mysql.find(actor.company(),source.caseId(),source.version(),original.id());
                if(stored==null||stored.manifest()==null)return null;
                byte[] bytes=stored.manifest().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                if(bytes.length>1_048_576||!sha256(bytes).equals(stored.hash()))return null;
                Manifest manifest=METADATA.readValue(bytes,Manifest.class);validate(manifest);
                if(!expected.equals(manifest.identity())||!Objects.equals(original.sheet(),manifest.sheet()))return null;
                if(file.sha256()!=null&&!file.sha256().equals(manifest.sourceSha256()))return null;
                return new Selection(null,manifest,stored.id(),original.id());
            } catch(IOException|org.springframework.dao.DataAccessException|Unavailable ex) { return null; }
        }

        // 이 명시적 pin은 고정 시험 객체의 내용/존재를 시험 기간 동안 유지한다는 서버 운영자의 선언이다.
        // 일반 fileNo/path만으로 불변성을 추정하는 자동 매핑은 없다.
        for(String fixed:fixedInputs) {
            try {
                int separator=fixed.lastIndexOf('#');
                if(separator<1)continue;
                Path path=Path.of(fixed.substring(0,separator));String digest=fixed.substring(separator+1);
                if(!path.isAbsolute()||!hash(digest)||Files.size(path)>1_048_576)continue;
                byte[] bytes;
                try(InputStream input=Files.newInputStream(path)){bytes=input.readNBytes(1_048_577);}
                if(bytes.length>1_048_576||!sha256(bytes).equals(digest))continue;
                Manifest manifest=METADATA.readValue(bytes,Manifest.class);
                validate(manifest);
                if(!expected.equals(manifest.identity())||!Objects.equals(original.sheet(),manifest.sheet()))continue;
                if(file.sha256()!=null&&!file.sha256().equals(manifest.sourceSha256()))continue;
                return new Selection(path.toRealPath().getParent(),manifest);
            } catch(IOException|java.nio.file.InvalidPathException|Unavailable ex) { /* 같은 원본 SAX로 진행한다. */ }
        }
        return null;
    }

    void read(Selection selection,List<InputWorkbookReader.Window> windows) {
        Manifest manifest=selection.manifest();validate(manifest);
        if(windows.stream().anyMatch(w->!manifest.sheet().equals(w.sheet())))throw new Unavailable();
        InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.TABLE_SNAPSHOT_READ,manifest.identity().file().role(),()->{
            int previous=0;int lastRow=windows.stream().mapToInt(InputWorkbookReader.Window::lastRow).max().orElse(Integer.MAX_VALUE);
            PartInputs inputs=new PartInputs(selection);
            for(int index=0;index<manifest.parts().size();index++) {
                Part part=manifest.parts().get(index);
                try {
                    MessageDigest digest=digest();
                    try(var input=new LimitedInput(inputs.open(index,part),part.bytes());
                        var hashed=new DigestInputStream(input,digest);var parser=FACTORY.createParser(hashed)) {
                        parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
                        require(parser.nextToken()==JsonToken.START_ARRAY);
                        while(parser.nextToken()!=JsonToken.END_ARRAY) {
                            InputTempFiles.checkpoint();
                            long begin=parser.currentTokenLocation().getByteOffset();
                            require(parser.currentToken()==JsonToken.START_OBJECT);
                            field(parser,"row");int row=integer(parser,1,1048576);require(row>previous);previous=row;
                            field(parser,"cells");require(parser.currentToken()==JsonToken.START_ARRAY);
                            Map<Integer,InputWorkbookReader.Cell> cells=new HashMap<>();
                            int columnBefore=0;
                            while(parser.nextToken()!=JsonToken.END_ARRAY) {
                                require(parser.currentToken()==JsonToken.START_OBJECT);
                                field(parser,"column");int column=integer(parser,1,16384);require(column>columnBefore);columnBefore=column;
                                field(parser,"raw");Object raw=scalar(parser);
                                field(parser,"display");Object display=scalar(parser);
                                field(parser,"formula");require(parser.currentToken()==JsonToken.VALUE_TRUE||parser.currentToken()==JsonToken.VALUE_FALSE);
                                boolean formula=parser.getBooleanValue();require(parser.nextToken()==JsonToken.END_OBJECT);
                                cells.put(column,new InputWorkbookReader.Cell(raw,display,formula));
                                require(parser.currentLocation().getByteOffset()-begin<=ROW_LIMIT);
                            }
                            field(parser,"invalidColumns");require(parser.currentToken()==JsonToken.START_ARRAY);
                            Set<Integer> invalid=new HashSet<>();
                            while(parser.nextToken()!=JsonToken.END_ARRAY)require(invalid.add(integer(parser,1,16384)));
                            require(parser.nextToken()==JsonToken.END_OBJECT);
                            require(parser.currentLocation().getByteOffset()-begin<=ROW_LIMIT);
                            // callback 밖 IOException만 아래에서 변환하므로 기존 렌더 오류를 fallback으로 삼지 않는다.
                            for(var window:windows)if(row<=lastRow&&window.rows().test(row))window.deliverRow(row,cells,invalid);
                        }
                        require(parser.nextToken()==null&&input.count==part.bytes());
                    }
                    require(HexFormat.of().formatHex(digest.digest()).equals(part.sha256()));
                } catch(IOException ex) {throw new Unavailable();}
            }
            return null;
        });
    }

    private final class PartInputs {
        private final Selection selection;
        private Map<Integer,byte[]> batch=Map.of();
        PartInputs(Selection selection) { this.selection=selection; }
        InputStream open(int ordinal,Part part)throws IOException {
            if(selection.databaseId()==null) {
                Path path=selection.directory().resolve(part.name());
                if(!path.toRealPath().getParent().equals(selection.directory().toRealPath())||Files.size(path)!=part.bytes())throw new IOException();
                return Files.newInputStream(path);
            }
            if(mysql==null||part.bytes()>com.sk.domain.shared.repository.master.tmm.ModelInputTableSnapshotRepository.PART_LIMIT
                    ||!part.name().equals(String.format(java.util.Locale.ROOT,"part-%05d.json",ordinal)))throw new IOException();
            if(!batch.containsKey(ordinal)) {
                try {
                    Map<Integer,byte[]> next=new HashMap<>();
                    for(var row:mysql.parts(selection.databaseId(),selection.tableId(),ordinal))next.put(row.ordinal(),row.bytes());
                    batch=next;
                } catch(org.springframework.dao.DataAccessException|IllegalStateException ex) { throw new IOException(); }
            }
            byte[] payload=batch.get(ordinal);
            if(payload==null||payload.length!=part.bytes())throw new IOException();
            return new java.io.ByteArrayInputStream(payload);
        }
    }

    private static void validate(Manifest manifest) {
        if(manifest==null||manifest.schema()!=SCHEMA||!EXTRACTOR.equals(manifest.extractor())||manifest.identity()==null
            ||manifest.identity().file()==null||!hash(manifest.sourceSha256())||manifest.sheet()==null
            ||manifest.parts()==null||manifest.parts().isEmpty()||manifest.parts().size()>1024)throw new Unavailable();
        long total=0;Set<String> names=new HashSet<>();
        for(Part part:manifest.parts()) {
            if(part==null||part.name()==null||!part.name().matches("part-[0-9]{5}\\.json")||!names.add(part.name())
                ||part.bytes()<2||part.bytes()>PART_LIMIT||!hash(part.sha256()))throw new Unavailable();
            total+=part.bytes();if(total>TOTAL_LIMIT)throw new Unavailable();
        }
    }
    private static void field(JsonParser parser,String name)throws IOException {
        require(parser.nextToken()==JsonToken.FIELD_NAME&&name.equals(parser.currentName()));parser.nextToken();
    }
    private static int integer(JsonParser parser,int min,int max)throws IOException {
        require(parser.currentToken()==JsonToken.VALUE_NUMBER_INT);int value=parser.getIntValue();require(value>=min&&value<=max);return value;
    }
    private static Object scalar(JsonParser parser)throws IOException {
        require(parser.currentToken()!=null);
        return switch(parser.currentToken()) {
            case VALUE_NULL -> null;
            case VALUE_STRING -> parser.getText();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NUMBER_INT,VALUE_NUMBER_FLOAT -> parser.getDecimalValue();
            default -> throw new IOException();
        };
    }
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static void require(boolean valid)throws IOException {if(!valid)throw new IOException();}
    static MessageDigest digest() {
        try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    static String sha256(byte[] bytes){return HexFormat.of().formatHex(digest().digest(bytes));}
    private static final class LimitedInput extends FilterInputStream {
        private final long maximum;private long count;
        LimitedInput(InputStream input,long maximum){super(input);this.maximum=maximum;}
        @Override public int read()throws IOException {int value=super.read();if(value!=-1&&++count>maximum)throw new IOException();return value;}
        @Override public int read(byte[] buffer,int offset,int length)throws IOException {
            int size=super.read(buffer,offset,(int)Math.min(length,maximum-count+1));
            if(size>0&&(count+=size)>maximum)throw new IOException();return size;
        }
    }
}
