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
    public record Cell(Object raw,Object display,boolean formula) {}
    public record Window(String sheet,IntPredicate rows,Set<Integer> columns,
            BiConsumer<Integer,Map<Integer,Cell>> accept,IntConsumer invalidRow) {
        public Window(String sheet,IntPredicate rows,Set<Integer> columns,BiConsumer<Integer,Map<Integer,Cell>> accept) {
            this(sheet,rows,columns,accept,row->{throw InputDraftException.internal();});
        }
    }
    public void read(FileRef file,String company,List<Window> windows) {
        withLocal(file,company,path->{try {readLocal(path,windows);return null;}catch(InputDraftException ex){throw ex;}catch(Exception ex){throw InputDraftException.internal();}});
    }
    /** 저장소 원본은 공통 업로드 복호화 후 저장된 객체다. 임시 사본만 읽고 항상 제거한다. */
    <T>T withLocal(FileRef file,String company,java.util.function.Function<Path,T> action) {
        Path temp=null;
        try {
            try {
            URI location=URI.create(file.location()),server=URI.create(endpoint);
            if(!Set.of("http","https").contains(location.getScheme())||!location.getAuthority().equals(server.getAuthority())
                ||location.getRawQuery()!=null||location.getFragment()!=null) throw InputDraftException.internal();
            String path=location.getPath();String[] parts=path.split("/",3);
            if(parts.length!=3||(!parts[1].equals(commonBucket)&&!parts[1].equals(bucketPrefix+company.toLowerCase(Locale.ROOT)))
                ||!parts[2].startsWith("SCNRCASE/")||parts[2].contains("..")||parts[2].contains("\\")) throw InputDraftException.missing();
            temp=tempFiles.create();
            try(var object=storage.getObject(parts[1],parts[2]);InputStream stream=object.getObjectContent();var out=Files.newOutputStream(temp)) {
                byte[] buffer=new byte[65536];long size=0;int len;
                while((len=stream.read(buffer))!=-1) { size+=len;if(size>300L*1024*1024)throw InputDraftException.internal();out.write(buffer,0,len); }
            }
            } catch(InputDraftException ex) {throw ex;}
            catch(Exception ex) {throw InputDraftException.internal();}
            return action.apply(temp);
        }
        finally { if(temp!=null)try { Files.deleteIfExists(temp); }catch(IOException ex) {throw InputDraftException.internal();} }
    }
    void readLocal(Path file,List<Window> windows)throws Exception {
        // 압축 해제 후 공유 문자열/스타일의 일괄 메모리 적재 상한. 전역 POI 설정은 변경하지 않는다.
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file.toFile())) {
            if (zip.size() > 10000) throw InputDraftException.internal();
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                long maximum = entry.getName().equals("xl/sharedStrings.xml") ? 64L * 1024 * 1024
                    : entry.getName().equals("xl/styles.xml") ? 8L * 1024 * 1024 : 512L * 1024 * 1024;
                if (entry.getSize() < 0 || entry.getSize() > maximum) throw InputDraftException.internal();
            }
        }
        try(OPCPackage pkg=OPCPackage.open(file.toFile(),PackageAccess.READ)) {
            XSSFReader reader=new XSSFReader(pkg);StylesTable styles=reader.getStylesTable();
            ReadOnlySharedStringsTable strings=new ReadOnlySharedStringsTable(pkg);
            Set<String> visible=new HashSet<>();boolean[] date1904={false};
            var xml=XMLHelper.newXMLReader();xml.setContentHandler(new DefaultHandler(){
                @Override public void startElement(String uri,String local,String name,Attributes a) {
                    if("sheet".equals(local)&& (a.getValue("state")==null||"visible".equals(a.getValue("state")))) visible.add(a.getValue("name"));
                    if("workbookPr".equals(local)) date1904[0]="1".equals(a.getValue("date1904"))||"true".equals(a.getValue("date1904"));
                }});
            try(InputStream workbook=reader.getWorkbookData()){xml.parse(new InputSource(workbook));}
            for(Window window:windows)if(!visible.contains(window.sheet()))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            XSSFReader.SheetIterator sheets=(XSSFReader.SheetIterator)reader.getSheetsData();Set<String> found=new HashSet<>();
            while(sheets.hasNext())try(InputStream data=sheets.next()) {
                String name=sheets.getSheetName();List<Window> selected=windows.stream().filter(w->w.sheet().equals(name)).toList();
                if(selected.isEmpty())continue;found.add(name);
                var parser=XMLHelper.newXMLReader();parser.setContentHandler(new SheetHandler(selected,styles,strings,date1904[0]));parser.parse(new InputSource(data));
            }
            if(windows.stream().anyMatch(w->!found.contains(w.sheet())))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        }
    }
    private static final class SheetHandler extends DefaultHandler {
        private final List<Window> windows;private final StylesTable styles;private final ReadOnlySharedStringsTable strings;
        private final DataFormatter formatter=new DataFormatter(Locale.US);private final boolean date1904;
        private final Set<Integer> invalidColumns=new HashSet<>();
        private Map<Integer,Cell> cells;private List<Window> selected=List.of();private int row,col,style;
        private String type,element;private boolean wanted,formula,hasValue;private final StringBuilder value=new StringBuilder(),inline=new StringBuilder();
        SheetHandler(List<Window> windows,StylesTable styles,ReadOnlySharedStringsTable strings,boolean date1904){this.windows=windows;this.styles=styles;this.strings=strings;this.date1904=date1904;}
        @Override public void startElement(String uri,String local,String q,Attributes a) {
            element=local;
            if("row".equals(local)){row=Integer.parseInt(a.getValue("r"));selected=windows.stream().filter(w->w.rows().test(row)).toList();cells=new HashMap<>();invalidColumns.clear();}
            if("c".equals(local)) {
                CellReference ref=new CellReference(a.getValue("r"));col=ref.getCol()+1;
                if(ref.getRow()+1!=row)throw InputDraftException.internal();
                wanted=selected.stream().anyMatch(w->w.columns().contains(col));formula=false;hasValue=false;type=a.getValue("t");style=a.getValue("s")==null?0:Integer.parseInt(a.getValue("s"));value.setLength(0);inline.setLength(0);
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
            if("row".equals(local))for(Window window:selected) {
                if(window.columns().stream().anyMatch(invalidColumns::contains))window.invalidRow().accept(row);
                Map<Integer,Cell> values=new HashMap<>();
                for(int column:window.columns())if(cells.containsKey(column))values.put(column,cells.get(column));
                window.accept().accept(row,Map.copyOf(values));
            }
            element=null;
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
