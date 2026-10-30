package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.FileRef;
import com.sk.domain.shared.service.ObjectStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntConsumer;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** 수식 평가 없이 선택한 원본 XML의 저장값만 순차 판독한다. DOM workbook은 만들지 않는다. */
@Component
@RequiredArgsConstructor
public class InputWorkbookReader {
    private final ObjectStorageService storage;
    private final InputTempFiles tempFiles;
    @Value("${ncp.storage.end-point}") private String endpoint;
    @Value("${ncp.storage.bucket-name}") private String commonBucket;
    @Value("${ncp.storage.prefix}") private String bucketPrefix;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private com.sk.domain.shared.service.FileEncryptionPolicy uploadProtection;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private com.sk.domain.shared.client.FileCryptoApiClient uploadCrypto;
    public record Cell(Object raw,Object display,boolean formula) {}
    /** lastRow: 이 창이 필요로 하는 마지막 원본 행. 시트의 모든 창이 값을 주면 그 행 뒤는 판독하지 않는다. */
    public record Window(String sheet,IntPredicate rows,Set<Integer> columns,
            BiConsumer<Integer,Map<Integer,Cell>> accept,IntConsumer invalidRow,int lastRow) {
        public Window(String sheet,IntPredicate rows,Set<Integer> columns,BiConsumer<Integer,Map<Integer,Cell>> accept) {
            this(sheet,rows,columns,accept,row->{throw InputDraftException.internal();});
        }
        public Window(String sheet,IntPredicate rows,Set<Integer> columns,BiConsumer<Integer,Map<Integer,Cell>> accept,IntConsumer invalidRow) {
            this(sheet,rows,columns,accept,invalidRow,Integer.MAX_VALUE);
        }
        void deliverRow(int row,Map<Integer,Cell> cells,Set<Integer> invalidColumns) {
            if(!invalidColumns.isEmpty()&&columns.stream().anyMatch(invalidColumns::contains))invalidRow.accept(row);
            Map<Integer,Cell> values=new HashMap<>();
            for(int column:columns)if(cells.containsKey(column))values.put(column,cells.get(column));
            accept.accept(row,Map.copyOf(values));
        }
    }
    public void read(FileRef file,String company,List<Window> windows) {
        withLocal(file,company,path->InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.WORKBOOK_PARSE,file.role(),()->{
            try {readLocal(path,windows);return null;}
            catch(InputDraftException ex){throw ex;}
            catch(Exception ex){InputDraftDiagnostics.failure(InputDraftDiagnostics.Stage.WORKBOOK_PARSE,file.role(),ex);throw InputDraftException.internal();}
        }));
    }
    /** 원본 구조 확인 전용: 객체와 요청 시트의 표시 상태·본문 part 존재만 확인하고 셀 XML은 판독하지 않는다. */
    public void verifySheets(FileRef file,String company,Set<String> sheets) {
        withLocal(file,company,path->InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.WORKBOOK_PARSE,file.role(),()->{
            try {verifyLocal(path,sheets);return null;}
            catch(InputDraftException ex){throw ex;}
            catch(Exception ex){InputDraftDiagnostics.failure(InputDraftDiagnostics.Stage.WORKBOOK_PARSE,file.role(),ex);throw InputDraftException.internal();}
        }));
    }
    /** 저장소 원본은 공통 업로드 복호화 후 저장된 객체다. 임시 사본만 읽고 항상 제거한다. */
    <T>T withLocal(FileRef file,String company,java.util.function.Function<Path,T> action) {
        return withLocal(file,company,action,InputTempFiles::checkpoint);
    }
    <T>T withLocal(FileRef file,String company,java.util.function.Function<Path,T> action,Runnable checkpoint) {
        return withLocal(file,company,action,checkpoint,InputTempFiles.readTimeout());
    }
    private static final java.util.concurrent.ScheduledExecutorService READ_DEADLINES=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"input-preview-read-deadline");t.setDaemon(true);return t;});
    <T>T withLocal(FileRef file,String company,java.util.function.Function<Path,T> action,Runnable checkpoint,int timeoutMillis) {
        checkpoint.run();
        final long readDeadline=timeoutMillis>0?System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis):Long.MAX_VALUE;
        Path temp=null;
        try {
            long downloadStarted=System.nanoTime();
            boolean downloaded=false;
            try {
            String[] parts=validatedLocation(file,company);
            temp=tempFiles.create();
            try(var object=timeoutMillis>0?storage.getObject(parts[1],parts[2],timeoutMillis):storage.getObject(parts[1],parts[2]);InputStream stream=object.getObjectContent();var out=Files.newOutputStream(temp)) {
                var abort=timeoutMillis>0?READ_DEADLINES.schedule(()->object.getObjectContent().abort(),Math.max(1,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(readDeadline-System.nanoTime())),java.util.concurrent.TimeUnit.MILLISECONDS):null;
                try {
                byte[] buffer=new byte[65536];long size=0;int len;
                var digest=java.security.MessageDigest.getInstance("SHA-256");
                while((len=stream.read(buffer))!=-1) { checkpoint.run();size+=len;if(size>300L*1024*1024||(file.size()!=null&&size>file.size()))throw InputDraftException.internal();digest.update(buffer,0,len);out.write(buffer,0,len); }
                if(file.size()!=null&&size!=file.size())throw InputDraftException.internal();
                if(file.sha256()!=null&&!java.util.HexFormat.of().formatHex(digest.digest()).equals(file.sha256()))throw InputDraftException.internal();
                }finally{if(abort!=null)abort.cancel(false);}
            }
            if(file.fileNo()==null&&uploadProtection!=null&&uploadProtection.isEnabled(company,file.name())) {
                if(uploadCrypto==null)throw InputDraftException.internal();
                byte[] encrypted=Files.readAllBytes(temp),plain=null;
                try {plain=timeoutMillis>0?uploadCrypto.decrypt(encrypted,file.name(),()->{checkpoint.run();return (int)Math.max(1,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(readDeadline-System.nanoTime()));},checkpoint):uploadCrypto.decrypt(encrypted,file.name());if(plain.length>300L*1024*1024)throw InputDraftException.internal();Files.write(temp,plain);}
                finally {java.util.Arrays.fill(encrypted,(byte)0);if(plain!=null)java.util.Arrays.fill(plain,(byte)0);}
            }
            downloaded=true;
            } catch(InputDraftException ex) {throw ex;}
            catch(Exception ex) {checkpoint.run();if(timeoutMillis>0&&timedOut(ex))throw new InputDraftPreserveChange.Failure("TIMEOUT");InputDraftDiagnostics.failure(InputDraftDiagnostics.Stage.WORKBOOK_DOWNLOAD,file.role(),ex);throw InputDraftException.internal();}
            finally {InputDraftDiagnostics.elapsed(InputDraftDiagnostics.Stage.WORKBOOK_DOWNLOAD,file.role(),downloadStarted,downloaded);}
            checkpoint.run();
            return action.apply(temp);
        }
        finally { if(temp!=null)try { Files.deleteIfExists(temp); }catch(IOException ex) {throw InputDraftException.internal();} }
    }
    private static boolean timedOut(Throwable failure) {for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("timeout")||cause instanceof InterruptedException)return true;return false;}
    /** Snapshot hit에도 원본 Reader와 같은 저장소/회사 범위를 검사한다. 객체 IO는 하지 않는다. */
    String[] validatedLocation(FileRef file,String company) {
        URI location=URI.create(file.location()),server=URI.create(endpoint);
        if(!Set.of("http","https").contains(location.getScheme())||!location.getAuthority().equals(server.getAuthority())
            ||location.getRawQuery()!=null||location.getFragment()!=null) throw InputDraftException.internal();
        String[] parts=location.getPath().split("/",3);
        if(parts.length!=3||(!parts[1].equals(commonBucket)&&!parts[1].equals(bucketPrefix+company.toLowerCase(Locale.ROOT)))
            ||!parts[2].startsWith("SCNRCASE/")||parts[2].contains("..")||parts[2].contains("\\")) throw InputDraftException.missing();
        return parts;
    }
    void readLocal(Path file,List<Window> windows)throws Exception {
        checkArchive(file);
        try(OPCPackage pkg=OPCPackage.open(file.toFile(),PackageAccess.READ)) {
            InputTempFiles.checkpoint();XSSFReader reader=new XSSFReader(pkg);StylesTable styles;
            try(InputStream data=reader.getStylesData()){styles=data==null?null:new StylesTable(checked(data));}
            ReadOnlySharedStringsTable strings=new ReadOnlySharedStringsTable(pkg){
                @Override public void readFrom(InputStream data)throws IOException,org.xml.sax.SAXException{super.readFrom(checked(data));}
            };
            InputTempFiles.checkpoint();
            Set<String> visible=new HashSet<>();boolean date1904=workbook(reader,visible);
            for(Window window:windows)if(!visible.contains(window.sheet()))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            XSSFReader.SheetIterator sheets=(XSSFReader.SheetIterator)reader.getSheetsData();Set<String> found=new HashSet<>();
            while(sheets.hasNext())try(InputStream data=sheets.next()) {
                String name=sheets.getSheetName();List<Window> selected=windows.stream().filter(w->w.sheet().equals(name)).toList();
                if(selected.isEmpty())continue;found.add(name);
                var parser=XMLHelper.newXMLReader();parser.setContentHandler(new SheetHandler(selected,styles,strings,date1904));
                try {parser.parse(new InputSource(checked(data)));}
                catch(org.xml.sax.SAXException ex){if(!(ex instanceof StopReading)&&!(ex.getException() instanceof StopReading))throw ex;}
            }
            if(windows.stream().anyMatch(w->!found.contains(w.sheet())))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        }
    }
    /** POI 일괄 파싱도 입력 블록마다 동일 요청 기한을 검사한다. 전역 POI 설정은 바꾸지 않는다. */
    static InputStream checked(InputStream input) {
        return new java.io.FilterInputStream(input){
            @Override public int read()throws IOException{InputTempFiles.checkpoint();return in.read();}
            @Override public int read(byte[] bytes,int off,int len)throws IOException{InputTempFiles.checkpoint();return in.read(bytes,off,Math.min(len,8192));}
            @Override public long skip(long bytes)throws IOException{InputTempFiles.checkpoint();return in.skip(Math.min(bytes,8192));}
        };
    }
    void verifyLocal(Path file,Set<String> requested)throws Exception {
        checkArchive(file);
        try(OPCPackage pkg=OPCPackage.open(file.toFile(),PackageAccess.READ)) {
            XSSFReader reader=new XSSFReader(pkg);Set<String> visible=new HashSet<>();workbook(reader,visible);
            if(!visible.containsAll(requested))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            XSSFReader.SheetIterator sheets=(XSSFReader.SheetIterator)reader.getSheetsData();Set<String> found=new HashSet<>();
            while(sheets.hasNext())try(InputStream ignored=sheets.next()) {if(requested.contains(sheets.getSheetName()))found.add(sheets.getSheetName());}
            if(!found.containsAll(requested))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        }
    }
    // 압축 해제 후 공유 문자열/스타일의 일괄 메모리 적재 상한. 전역 POI 설정은 변경하지 않는다.
    private static void checkArchive(Path file)throws IOException {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file.toFile())) {
            if (zip.size() > 10000) throw InputDraftException.internal();
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                InputTempFiles.checkpoint();
                var entry = entries.nextElement();
                long maximum = entry.getName().equals("xl/sharedStrings.xml") ? 64L * 1024 * 1024
                    : entry.getName().equals("xl/styles.xml") ? 8L * 1024 * 1024 : 512L * 1024 * 1024;
                if (entry.getSize() < 0 || entry.getSize() > maximum) throw InputDraftException.internal();
            }
        }
    }
    /** 표시 시트 이름을 모으고 1904 날짜 체계 여부를 반환한다. */
    private static boolean workbook(XSSFReader reader,Set<String> visible)throws Exception {
        boolean[] date1904={false};
        var xml=XMLHelper.newXMLReader();xml.setContentHandler(new DefaultHandler(){
            @Override public void startElement(String uri,String local,String name,Attributes a) {
                InputTempFiles.checkpoint();
                if("sheet".equals(local)&& (a.getValue("state")==null||"visible".equals(a.getValue("state")))) visible.add(a.getValue("name"));
                if("workbookPr".equals(local)) date1904[0]="1".equals(a.getValue("date1904"))||"true".equals(a.getValue("date1904"));
            }});
        try(InputStream workbook=reader.getWorkbookData()){xml.parse(new InputSource(checked(workbook)));}
        return date1904[0];
    }
    /** 필요한 마지막 행 뒤의 판독 중단 신호. 오류가 아니며 원문/스택을 만들지 않는다. */
    private static final class StopReading extends org.xml.sax.SAXException {
        StopReading(){super("stop");}
        @Override public synchronized Throwable fillInStackTrace(){return this;}
    }
    private static final class SheetHandler extends DefaultHandler {
        private final List<Window> windows;private final StylesTable styles;private final ReadOnlySharedStringsTable strings;
        private final DataFormatter formatter=new DataFormatter(Locale.US);private final boolean date1904;
        private final Set<Integer> invalidColumns=new HashSet<>();private final int stopAfter;private int previousRow;
        private Map<Integer,Cell> cells;private List<Window> selected=List.of();private int row,col,style;
        private String type,element;private boolean wanted,formula,hasValue;private final StringBuilder value=new StringBuilder(),inline=new StringBuilder();
        SheetHandler(List<Window> windows,StylesTable styles,ReadOnlySharedStringsTable strings,boolean date1904){
            this.windows=windows;this.styles=styles;this.strings=strings;this.date1904=date1904;
            stopAfter=windows.stream().mapToInt(Window::lastRow).max().orElse(Integer.MAX_VALUE);
        }
        @Override public void startElement(String uri,String local,String q,Attributes a)throws org.xml.sax.SAXException {
            InputTempFiles.checkpoint();
            element=local;
            if("row".equals(local)){
                row=Integer.parseInt(a.getValue("r"));
                if(stopAfter!=Integer.MAX_VALUE) {
                    // 중단은 행이 오름차순일 때만 안전하다. Excel과 편집 저장(InputWorkbookXml)은 이 순서를 지킨다.
                    if(row<=previousRow)throw InputDraftException.internal();
                    previousRow=row;
                    if(row>stopAfter)throw new StopReading();
                }
                selected=windows.stream().filter(w->w.rows().test(row)).toList();cells=new HashMap<>();invalidColumns.clear();
            }
            if("c".equals(local)) {
                col=column(a.getValue("r"));
                wanted=false;
                for(Window window:selected)if(window.columns().contains(col)){wanted=true;break;}
                formula=false;hasValue=false;type=a.getValue("t");style=a.getValue("s")==null?0:Integer.parseInt(a.getValue("s"));value.setLength(0);inline.setLength(0);
            }
            if(wanted&&"f".equals(local))formula=true;
            if(wanted&&"v".equals(local))hasValue=true;
        }
        @Override public void characters(char[] ch,int start,int len) {
            if(!wanted)return;
            if("v".equals(element))value.append(ch,start,len);
            if("t".equals(element))inline.append(ch,start,len);
            if(value.length()>1_048_576||inline.length()>1_048_576)throw InputDraftException.internal();
        }
        @Override public void endElement(String uri,String local,String q) {
            if(wanted&&"c".equals(local)) {
                try {
                    Cell cell=storedCell();
                    if(cell!=null)cells.put(col,cell);
                } catch(RuntimeException ex) {
                    // 원문 예외/거부값은 보관하지 않는다. 최종 반환 범위가 확정된 뒤 행 실패를 판정한다.
                    invalidColumns.add(col);
                }
            }
            if("row".equals(local))for(Window window:selected)window.deliverRow(row,cells,invalidColumns);
            element=null;
        }
        private int column(String reference) {
            // 일반 XLSX 좌표만 직접 해석한다. 특수/범위 밖 표기는 기존 POI 처리와 오류 동작을 유지한다.
            if(reference!=null&&reference.length()>=2&&reference.length()<=10) {
                int index=0,column=0;
                while(index<reference.length()) {
                    char letter=reference.charAt(index);
                    if(letter<'A'||letter>'Z')break;
                    column=column*26+letter-'A'+1;index++;
                    if(column>16_384)break;
                }
                if(column>0&&column<=16_384&&index<reference.length()
                        &&reference.charAt(index)>='1'&&reference.charAt(index)<='9') {
                    int addressRow=0;
                    while(index<reference.length()) {
                        char digit=reference.charAt(index);
                        if(digit<'0'||digit>'9')break;
                        addressRow=addressRow*10+digit-'0';
                        if(addressRow>1_048_576)break;
                        index++;
                    }
                    if(index==reference.length()) {
                        if(addressRow!=row)throw InputDraftException.internal();
                        return column;
                    }
                }
            }
            CellReference parsed=new CellReference(reference);
            if(parsed.getRow()+1!=row)throw InputDraftException.internal();
            return parsed.getCol()+1;
        }
        private Cell storedCell() {
                Object raw=null,display=null;String text=value.toString();
                if(formula&&!hasValue)throw InputDraftException.internal();
                if("inlineStr".equals(type)){raw=inline.toString();display=raw;}
                else if("s".equals(type)){raw=strings.getItemAt(Integer.parseInt(text)).getString();display=raw;}
                else if("str".equals(type)||"d".equals(type)){raw=text;display=raw;}
                else if("e".equals(type)){FormulaError.forString(text);raw=text;display=text;}
                else if("b".equals(type)){if(!Set.of("0","1").contains(text))throw InputDraftException.internal();raw="1".equals(text);display=(boolean)raw?"TRUE":"FALSE";}
                else if(type==null||"n".equals(type)) {
                    if(text.isEmpty()){if(formula)throw InputDraftException.internal();}
                    else {
                        raw=new BigDecimal(text);XSSFCellStyle cellStyle=styles.getStyleAt(style);
                        display=numericDisplay((BigDecimal)raw,cellStyle);
                    }
                } else throw InputDraftException.internal();
                return raw==null?null:new Cell(raw,display,formula);
        }
        private String numericDisplay(BigDecimal raw,XSSFCellStyle style) {
            double number=raw.doubleValue();
            if(!Double.isFinite(number))throw InputDraftException.internal();
            String format=style.getDataFormatString();
            if("General".equalsIgnoreCase(format))return generalDisplay(number);
            String accounting="_-* #,##0.00_-;-* #,##0.00_-;_-* \"-\"??_-;_-@_-";
            if(format.replace("\\-","-").equals(accounting))format="#,##0.00;-#,##0.00;\"-\";@";
            return formatter.formatRawCellContents(number,style.getDataFormat(),format,date1904);
        }
        /** provision §3.2 승인 표시 규칙. raw BigDecimal은 변경하지 않는다. */
        private static String generalDisplay(double number) {
            if(number==0)return "0";
            BigDecimal value=new BigDecimal(NumberToTextConverter.toText(number));
            BigDecimal absolute=value.abs(),upper=new BigDecimal("100000000000");
            if((absolute.compareTo(new BigDecimal("0.0001"))>=0||absolute.stripTrailingZeros().scale()<=9)
                    &&absolute.compareTo(upper)<0) {
                int digits=Math.max(1,absolute.precision()-absolute.scale());
                BigDecimal rounded=value.setScale(Math.max(0,10-digits),RoundingMode.HALF_UP);
                if(rounded.abs().compareTo(upper)<0)return rounded.stripTrailingZeros().toPlainString();
            }
            BigDecimal rounded=value.round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros();
            int exponent=rounded.precision()-rounded.scale()-1;
            return rounded.movePointLeft(exponent).stripTrailingZeros().toPlainString()+"E"+(exponent>=0?"+":"-")
                +String.format(Locale.ROOT,"%02d",Math.abs(exponent));
        }
    }
}
