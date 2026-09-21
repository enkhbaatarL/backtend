package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellReference;

/**
 * xlsm(zip) 직접 조작 병합 엔진 (패키지 외부 비공개) — POI DOM을 만들지 않는다.
 *
 * 동작 원리:
 * - 읽기: 기입 대상 시트의 셀 "값"만 StAX 커서로 훑어 희소 맵으로 만든다(수십 KB~수 MB).
 *   문자열 셀은 sharedStrings 테이블로 해석하며, 값 정규화는 기존 POI 경로와 동일 규칙이다.
 * - 기입: 기입 로직(Fixed/MatrixWriter)이 MergeSheet.writeValue로 남긴 변경을 셀 좌표 단위로 축적한다.
 * - 쓰기(writeTo): zip entry를 순회하며 변경 없는 entry는 바이트 그대로 복사하고,
 *   변경된 시트 XML만 StAX 이벤트로 통과시키며 해당 셀의 값을 치환하거나(<v> 교체, 수식 <f>는 보존)
 *   없던 셀/행을 자리에 맞춰 삽입한다. workbook.xml에는 재계산 플래그(fullCalcOnLoad)를 켠다.
 *
 * 결과: 파일 크기와 무관하게 메모리 사용이 수십 MB 수준으로 고정된다. 매크로(vbaProject)·서식 등
 * 건드리지 않는 entry는 바이트 동일 복사라 원본 보존성은 POI 재직렬화보다 오히려 높다.
 *
 * 알려진 허용 편차: 시트의 dimension(전체 범위) 선언은 갱신하지 않는다 — Excel/openpyxl 모두 실제 행을 기준으로 읽는다.
 */
@Slf4j
final class XlsmStreamMerger implements AutoCloseable {

    private static final String WORKBOOK_ENTRY = "xl/workbook.xml";
    private static final String WORKBOOK_RELS_ENTRY = "xl/_rels/workbook.xml.rels";
    private static final String SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml";

    private final Path source;
    private final String sourceLabel;
    private final ZipFile zip;
    private final Map<String, String> sheetEntryByName;
    private List<String> sharedStrings;
    private final Map<String, XmlSheet> openedSheets = new LinkedHashMap<>();

    XlsmStreamMerger(Path source, String sourceLabel) {
        this.source = source;
        this.sourceLabel = sourceLabel;
        try {
            this.zip = new ZipFile(source.toFile());
            this.sheetEntryByName = resolveSheetEntries();
        } catch (Exception e) {
            log.error("[ModelInput Merge] 원본 오픈 실패: {}", sourceLabel, e);
            throw internalError("수급모델 입력 병합 실패 — Basecase 원본을 열 수 없습니다: %s".formatted(sourceLabel));
        }
    }

    /** 시트명으로 시트를 연다. 없으면 null — 호출 측이 마스터-원본 정합 오류로 처리한다. */
    MergeSheet sheet(String sheetName) {
        String entryName = sheetEntryByName.get(sheetName);
        if (entryName == null) {
            return null;
        }
        return openedSheets.computeIfAbsent(sheetName, name -> new XmlSheet(entryName, loadSheetValues(entryName)));
    }

    /** 축적된 변경을 적용한 결과 xlsm을 out에 쓴다. 변경 없는 entry는 원본 바이트 그대로 복사한다. */
    void writeTo(Path out) {
        Map<String, XmlSheet> modifiedByEntry = new HashMap<>();
        openedSheets.values().stream()
            .filter(sheet -> !sheet.pendingWrites.isEmpty())
            .forEach(sheet -> modifiedByEntry.put(sheet.entryName, sheet));

        try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(out));
             ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                zipOut.putNextEntry(new ZipEntry(entry.getName()));
                XmlSheet modified = modifiedByEntry.get(entry.getName());
                if (modified != null) {
                    transformSheet(entry, modified, zipOut);
                } else if (WORKBOOK_ENTRY.equals(entry.getName())) {
                    transformWorkbook(entry, zipOut);
                } else {
                    copyRaw(entry, zipOut);
                }
                zipOut.closeEntry();
            }
            zipOut.finish();
        } catch (Exception e) {
            log.error("[ModelInput Merge] 병합 파일 생성 실패: {}", sourceLabel, e);
            throw internalError("수급모델 입력 병합 실패 — 병합 파일 생성에 실패했습니다: %s".formatted(sourceLabel));
        }
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException e) {
            log.warn("[ModelInput Merge] 원본 zip close 실패: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 시트/문자열 로드

    /** workbook.xml(시트명-rId)과 rels(rId-entry 경로)를 대조해 시트명 → zip entry 매핑을 만든다. */
    private Map<String, String> resolveSheetEntries() throws IOException, XMLStreamException {
        Map<String, String> relTargets = new HashMap<>();
        try (InputStream in = openEntry(WORKBOOK_RELS_ENTRY)) {
            XMLStreamReader reader = newStreamReader(in);
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT && "Relationship".equals(reader.getLocalName())) {
                    relTargets.put(reader.getAttributeValue(null, "Id"), normalizeTarget(reader.getAttributeValue(null, "Target")));
                }
            }
        }

        Map<String, String> entriesByName = new LinkedHashMap<>();
        try (InputStream in = openEntry(WORKBOOK_ENTRY)) {
            XMLStreamReader reader = newStreamReader(in);
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT && "sheet".equals(reader.getLocalName())) {
                    String name = reader.getAttributeValue(null, "name");
                    String relId = relationshipId(reader);
                    String target = relTargets.get(relId);
                    if (name != null && target != null) {
                        entriesByName.put(name, target);
                    }
                }
            }
        }
        return entriesByName;
    }

    private String relationshipId(XMLStreamReader reader) {
        for (int index = 0; index < reader.getAttributeCount(); index++) {
            if ("id".equals(reader.getAttributeLocalName(index))) {
                return reader.getAttributeValue(index);
            }
        }
        return null;
    }

    private String normalizeTarget(String target) {
        if (target == null) {
            return null;
        }
        if (target.startsWith("/")) {
            return target.substring(1);
        }
        return "xl/" + target;
    }

    /** sharedStrings 테이블을 문자열 목록으로 로드한다 (없으면 빈 목록). 발음(rPh) 텍스트는 제외한다. */
    private List<String> sharedStrings() {
        if (sharedStrings != null) {
            return sharedStrings;
        }
        List<String> strings = new ArrayList<>();
        if (zip.getEntry(SHARED_STRINGS_ENTRY) != null) {
            try (InputStream in = openEntry(SHARED_STRINGS_ENTRY)) {
                XMLStreamReader reader = newStreamReader(in);
                StringBuilder current = null;
                int phoneticDepth = 0;
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String local = reader.getLocalName();
                        if ("si".equals(local)) {
                            current = new StringBuilder();
                        } else if ("rPh".equals(local)) {
                            phoneticDepth++;
                        } else if ("t".equals(local) && current != null && phoneticDepth == 0) {
                            current.append(reader.getElementText());
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        String local = reader.getLocalName();
                        if ("si".equals(local) && current != null) {
                            strings.add(current.toString());
                            current = null;
                        } else if ("rPh".equals(local)) {
                            phoneticDepth--;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[ModelInput Merge] sharedStrings 파싱 실패: {}", sourceLabel, e);
                throw internalError("수급모델 입력 병합 실패 — 원본 문자열 테이블을 읽을 수 없습니다: %s".formatted(sourceLabel));
            }
        }
        sharedStrings = strings;
        return strings;
    }

    /** 시트 XML을 1회 훑어 셀 "값"만 희소 맵(row → col → 정규화 값)으로 만든다. */
    private TreeMap<Integer, TreeMap<Integer, Object>> loadSheetValues(String entryName) {
        TreeMap<Integer, TreeMap<Integer, Object>> values = new TreeMap<>();
        try (InputStream in = openEntry(entryName)) {
            XMLStreamReader reader = newStreamReader(in);
            int currentRowIndex = -1;
            int lastColumnIndex = -1;
            while (reader.hasNext()) {
                if (reader.next() != XMLStreamConstants.START_ELEMENT) {
                    continue;
                }
                String local = reader.getLocalName();
                if ("row".equals(local)) {
                    String rowRef = reader.getAttributeValue(null, "r");
                    currentRowIndex = rowRef != null ? Integer.parseInt(rowRef) - 1 : currentRowIndex + 1;
                    lastColumnIndex = -1;
                } else if ("c".equals(local)) {
                    String cellRef = reader.getAttributeValue(null, "r");
                    int columnIndex = cellRef != null ? columnOf(cellRef) : lastColumnIndex + 1;
                    lastColumnIndex = columnIndex;
                    Object value = readCellValue(reader);
                    if (value != null) {
                        values.computeIfAbsent(currentRowIndex, key -> new TreeMap<>()).put(columnIndex, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[ModelInput Merge] 시트 파싱 실패: {} ({})", entryName, sourceLabel, e);
            throw internalError("수급모델 입력 병합 실패 — 원본 시트를 읽을 수 없습니다: %s".formatted(sourceLabel));
        }
        return values;
    }

    /**
     * c 요소 내부(f/v/is)를 소비하고 정규화된 값을 반환한다.
     * 기존 POI 경로와 동일 규칙: 숫자 Double, 문자 trim(빈 문자열 null), boolean, ERROR/BLANK null.
     * 수식 셀은 캐시값(v)을 쓰며, 캐시가 없는 숫자형 수식 셀은 POI와 동일하게 0.0으로 본다.
     */
    private Object readCellValue(XMLStreamReader reader) throws XMLStreamException {
        String type = reader.getAttributeValue(null, "t");
        boolean hasFormula = false;
        String rawValue = null;
        String inlineText = null;
        int depth = 0;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String local = reader.getLocalName();
                if (depth == 0 && "f".equals(local)) {
                    hasFormula = true;
                    skipElement(reader);
                } else if (depth == 0 && "v".equals(local)) {
                    rawValue = reader.getElementText();
                } else if (depth == 0 && "is".equals(local)) {
                    inlineText = readInlineString(reader);
                } else {
                    depth++;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (depth == 0 && "c".equals(reader.getLocalName())) {
                    break;
                }
                if (depth > 0) {
                    depth--;
                }
            }
        }
        return normalizeCellValue(type, hasFormula, rawValue, inlineText);
    }

    private Object normalizeCellValue(String type, boolean hasFormula, String rawValue, String inlineText) {
        if ("inlineStr".equals(type)) {
            return normalizeString(inlineText);
        }
        if ("s".equals(type)) {
            if (rawValue == null) {
                return null;
            }
            List<String> table = sharedStrings();
            int index = Integer.parseInt(rawValue.trim());
            return index >= 0 && index < table.size() ? normalizeString(table.get(index)) : null;
        }
        if ("str".equals(type)) {
            return normalizeString(rawValue);
        }
        if ("b".equals(type)) {
            return rawValue != null && ("1".equals(rawValue.trim()) || "true".equalsIgnoreCase(rawValue.trim()));
        }
        if ("e".equals(type)) {
            return null;
        }
        // 숫자형 (t 생략 또는 "n")
        if (rawValue == null) {
            return hasFormula ? Double.valueOf(0.0) : null;
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeString(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String readInlineString(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder text = new StringBuilder();
        int phoneticDepth = 0;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String local = reader.getLocalName();
                if ("rPh".equals(local)) {
                    phoneticDepth++;
                } else if ("t".equals(local) && phoneticDepth == 0) {
                    text.append(reader.getElementText());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String local = reader.getLocalName();
                if ("rPh".equals(local)) {
                    phoneticDepth--;
                } else if ("is".equals(local)) {
                    break;
                }
            }
        }
        return text.toString();
    }

    private void skipElement(XMLStreamReader reader) throws XMLStreamException {
        int depth = 1;
        while (depth > 0 && reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }

    // ---------------------------------------------------------------- 시트 변환 (쓰기)

    /**
     * 시트 XML을 이벤트로 통과시키며 pendingWrites를 자리에 반영한다.
     * 기존 셀은 값(<v>)만 교체하고 스타일(s)·수식(<f>)은 보존한다. 없던 셀/행은 좌표 순서에 맞춰 삽입한다.
     */
    private void transformSheet(ZipEntry entry, XmlSheet sheet, OutputStream out) throws IOException, XMLStreamException {
        TreeMap<Integer, TreeMap<Integer, Object>> pending = new TreeMap<>();
        sheet.pendingWrites.forEach((row, cols) -> pending.put(row, new TreeMap<>(cols)));

        XMLEventFactory eventFactory = XMLEventFactory.newFactory();
        try (InputStream in = openEntry(entry.getName())) {
            XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(in);
            XMLEventWriter writer = XMLOutputFactory.newFactory().createXMLEventWriter(nonClosing(out), StandardCharsets.UTF_8.name());
            String nsUri = null;
            boolean inSheetData = false;
            int lastRowIndex = -1;
            TreeMap<Integer, Object> rowPending = null;

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartElement()) {
                    StartElement start = event.asStartElement();
                    String local = start.getName().getLocalPart();
                    if (nsUri == null) {
                        nsUri = start.getName().getNamespaceURI();
                    }
                    if ("sheetData".equals(local)) {
                        inSheetData = true;
                        writer.add(event);
                        continue;
                    }
                    if (inSheetData && "row".equals(local)) {
                        Attribute rowRef = start.getAttributeByName(new QName("r"));
                        int rowIndex = rowRef != null ? Integer.parseInt(rowRef.getValue()) - 1 : lastRowIndex + 1;
                        lastRowIndex = rowIndex;
                        emitPendingRowsBefore(pending, rowIndex, eventFactory, nsUri, writer);
                        rowPending = pending.remove(rowIndex);
                        writer.add(event);
                        continue;
                    }
                    if (rowPending != null && "c".equals(local)) {
                        Attribute cellRef = start.getAttributeByName(new QName("r"));
                        Integer columnIndex = cellRef != null ? columnOf(cellRef.getValue()) : null;
                        if (columnIndex != null) {
                            emitPendingCellsBefore(rowPending, lastRowIndex, columnIndex, eventFactory, nsUri, writer);
                            if (rowPending.containsKey(columnIndex)) {
                                rewriteCell(reader, start, rowPending.remove(columnIndex), eventFactory, nsUri, writer);
                                continue;
                            }
                        }
                        writer.add(event);
                        continue;
                    }
                } else if (event.isEndElement()) {
                    EndElement end = event.asEndElement();
                    String local = end.getName().getLocalPart();
                    if (rowPending != null && "row".equals(local)) {
                        emitPendingCellsBefore(rowPending, lastRowIndex, Integer.MAX_VALUE, eventFactory, nsUri, writer);
                        rowPending = null;
                        writer.add(event);
                        continue;
                    }
                    if (inSheetData && "sheetData".equals(local)) {
                        emitPendingRowsBefore(pending, Integer.MAX_VALUE, eventFactory, nsUri, writer);
                        inSheetData = false;
                        writer.add(event);
                        continue;
                    }
                }
                writer.add(event);
            }
            writer.flush();
        }
    }

    /** rowIndexExclusive 이전의 미반영 행 전부를 신규 <row>로 삽입한다. */
    private void emitPendingRowsBefore(
        TreeMap<Integer, TreeMap<Integer, Object>> pending,
        int rowIndexExclusive,
        XMLEventFactory factory,
        String nsUri,
        XMLEventWriter writer
    ) throws XMLStreamException {
        Iterator<Map.Entry<Integer, TreeMap<Integer, Object>>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TreeMap<Integer, Object>> row = iterator.next();
            if (row.getKey() >= rowIndexExclusive) {
                return;
            }
            writer.add(factory.createStartElement("", nsUri, "row",
                List.of(factory.createAttribute("r", String.valueOf(row.getKey() + 1))).iterator(), null));
            for (Map.Entry<Integer, Object> cell : row.getValue().entrySet()) {
                emitNewCell(row.getKey(), cell.getKey(), cell.getValue(), factory, nsUri, writer);
            }
            writer.add(factory.createEndElement("", nsUri, "row"));
            iterator.remove();
        }
    }

    /** columnIndexExclusive 이전의 미반영 셀들을 신규 <c>로 삽입한다. */
    private void emitPendingCellsBefore(
        TreeMap<Integer, Object> rowPending,
        int rowIndex,
        int columnIndexExclusive,
        XMLEventFactory factory,
        String nsUri,
        XMLEventWriter writer
    ) throws XMLStreamException {
        Iterator<Map.Entry<Integer, Object>> iterator = rowPending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Object> cell = iterator.next();
            if (cell.getKey() >= columnIndexExclusive) {
                return;
            }
            emitNewCell(rowIndex, cell.getKey(), cell.getValue(), factory, nsUri, writer);
            iterator.remove();
        }
    }

    /** 신규 셀 — 숫자는 숫자 셀, 문자열은 inlineStr 셀로 쓴다 (sharedStrings 테이블 수정 회피). */
    private void emitNewCell(int rowIndex, int columnIndex, Object value, XMLEventFactory factory, String nsUri, XMLEventWriter writer)
        throws XMLStreamException {
        String cellRef = CellReference.convertNumToColString(columnIndex) + (rowIndex + 1);
        if (value instanceof Double number) {
            writer.add(factory.createStartElement("", nsUri, "c",
                List.of(factory.createAttribute("r", cellRef)).iterator(), null));
            writer.add(factory.createStartElement("", nsUri, "v", null, null));
            writer.add(factory.createCharacters(numberText(number)));
            writer.add(factory.createEndElement("", nsUri, "v"));
        } else {
            writer.add(factory.createStartElement("", nsUri, "c",
                List.of(factory.createAttribute("r", cellRef), factory.createAttribute("t", "inlineStr")).iterator(), null));
            writer.add(factory.createStartElement("", nsUri, "is", null, null));
            writer.add(factory.createStartElement("", nsUri, "t", null, null));
            writer.add(factory.createCharacters(String.valueOf(value)));
            writer.add(factory.createEndElement("", nsUri, "t"));
            writer.add(factory.createEndElement("", nsUri, "is"));
        }
        writer.add(factory.createEndElement("", nsUri, "c"));
    }

    /**
     * 기존 셀의 값 교체 — 원본 c의 r/s 속성과 수식(<f>)은 보존하고 값만 새로 쓴다.
     * 숫자 값이면 t 속성을 제거(숫자 기본형), 문자열 값이면 수식 유무에 따라 str/inlineStr로 쓴다.
     */
    private void rewriteCell(
        XMLEventReader reader,
        StartElement originalStart,
        Object newValue,
        XMLEventFactory factory,
        String nsUri,
        XMLEventWriter writer
    ) throws XMLStreamException {
        List<XMLEvent> formulaEvents = new ArrayList<>();
        int depth = 0;
        boolean inFormula = false;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                String local = event.asStartElement().getName().getLocalPart();
                if (depth == 0 && "f".equals(local)) {
                    inFormula = true;
                }
                depth++;
            }
            if (inFormula) {
                formulaEvents.add(event);
            }
            if (event.isEndElement()) {
                String local = event.asEndElement().getName().getLocalPart();
                depth--;
                if (inFormula && "f".equals(local) && depth == 0) {
                    inFormula = false;
                }
                if (depth < 0 && "c".equals(local)) {
                    break;
                }
            }
        }

        boolean numeric = newValue instanceof Double;
        List<Attribute> attributes = new ArrayList<>();
        Iterator<Attribute> originals = originalStart.getAttributes();
        while (originals.hasNext()) {
            Attribute attribute = originals.next();
            String name = attribute.getName().getLocalPart();
            if ("t".equals(name)) {
                continue;
            }
            attributes.add(attribute);
        }
        if (!numeric) {
            attributes.add(factory.createAttribute("t", formulaEvents.isEmpty() ? "inlineStr" : "str"));
        }

        writer.add(factory.createStartElement("", nsUri, "c", attributes.iterator(), null));
        formulaEvents.forEach(event -> {
            try {
                writer.add(event);
            } catch (XMLStreamException e) {
                throw new IllegalStateException(e);
            }
        });
        if (numeric) {
            writer.add(factory.createStartElement("", nsUri, "v", null, null));
            writer.add(factory.createCharacters(numberText((Double) newValue)));
            writer.add(factory.createEndElement("", nsUri, "v"));
        } else if (formulaEvents.isEmpty()) {
            writer.add(factory.createStartElement("", nsUri, "is", null, null));
            writer.add(factory.createStartElement("", nsUri, "t", null, null));
            writer.add(factory.createCharacters(String.valueOf(newValue)));
            writer.add(factory.createEndElement("", nsUri, "t"));
            writer.add(factory.createEndElement("", nsUri, "is"));
        } else {
            writer.add(factory.createStartElement("", nsUri, "v", null, null));
            writer.add(factory.createCharacters(String.valueOf(newValue)));
            writer.add(factory.createEndElement("", nsUri, "v"));
        }
        writer.add(factory.createEndElement("", nsUri, "c"));
    }

    /** 정수성 숫자는 소수점 없이 쓴다 — 기존 POI 직렬화와 표기 규칙을 맞춘다. */
    private String numberText(Double number) {
        double value = number;
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    // ---------------------------------------------------------------- workbook.xml 변환

    /** workbook.xml에 재계산 플래그(fullCalcOnLoad="1")를 켠다 — 값이 바뀐 셀에 걸린 수식들이 다음 오픈 시 재계산되게. */
    private void transformWorkbook(ZipEntry entry, OutputStream out) throws IOException, XMLStreamException {
        byte[] workbookXml;
        try (InputStream in = openEntry(entry.getName())) {
            workbookXml = in.readAllBytes();
        }
        boolean hasCalcPr = new String(workbookXml, StandardCharsets.UTF_8).contains("<calcPr");

        XMLEventFactory factory = XMLEventFactory.newFactory();
        XMLEventReader reader = XMLInputFactory.newFactory()
            .createXMLEventReader(new java.io.ByteArrayInputStream(workbookXml));
        XMLEventWriter writer = XMLOutputFactory.newFactory().createXMLEventWriter(nonClosing(out), StandardCharsets.UTF_8.name());

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (hasCalcPr && event.isStartElement() && "calcPr".equals(event.asStartElement().getName().getLocalPart())) {
                StartElement start = event.asStartElement();
                List<Attribute> attributes = new ArrayList<>();
                Iterator<Attribute> originals = start.getAttributes();
                while (originals.hasNext()) {
                    Attribute attribute = originals.next();
                    if (!"fullCalcOnLoad".equals(attribute.getName().getLocalPart())) {
                        attributes.add(attribute);
                    }
                }
                attributes.add(factory.createAttribute("fullCalcOnLoad", "1"));
                writer.add(factory.createStartElement("", start.getName().getNamespaceURI(), "calcPr", attributes.iterator(), null));
                continue;
            }
            writer.add(event);
            if (!hasCalcPr && event.isEndElement() && "sheets".equals(event.asEndElement().getName().getLocalPart())) {
                String nsUri = event.asEndElement().getName().getNamespaceURI();
                log.warn("[ModelInput Merge] workbook.xml에 calcPr가 없어 sheets 뒤에 삽입: {}", sourceLabel);
                writer.add(factory.createStartElement("", nsUri, "calcPr",
                    List.of(factory.createAttribute("fullCalcOnLoad", "1")).iterator(), null));
                writer.add(factory.createEndElement("", nsUri, "calcPr"));
                hasCalcPr = true;
            }
        }
        writer.flush();
    }

    // ---------------------------------------------------------------- 공용

    private void copyRaw(ZipEntry entry, OutputStream out) throws IOException {
        try (InputStream in = openEntry(entry.getName())) {
            in.transferTo(out);
        }
    }

    private InputStream openEntry(String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("zip entry not found: " + name);
        }
        return new BufferedInputStream(zip.getInputStream(entry));
    }

    private XMLStreamReader newStreamReader(InputStream in) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory.createXMLStreamReader(in);
    }

    /** ZipOutputStream이 entry 단위 close에 휘말리지 않도록 close를 무시하는 래퍼. */
    private OutputStream nonClosing(OutputStream out) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] buffer, int offset, int length) throws IOException {
                out.write(buffer, offset, length);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                out.flush();
            }
        };
    }

    private int columnOf(String cellRef) {
        int letterEnd = 0;
        while (letterEnd < cellRef.length() && Character.isLetter(cellRef.charAt(letterEnd))) {
            letterEnd++;
        }
        return CellReference.convertColStringToIndex(cellRef.substring(0, letterEnd));
    }

    private SystemErrorException internalError(String message) {
        return SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, message);
    }

    // ---------------------------------------------------------------- MergeSheet 구현

    /**
     * 값 맵 기반 시트 — 읽기는 로드된 정규화 값, 쓰기는 값 맵 갱신 + 변경 좌표 축적.
     * 쓰기가 값 맵에도 반영되므로 같은 병합 내 이후 스캔에 즉시 보인다 (기존 POI 동작과 동일).
     */
    private static final class XmlSheet implements MergeSheet {

        private final String entryName;
        private final TreeMap<Integer, TreeMap<Integer, Object>> values;
        private final TreeMap<Integer, TreeMap<Integer, Object>> pendingWrites = new TreeMap<>();

        private XmlSheet(String entryName, TreeMap<Integer, TreeMap<Integer, Object>> values) {
            this.entryName = entryName;
            this.values = values;
        }

        @Override
        public Object readValue(int rowIndex, int columnIndex) {
            TreeMap<Integer, Object> row = values.get(rowIndex);
            return row == null ? null : row.get(columnIndex);
        }

        @Override
        public Object writeValue(int rowIndex, int columnIndex, Object value) {
            Double number = ExcelCells.asNumber(value);
            Object stored = number != null ? number : String.valueOf(value);
            Object previous = values.computeIfAbsent(rowIndex, key -> new TreeMap<>()).put(columnIndex, stored);
            pendingWrites.computeIfAbsent(rowIndex, key -> new TreeMap<>()).put(columnIndex, stored);
            return previous;
        }

    }

}
