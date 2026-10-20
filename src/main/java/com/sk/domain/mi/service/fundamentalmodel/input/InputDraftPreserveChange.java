package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.XSSFReader;

/** 승인 F-O-01만 처리한다. 다른 입력의 계산 생략/기본값 정책으로 재사용하지 않는다. */
final class InputDraftPreserveChange {
    static final String TABLE="OUTPUT/USCostofSupply/us_cost_of_supply";
    static final String FIELD="breakevenPrice";
    static final String SOURCE_SHA="69742cc9c6fba170324c09c21986574a390c741ba26d371acdce570aeaac28ce";
    private static final BigDecimal VALUE=new BigDecimal("0.35");
    private static final Pattern CELL=Pattern.compile("(<c\\b(?=[^>]*\\br=\"B8\")[^>]*>)(.*?)(</c>)",Pattern.DOTALL);

    static boolean supports(TableRef table,InputReadDefinition definition) {
        if(!TABLE.equals(table.key())||definition.fields().size()!=1
            ||!definition.columns().equals(List.of(2))||!definition.identityColumns().equals(List.of(2))
            ||!definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(8,8)))
            ||!definition.controls().isEmpty()||!definition.displayUpdateRules().isEmpty()
            ||!table.scopes().equals(List.of(new Scope(8,8,2,2))))return false;
        JsonNode field=definition.fields().get(0),bounds=field.path("setConstraints");
        return FIELD.equals(field.path("fieldKey").asText())&&field.path("columnNumber").asInt()==2
            &&"DECIMAL".equals(field.path("dataType").asText())
            &&"READY".equals(definition.capabilities().path("SET").asText())
            &&"READY".equals(field.path("policyStatus").path("SET").asText())
            &&bounds.size()==2&&number(bounds.path("minimum"))&&number(bounds.path("maximum"));
    }

    static boolean supports(ModelInputDraftService.Source source,TableRef table,InputReadDefinition definition) {
        return supports(table,definition)&&(source.caseId()!=null||source.files().stream()
            .anyMatch(file->file.role().equals(table.role())&&SOURCE_SHA.equals(file.sha256())));
    }

    static void validate(ModelInputDraftService.Source source,InputDraftJson.ChangeRequest request,
            Draft draft,List<RowPosition> positions) {
        if(request.operations().size()!=1)throw unsupported(draft);
        JsonNode command=request.operations().get(0);
        TableRef table=source.catalog().tables().stream().filter(t->TABLE.equals(t.key())).findFirst().orElseThrow(()->unsupported(draft));
        if(!supports(source,table,source.definitions().get(table.id()))||!TABLE.equals(command.path("tableId").asText())
            ||!"SET".equals(command.path("type").asText())
            ||!command.path("rowRef").has("rowId"))throw unsupported(draft);
        if(!FIELD.equals(command.path("fieldKey").asText()))throw InputDraftException.invalid();
        String rowId=command.path("rowRef").path("rowId").asText();
        if(positions.stream().noneMatch(p->p.table()==table.id()&&p.id().equals(rowId)&&p.end()==null&&Integer.valueOf(8).equals(p.row())))throw InputDraftException.missing();
        if(!number(command.path("value")))throw InputDraftException.validation(draft.revision(),List.of(Map.of(
            "commandId",command.path("commandId").asText(),"tableId",TABLE,"path","/operations/0/value","reason","VALUE_NOT_ALLOWED")));
    }

    static void validateHistory(List<Revision> revisions,InputDraftJson json) {
        for(Revision revision:revisions) {
            if(revision.schema()!=2||(revision.number()>0&&revision.operationSchema()!=2))throw InputDraftException.internal();
            if(revision.number()==0||"UPLOAD_FILE".equals(revision.type()))continue;
            JsonNode delta=json.delta(revision.delta());
            if(delta.path("commands").size()!=1||!delta.path("tables").isEmpty()||!delta.path("columns").isArray()||!delta.path("columns").isEmpty()||!delta.path("rows").isEmpty()||delta.path("cells").size()!=1)
                throw InputDraftException.conflict("POLICY_UNSUPPORTED",revision.number());
            JsonNode command=delta.path("commands").get(0),cell=delta.path("cells").get(0);
            if(!TABLE.equals(command.path("tableId").asText())||!"SET".equals(command.path("type").asText())
                ||!FIELD.equals(command.path("fieldKey").asText())||!number(command.path("value"))
                ||!TABLE.equals(cell.path("tableId").asText())||cell.path("columnNumber").asInt()!=2
                ||!cell.path("rowId").equals(command.path("rowRef").path("rowId"))||!number(cell.path("raw"))||cell.path("formula").asBoolean())
                throw InputDraftException.conflict("POLICY_UNSUPPORTED",revision.number());
        }
    }

    static JsonNode execute(ModelInputDraftService.PreparedChange job,InputWorkbookReader reader,InputTempFiles scratch,InputDraftJson json) {
        return reader.withLocal(job.file(),job.company(),source->{
            Path saved=null;
            try {
                saved=scratch.create();
                InputWorkbookReader.Cell actual=generate(source,saved,job.previous(),reader,json);
                ObjectNode delta=(ObjectNode)json.tree(Map.of("policyTemplateId",job.templateId(),"commands",job.request().operations(),"tables",List.of(),"rows",List.of(),"columns",List.of(),"cells",List.of()));
                ObjectNode cell=delta.withArray("cells").addObject();
                cell.put("tableId",TABLE);cell.put("rowId",job.rowId());cell.putNull("rowNumber");cell.put("columnNumber",2);
                cell.set("raw",json.tree(actual.raw()));cell.set("display",json.tree(actual.display()));cell.put("formula",false);
                return delta;
            } catch(Failure ex){throw ex;}
            catch(Exception ex){throw new Failure("PRESERVATION_FAILED");}
            finally {if(saved!=null)try{Files.deleteIfExists(saved);}catch(Exception ex){throw new Failure("PERSISTENCE_FAILED");}}
        });
    }

    /** 최종저장도 같은 승인 변환/저장 재읽기·전체 보존 검증을 사용한다. */
    static void replay(InputDraftFinalizationService.Job job,com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.FileRef role,
            Path original,Path target,InputWorkbookReader reader,InputDraftJson json)throws Exception {
        validateHistory(job.revisions(),json);
        if(!role.role().equals("OUTPUT")) {
            Files.copy(original,target,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if(!InputDraftFinalizationFiles.digest(original).equals(InputDraftFinalizationFiles.digest(target)))throw new Failure("PRESERVATION_FAILED");
            return;
        }
        BigDecimal previous=null;
        for(Revision revision:job.revisions())if(revision.number()>role.baseline()&&"CHANGE".equals(revision.type())) {
            generate(original,target,previous,reader,json);previous=VALUE;
        }
        if(previous==null){Files.copy(original,target,java.nio.file.StandardCopyOption.REPLACE_EXISTING);if(!InputDraftFinalizationFiles.digest(original).equals(InputDraftFinalizationFiles.digest(target)))throw new Failure("PRESERVATION_FAILED");}
    }

    private static InputWorkbookReader.Cell generate(Path source,Path saved,BigDecimal previous,
            InputWorkbookReader reader,InputDraftJson json)throws Exception {
        try(InputStream input=Files.newInputStream(source)) {
            if(!SOURCE_SHA.equals(digest(input)))throw new Failure("SOURCE_CHANGED");
        }
        String sheet=sheetPart(source);
        InputWorkbookReader.Cell original=readCell(reader,source);
        if(original.formula()||!(original.raw() instanceof BigDecimal raw)||raw.compareTo(new BigDecimal("0.34"))!=0)
            throw new Failure("PRESERVATION_FAILED");
        if(previous!=null&&!number(json.tree(previous)))throw new Failure("PRESERVATION_FAILED");
        byte[] changed;
        try(ZipFile zip=new ZipFile(source.toFile())) {
            validateArchive(zip);
            ZipEntry entry=zip.getEntry(sheet);
            if(entry==null||entry.getSize()>64L*1024*1024)throw new Failure("STRUCTURE_VALIDATION_FAILED");
            byte[] baseline;
            try(InputStream input=zip.getInputStream(entry)){baseline=input.readAllBytes();}
            // 고정 원본 + 직전 성공값을 구성한 뒤 이번 명령을 적용한다.
            byte[] current=replace(baseline,previous==null?new BigDecimal("0.34"):previous);
            changed=replace(current,VALUE);
            try(ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(saved))) {
                var entries=zip.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry before=entries.nextElement(),after=new ZipEntry(before);
                    if(after.getMethod()==ZipEntry.DEFLATED)after.setCompressedSize(-1);
                    if(before.getName().equals(sheet)) {
                        CRC32 crc=new CRC32();crc.update(changed);after.setSize(changed.length);after.setCrc(crc.getValue());
                    }
                    output.putNextEntry(after);
                    if(before.getName().equals(sheet))output.write(changed);
                    else try(InputStream input=zip.getInputStream(before)){input.transferTo(output);}
                    output.closeEntry();
                }
            }
        }
        verify(source,saved,sheet,changed);
        InputWorkbookReader.Cell actual=readCell(reader,saved);
        if(actual.formula()||!(actual.raw() instanceof BigDecimal)||!number(json.tree(actual.raw())))throw new Failure("PRESERVATION_FAILED");
        return actual;
    }

    private static String sheetPart(Path source)throws Exception {
        try(OPCPackage pkg=OPCPackage.open(source.toFile(),PackageAccess.READ)) {
            var sheets=(XSSFReader.SheetIterator)new XSSFReader(pkg).getSheetsData();
            while(sheets.hasNext())try(InputStream ignored=sheets.next()) {
                if("USCostofSupply".equals(sheets.getSheetName()))return sheets.getSheetPart().getPartName().getName().substring(1);
            }
        }
        throw new Failure("STRUCTURE_VALIDATION_FAILED");
    }
    private static InputWorkbookReader.Cell readCell(InputWorkbookReader reader,Path file)throws Exception {
        InputWorkbookReader.Cell[] cell={null};
        reader.readLocal(file,List.of(new InputWorkbookReader.Window("USCostofSupply",row->row==8,Set.of(2),(row,values)->cell[0]=values.get(2))));
        if(cell[0]==null)throw new Failure("PRESERVATION_FAILED");return cell[0];
    }
    private static byte[] replace(byte[] xml,BigDecimal value) {
        String text=new String(xml,StandardCharsets.UTF_8);var cell=CELL.matcher(text);
        if(!cell.find())throw new Failure("STRUCTURE_VALIDATION_FAILED");
        int start=cell.start(2),end=cell.end(2);String body=cell.group(2);
        if(cell.find()||body.contains("<f"))throw new Failure("STRUCTURE_VALIDATION_FAILED");
        var number=Pattern.compile("(<v>)([^<]*)(</v>)").matcher(body);
        if(!number.find())throw new Failure("STRUCTURE_VALIDATION_FAILED");
        String replaced=body.substring(0,number.start(2))+value.toPlainString()+body.substring(number.end(2));
        if(number.find())throw new Failure("STRUCTURE_VALIDATION_FAILED");
        return (text.substring(0,start)+replaced+text.substring(end)).getBytes(StandardCharsets.UTF_8);
    }
    private static void validateArchive(ZipFile zip) {
        if(zip.size()>10000)throw new Failure("STRUCTURE_VALIDATION_FAILED");
        long total=0;Set<String> names=new java.util.HashSet<>();var entries=zip.entries();
        while(entries.hasMoreElements()) {
            var entry=entries.nextElement();long size=entry.getSize();total+=size;
            if(!names.add(entry.getName())||size<0||size>512L*1024*1024||total>2L*1024*1024*1024
                ||size>Math.max(1,entry.getCompressedSize())*1000)throw new Failure("STRUCTURE_VALIDATION_FAILED");
        }
    }
    private static void verify(Path source,Path saved,String sheet,byte[] expected)throws Exception {
        try(ZipFile before=new ZipFile(source.toFile());ZipFile after=new ZipFile(saved.toFile())) {
            validateArchive(after);if(before.size()!=after.size())throw new Failure("PRESERVATION_FAILED");
            var entries=before.entries();
            while(entries.hasMoreElements()) {
                var a=entries.nextElement();var b=after.getEntry(a.getName());
                if(b==null||a.getMethod()!=b.getMethod()||a.getTime()!=b.getTime()
                    ||!java.util.Objects.equals(a.getComment(),b.getComment())||!Arrays.equals(a.getExtra(),b.getExtra()))throw new Failure("PRESERVATION_FAILED");
                try(InputStream input=after.getInputStream(b);InputStream original=before.getInputStream(a)) {
                    String wanted=a.getName().equals(sheet)?digest(new java.io.ByteArrayInputStream(expected)):digest(original);
                    if(!wanted.equals(digest(input)))throw new Failure("PRESERVATION_FAILED");
                }
            }
        }
    }
    private static String digest(InputStream input)throws Exception {
        var hash=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];int count;
        while((count=input.read(buffer))!=-1)hash.update(buffer,0,count);return HexFormat.of().formatHex(hash.digest());
    }
    private static boolean number(JsonNode value){return value.isNumber()&&value.decimalValue().compareTo(VALUE)==0;}
    private static InputDraftException unsupported(Draft draft){return InputDraftException.conflict("POLICY_UNSUPPORTED",draft.revision());}
    static final class Failure extends RuntimeException {
        final String reason;
        Failure(String reason){super(reason);this.reason=reason;}
    }
}
