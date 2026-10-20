package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.mi.dto.response.fundamentalmodel.ModelInputDraftResponses.Problem;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Component;

/** 발행된 고정 라벨만 대조한다. 사용자 값/기간 헤더나 개발 시트를 양식 불일치로 취급하지 않는다. */
@Component
public class InputUploadValidation {
    static final List<String> KINDS=List.of("DEMAND","SUPPLY","TRADE","OUTPUT");
    static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String XLSM="application/vnd.ms-excel.sheet.macroEnabled.12";
    private final InputWorkbookReader reader;
    private final InputDraftJson json;
    public InputUploadValidation(InputWorkbookReader reader,InputDraftJson json){this.reader=reader;this.json=json;}
    static final class Failure extends RuntimeException {
        final Problem problem;
        Failure(String reason,List<Object> issues){problem=new Problem(reason,null,List.copyOf(issues));}
    }
    static Failure issue(String scope,String kind,String sheet,String table,String cell,String reason) {
        Map<String,Object> issue=new LinkedHashMap<>();issue.put("scope",scope);issue.put("kind",kind);
        issue.put("sheetName",sheet);issue.put("tableId",table);issue.put("cellRef",cell);issue.put("reason",reason);
        return new Failure("STRUCTURE_VALIDATION_FAILED",List.of(issue));
    }
    static String kind(String filename) {
        var matches=KINDS.stream().filter(k->filename.toUpperCase(java.util.Locale.ROOT).contains(k)).toList();
        if(matches.size()!=1)throw issue("FILE",null,null,null,null,matches.isEmpty()?"ROLE_NOT_DETECTED":"ROLE_AMBIGUOUS");
        return matches.get(0);
    }
    List<RowRef> validate(FileRef file,String company,Catalog catalog,String extension,String declared) {
        if(catalog.roles().size()!=4||!Set.copyOf(catalog.roles()).equals(Set.copyOf(KINDS)))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        var tables=catalog.tables().stream().filter(t->t.role().equals(file.role())).toList();
        if(tables.isEmpty())throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        return reader.withLocal(file,company,path->{
            List<HeaderCheck> headerChecks=new ArrayList<>();
            try {
                // 요청 크기와 별개로 압축 해제 총량도 제한한다. 실제 entry 크기는 아래 streaming CRC로 재검증한다.
                try(var zip=new java.util.zip.ZipFile(path.toFile())) {
                    long expanded=0;var entries=zip.entries();
                    while(entries.hasMoreElements()) {
                        long size=entries.nextElement().getSize();
                        if(size<0||size>512L*1024*1024-expanded)throw issue("FILE",file.role(),null,null,null,"FILE_FORMAT_MISMATCH");
                        expanded+=size;
                    }
                }
                InputDraftFinalizationFiles.validatePackage(path);
                try(OPCPackage pkg=OPCPackage.open(path.toFile(),PackageAccess.READ)) {
                    String type=extension.equals("xlsx")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml":"application/vnd.ms-excel.sheet.macroEnabled.main+xml";
                    if(pkg.getPartsByContentType(type).size()!=1||(!declared.equalsIgnoreCase("application/octet-stream")&&!declared.equalsIgnoreCase(extension.equals("xlsx")?XLSX:XLSM)))
                        throw issue("FILE",file.role(),null,null,null,"FILE_FORMAT_MISMATCH");
                    if(extension.equals("xlsx")&&pkg.getParts().stream().anyMatch(p->p.getContentType().toLowerCase().contains("vba")))
                        throw issue("FILE",file.role(),null,null,null,"FILE_FORMAT_MISMATCH");
                    Set<String> visible=new java.util.HashSet<>();var parser=org.apache.poi.util.XMLHelper.newXMLReader();
                    parser.setContentHandler(new org.xml.sax.helpers.DefaultHandler(){
                        @Override public void startElement(String uri,String local,String name,org.xml.sax.Attributes attributes) {
                            if("sheet".equals(local)&&(attributes.getValue("state")==null||"visible".equals(attributes.getValue("state"))))visible.add(attributes.getValue("name"));
                        }
                    });
                    try(var workbook=new org.apache.poi.xssf.eventusermodel.XSSFReader(pkg).getWorkbookData()){parser.parse(new org.xml.sax.InputSource(workbook));}
                    for(TableRef table:tables)if(!visible.contains(table.sheet()))throw issue("SHEET",file.role(),table.sheet(),null,null,"SHEET_MISSING");
                }
                List<RowRef> rows=new ArrayList<>();List<InputWorkbookReader.Window> windows=new ArrayList<>();
                for(TableRef table:tables) {
                    var definition=json.definition(table.definition());var policy=InputReadDefinition.parse(definition);
                    JsonNode validation=definition.path("uploadValidation");
                    if(!validation.isObject()||validation.path("schemaVersion").asInt()!=1||!validation.path("headerCells").isArray()
                        ||(!policy.headerRows().isEmpty()&&validation.path("headerCells").isEmpty()))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                    for(JsonNode header:validation.path("headerCells")) {
                        int r=header.path("rowNumber").asInt(),c=header.path("columnNumber").asInt();JsonNode expected=header.get("value");
                        if(r<1||r>1048576||c<1||c>16384||expected==null||(!expected.isTextual()&&!expected.isNumber()))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                        boolean[] found={false};
                        windows.add(new InputWorkbookReader.Window(table.sheet(),row->row==r,Set.of(c),(row,cells)->{
                            var actual=cells.get(c);boolean same=actual!=null&&!actual.formula()&&(expected.isTextual()?expected.textValue().equals(actual.raw()):actual.raw() instanceof Number&&new BigDecimal(actual.raw().toString()).compareTo(expected.decimalValue())==0);
                            if(!same)throw issue("HEADER",file.role(),table.sheet(),table.key(),CellReference.convertNumToColString(c-1)+r,"HEADER_MISMATCH");found[0]=true;
                        }));
                        // Missing header rows are checked after the streaming pass.
                        headerChecks.add(new HeaderCheck(found,file.role(),table.sheet(),table.key(),c,r));
                    }
                    windows.add(new InputWorkbookReader.Window(table.sheet(),policy::body,Set.copyOf(policy.identityColumns()),(row,cells)->{
                        boolean active=false;
                        for(int c:policy.identityColumns()) {
                            var value=cells.get(c);if(value==null)continue;
                            if(value.formula())throw issue("TABLE",file.role(),table.sheet(),table.key(),null,"TABLE_STRUCTURE_MISMATCH");
                            if(!(value.raw() instanceof String text)||!text.isBlank())active=true;
                        }
                        if(active)rows.add(new RowRef(table.id(),row,UUID.randomUUID().toString()));
                    }));
                }
                try {reader.readLocal(path,windows);}
                catch(InputDraftException ex){if("POLICY_INCOMPLETE".equals(ex.getReason()))throw issue("SHEET",file.role(),null,null,null,"SHEET_MISSING");throw ex;}
                for(HeaderCheck check:headerChecks)if(!check.found[0])throw issue("HEADER",check.kind,check.sheet,check.table,CellReference.convertNumToColString(check.column-1)+check.row,"HEADER_MISMATCH");
                return List.copyOf(rows);
            } catch(Failure|InputDraftException ex){throw ex;}
            catch(Exception ex){throw issue("FILE",file.role(),null,null,null,"FILE_FORMAT_MISMATCH");}
            finally {headerChecks.clear();}
        });
    }
    private record HeaderCheck(boolean[] found,String kind,String sheet,String table,int column,int row) {}
}
