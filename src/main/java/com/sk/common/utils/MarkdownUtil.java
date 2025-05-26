package com.sk.common.utils;

import com.sk.common.model.TableRes;
import lombok.experimental.UtilityClass;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.ext.gfm.tables.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Markdown 유틸리티.
 * GFM(GitHub‑Flavored Markdown) 테이블을 다루는 두 가지 핵심 기능을 제공한다.</p>
 *
 * <ol>
 *     <li>{@link #insertTableIds(String)} : 모든 테이블 앞에 <code><!-- tableId:{UUID} --></code> 주석을 강제 삽입해
 *     수정된 Markdown 문자열을 반환한다. 이미 존재하는 경우에는 그대로 유지한다.</li>
 *     <li>{@link #parseTablesWithIds(String)} : Markdown 문서에서 테이블을 찾아 {@link TableRes} 리스트로 변환한다.
 *     바로 위에 있는 tableId 주석을 추출해 {@code tableId} 필드에 매핑하고, 주석이 없다면 새로운 UUID를 부여한다.</li>
 * </ol>
 *
 * <p>모든 정규식 패턴은 <strong>static final</strong> 상수로 정의해 재사용되므로 런타임마다 재컴파일되지 않는다.</p>
 */
@UtilityClass
public final class MarkdownUtil {

    /**
     * 정확히 tableId HTML 주석 한 줄을 매칭.
     */
    private static final Pattern TABLE_ID_COMMENT_LINE = Pattern.compile("^\\s*<!--\\s*tableId:\\{?[0-9a-fA-F\\-]{36}}?\\s*-->\\s*$");

    /**
     * 주석 내부에서 UUID만 추출하기 위한 패턴. (대소문자 무시)
     */
    private static final Pattern TABLE_ID_UUID_EXTRACT = Pattern.compile("tableId:\\{?([0-9a-fA-F\\-]{36})}?", Pattern.CASE_INSENSITIVE);

    /**
     * 파이프(|)가 2개 이상 들어간 헤더 후보 행.
     */
    private static final Pattern PIPE_ROW = Pattern.compile("^\\s*\\|.*\\|.*");

    /**
     * | --- | :---: | 같은 구분선 행.
     */
    private static final Pattern DELIM_ROW = Pattern.compile("^\\s*\\|?\\s*:?-{3,}.*\\|.*");

    /**
     * 모든 테이블 위에 <code><!-- tableId:{UUID} --></code> 주석을 보장한다.
     * 이미 존재한다면 유지하고, 없으면 새로 삽입한다.
     *
     * @param markdown 원본 Markdown 문자열
     * @return tableId 주석이 보강된 새 Markdown 문자열
     */
    public static String insertTableIds(final String markdown) {
        List<String> lines = new ArrayList<>(List.of(markdown.split("\\R", -1))); // 줄 정보 유지
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);

            // 1) 이미 tableId 주석 → 그대로 복사
            if (TABLE_ID_COMMENT_LINE.matcher(line).matches()) {
                out.append(line).append('\n');
                continue;
            }

            // 2) 테이블 시작인지 검사 (헤더 + 구분선)
            if (PIPE_ROW.matcher(line).matches() && isDelimiterRow(lines, i + 1)) {
                int prevIdx = previousNonEmpty(lines, i);
                boolean hasId = prevIdx >= 0 && TABLE_ID_COMMENT_LINE.matcher(lines.get(prevIdx)).matches();
                if (!hasId) {
                    out.append("<!-- tableId:{").append(UUID.randomUUID()).append("} -->\n");
                }
            }

            // 3) 원본 행 복사
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /**
     * Markdown 문서의 모든 테이블을 {@link TableRes} 객체로 파싱한다.
     * <p>테이블 위에 존재하는 tableId 주석을 {@code tableId} 필드에 매핑하고,
     * 주석이 없으면 새 UUID를 부여한다.</p>
     *
     * @param markdown Markdown 문자열 (tableId 주석 유무와 관계없음)
     * @return 테이블 리스트 (문서 등장 순서대로, 불변 리스트)
     */
    public static List<TableRes> parseTablesWithIds(final String markdown) {
        Parser parser = Parser.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();

        Node document = parser.parse(markdown);
        TableCollector collector = new TableCollector();
        document.accept(collector);

        return List.copyOf(collector.tables); // 불변 리스트 반환
    }

    private static boolean isDelimiterRow(List<String> lines, int startIdx) {
        int j = startIdx;
        while (j < lines.size() && lines.get(j).trim().isEmpty()) j++; // blank skip
        return j < lines.size() && DELIM_ROW.matcher(lines.get(j)).matches();
    }

    private static int previousNonEmpty(List<String> lines, int idx) {
        int p = idx - 1;
        while (p >= 0 && lines.get(p).trim().isEmpty()) p--;
        return p;
    }

    /**
     * 문서 트리를 순회하며 테이블과 그 앞의 tableId 주석을 매핑한다.
     */
    private static class TableCollector extends AbstractVisitor {
        private final List<TableRes> tables = new ArrayList<>();
        private String pendingId;

        @Override
        public void visit(HtmlBlock block) {
            extractId(block.getLiteral());
        }

        @Override
        public void visit(HtmlInline inline) {
            extractId(inline.getLiteral());
        }

        @Override
        public void visit(Paragraph p) {
            extractId(literalOf(p));
            visitChildren(p);
        }

        @Override
        public void visit(CustomBlock custom) {
            if (custom instanceof TableBlock table) {
                String id = (pendingId != null) ? pendingId : UUID.randomUUID().toString();
                tables.add(buildTable(table, id));
                pendingId = null;
            } else {
                visitChildren(custom);
            }
        }

        /* ---- helpers ---- */
        private void extractId(String text) {
            if (text == null || pendingId != null) return;
            Matcher m = TABLE_ID_UUID_EXTRACT.matcher(text);
            if (m.find()) pendingId = m.group(1);
        }

        private TableRes buildTable(TableBlock tb, String id) {
            List<TableRes.Column> headers = new ArrayList<>();
            List<TableRes.Row> rows = new ArrayList<>();

            TableHead head = (TableHead) tb.getFirstChild();
            if (head != null) {
                TableRow hdr = (TableRow) head.getFirstChild();
                int colIdx = 1;
                for (Node cell = hdr.getFirstChild(); cell != null; cell = cell.getNext(), colIdx++) {
                    String txt = literalOf(cell);
                    String field = "col_" + colIdx;
                    headers.add(TableRes.Column.of(field, txt));
                }
            }

            TableBody body = (TableBody) (head != null ? head.getNext() : null);
            if (body != null) {
                for (Node rowNode = body.getFirstChild(); rowNode != null; rowNode = rowNode.getNext()) {
                    TableRow row = (TableRow) rowNode;
                    List<String> cells = new ArrayList<>();
                    for (Node cell = row.getFirstChild(); cell != null; cell = cell.getNext()) {
                        cells.add(literalOf(cell));
                    }
                    rows.add(TableRes.Row.of(cells));
                }
            }
            return TableRes.of(id, headers, rows);
        }

        /**
         * 재귀 DFS 로 모든 {@link Text} 노드의 내용을 이어붙인다.
         */
        private String literalOf(Node node) {
            StringBuilder sb = new StringBuilder();
            collect(node, sb);
            return sb.toString().trim();
        }

        private void collect(Node n, StringBuilder sb) {
            if (n instanceof Text t) {
                sb.append(t.getLiteral());
            }
            for (Node child = n.getFirstChild(); child != null; child = child.getNext()) {
                collect(child, sb);
            }
        }
    }

}

