#!/usr/bin/env python3
"""Build a probe workbook to check whether the model reads cached formula values.

Rewrites only the stored value (<v>) of the given cells inside the xlsx/xlsm zip,
without going through Excel. Formulas (<f>), styles, other sheets, VBA, external
links and recalculation flags are left untouched; every other zip entry is copied
as-is. Cell values are never printed — only the number of rewritten cells.

mode
  cached : formula cells only. Keep the formula, replace the stored value. (test A)
  input  : numeric literal cells only. Downstream formula values stay stale. (test B)

Example:
  python3 scripts/model-input-cached-value-probe.py --in SUPPLY.xlsm --out SUPPLY_testA.xlsm \
      --sheet RegasAdjustment --row 20 --cols AF:DK --value 0 --mode cached

Usage and interpretation: docs/work-notes/2026/2026-10-06-model-formula-cell-read-behavior-check.md
"""
import argparse
import re
import sys
import zipfile
from xml.sax.saxutils import escape


def col_to_num(col):
    n = 0
    for ch in col.upper():
        n = n * 26 + (ord(ch) - 64)
    return n


def num_to_col(n):
    s = ""
    while n:
        n, r = divmod(n - 1, 26)
        s = chr(65 + r) + s
    return s


def sheet_entry_name(zf, sheet_name):
    workbook = zf.read("xl/workbook.xml").decode("utf-8")
    rels = zf.read("xl/_rels/workbook.xml.rels").decode("utf-8")
    rid = None
    for m in re.finditer(r"<sheet\b[^>]*/>", workbook):
        tag = m.group(0)
        name = re.search(r'\bname="([^"]*)"', tag)
        if name and name.group(1) == escape(sheet_name, {'"': "&quot;"}):
            rid = re.search(r'\br:id="([^"]*)"', tag) or re.search(r'\bid="([^"]*)"', tag)
            rid = rid.group(1)
            break
    if rid is None:
        sys.exit(f"시트를 찾지 못함: {sheet_name}")
    for m in re.finditer(r"<Relationship\b[^>]*/>", rels):
        tag = m.group(0)
        if re.search(rf'\bId="{re.escape(rid)}"', tag):
            target = re.search(r'\bTarget="([^"]*)"', tag).group(1)
            return target.lstrip("/") if target.startswith("/") else "xl/" + target
    sys.exit(f"시트 관계를 찾지 못함: {rid}")


def rewrite_cell(cell_xml, ref, value, mode):
    has_formula = re.search(r"<f\b", cell_xml) is not None
    open_tag = re.match(r"<c\b[^>]*>", cell_xml).group(0)
    if open_tag.endswith("/>"):
        sys.exit(f"{ref}: 빈 칸이라 바꿀 저장값이 없음")
    if mode == "cached" and not has_formula:
        sys.exit(f"{ref}: 수식이 없는 칸 (cached 모드는 수식 칸만 허용)")
    if mode == "input" and has_formula:
        sys.exit(f"{ref}: 수식이 있는 칸 (input 모드는 수식 없는 칸만 허용)")
    t = re.search(r'\bt="([^"]*)"', open_tag)
    if t and t.group(1) not in ("n",):
        sys.exit(f"{ref}: 숫자 칸이 아님 (t={t.group(1)})")
    new_v = f"<v>{value}</v>"
    if re.search(r"<v>.*?</v>|<v/>", cell_xml, re.S):
        return re.sub(r"<v>.*?</v>|<v/>", new_v, cell_xml, count=1, flags=re.S)
    # 저장값이 없던 수식 칸: </c> 앞에 저장값을 넣는다
    return cell_xml[: -len("</c>")] + new_v + "</c>"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--in", dest="src", required=True)
    p.add_argument("--out", dest="dst", required=True)
    p.add_argument("--sheet", required=True)
    p.add_argument("--row", type=int, required=True)
    p.add_argument("--cols", required=True, help="예: AF:DK")
    p.add_argument("--value", required=True, help="숫자")
    p.add_argument("--mode", choices=["cached", "input"], required=True)
    a = p.parse_args()

    float(a.value)  # 숫자 검증
    start, end = a.cols.upper().split(":")
    refs = [f"{num_to_col(c)}{a.row}" for c in range(col_to_num(start), col_to_num(end) + 1)]

    with zipfile.ZipFile(a.src) as zin:
        entry = sheet_entry_name(zin, a.sheet)
        xml = zin.read(entry).decode("utf-8")

        row_m = re.search(rf'<row\b[^>]*\br="{a.row}"[^>]*>.*?</row>', xml, re.S)
        if not row_m:
            sys.exit(f"{a.sheet} {a.row}행을 찾지 못함")
        row_xml = row_m.group(0)
        for ref in refs:
            cell_m = re.search(rf'<c\b[^>]*\br="{ref}"[^>]*?(?:/>|>.*?</c>)', row_xml, re.S)
            if not cell_m:
                sys.exit(f"{ref}: 칸을 찾지 못함")
            new_cell = rewrite_cell(cell_m.group(0), ref, a.value, a.mode)
            row_xml = row_xml[: cell_m.start()] + new_cell + row_xml[cell_m.end():]
        new_xml = xml[: row_m.start()] + row_xml + xml[row_m.end():]

        with zipfile.ZipFile(a.dst, "w") as zout:
            for info in zin.infolist():
                data = new_xml.encode("utf-8") if info.filename == entry else zin.read(info.filename)
                zout.writestr(info, data)

    print(f"완료: {a.sheet} {a.row}행 {a.cols} {len(refs)}칸 ({a.mode}) -> {a.dst}")


if __name__ == "__main__":
    main()
