#!/usr/bin/env python3
"""Read-only structural inventory for the three model-input workbooks.

The report deliberately excludes cell values and formula text. It emits only
workbook/sheet metadata, counts, coordinate ranges, value-type counts, and
formula dependency sheet names so the catalog inventory can be reproduced
without exposing business data.
"""

from __future__ import annotations

import argparse
import collections
import json
import posixpath
import re
import zipfile
from dataclasses import asdict, dataclass
from io import BytesIO
from pathlib import Path

from lxml import etree


MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
CELL_TAG = f"{{{MAIN_NS}}}c"
FORMULA_TAG = f"{{{MAIN_NS}}}f"
VALUE_TAG = f"{{{MAIN_NS}}}v"
INLINE_TAG = f"{{{MAIN_NS}}}is"
ROW_TAG = f"{{{MAIN_NS}}}row"
SHEET_DATA_TAG = f"{{{MAIN_NS}}}sheetData"
CELL_REF_RE = re.compile(r"^([A-Z]+)(\d+)$")
SHEET_REF_RE = re.compile(
    r"(?:'((?:[^']|'')+)'|([A-Za-z0-9_{} .-]+))!\$?[A-Z]{1,3}\$?\d+"
)


def column_number(letters: str) -> int:
    number = 0
    for letter in letters:
        number = number * 26 + ord(letter) - ord("A") + 1
    return number


def column_letters(number: int) -> str:
    letters = []
    while number:
        number, remainder = divmod(number - 1, 26)
        letters.append(chr(ord("A") + remainder))
    return "".join(reversed(letters))


def normalize_target(source: str, target: str) -> str:
    if target.startswith("/"):
        return target.lstrip("/")
    return posixpath.normpath(posixpath.join(posixpath.dirname(source), target))


@dataclass
class ColumnStats:
    first_row: int | None = None
    last_row: int | None = None
    cells: int = 0
    values: int = 0
    formulas: int = 0
    blank_styled: int = 0
    strings: int = 0
    numbers: int = 0
    booleans: int = 0
    errors: int = 0

    def add(self, row: int, has_value: bool, has_formula: bool, cell_type: str, styled: bool) -> None:
        self.first_row = row if self.first_row is None else min(self.first_row, row)
        self.last_row = row if self.last_row is None else max(self.last_row, row)
        self.cells += 1
        self.values += int(has_value)
        self.formulas += int(has_formula)
        self.blank_styled += int(not has_value and not has_formula and styled)
        if has_value:
            if cell_type in {"s", "str", "inlineStr"}:
                self.strings += 1
            elif cell_type == "b":
                self.booleans += 1
            elif cell_type == "e":
                self.errors += 1
            else:
                self.numbers += 1


def workbook_sheets(archive: zipfile.ZipFile) -> list[dict[str, str]]:
    workbook_path = "xl/workbook.xml"
    workbook = etree.fromstring(archive.read(workbook_path))
    rels = etree.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
    targets = {
        rel.get("Id"): normalize_target(workbook_path, rel.get("Target", ""))
        for rel in rels.findall(f"{{{PKG_REL_NS}}}Relationship")
    }
    result = []
    for index, sheet in enumerate(workbook.findall(f".//{{{MAIN_NS}}}sheet"), start=1):
        relation_id = sheet.get(f"{{{REL_NS}}}id", "")
        result.append(
            {
                "order": index,
                "name": sheet.get("name", ""),
                "state": sheet.get("state", "visible"),
                "path": targets[relation_id],
            }
        )
    return result


def worksheet_relationships(archive: zipfile.ZipFile, sheet_path: str) -> dict[str, str]:
    rel_path = posixpath.join(
        posixpath.dirname(sheet_path),
        "_rels",
        posixpath.basename(sheet_path) + ".rels",
    )
    try:
        root = etree.fromstring(archive.read(rel_path))
    except KeyError:
        return {}
    return {
        rel.get("Id", ""): normalize_target(sheet_path, rel.get("Target", ""))
        for rel in root.findall(f"{{{PKG_REL_NS}}}Relationship")
    }


def worksheet_static_metadata(archive: zipfile.ZipFile, sheet_path: str) -> dict[str, object]:
    dimension = "?"
    merged_ranges: list[str] = []
    validation_ranges: list[str] = []
    auto_filter = None
    protected = False
    table_paths: list[str] = []
    rels = worksheet_relationships(archive, sheet_path)
    with archive.open(sheet_path) as source:
        for event, element in etree.iterparse(source, events=("end",), recover=True):
            local_name = etree.QName(element).localname
            if local_name == "dimension":
                dimension = element.get("ref", "?")
            elif local_name == "mergeCell":
                merged_ranges.append(element.get("ref", ""))
            elif local_name == "dataValidation":
                validation_ranges.extend(element.get("sqref", "").split())
            elif local_name == "autoFilter":
                auto_filter = element.get("ref")
            elif local_name == "sheetProtection":
                protected = True
            elif local_name == "tablePart":
                relation_id = element.get(f"{{{REL_NS}}}id", "")
                if relation_id in rels:
                    table_paths.append(rels[relation_id])
            element.clear()
    tables = []
    for table_path in table_paths:
        try:
            table = etree.fromstring(archive.read(table_path))
        except KeyError:
            continue
        tables.append({"name": table.get("name"), "ref": table.get("ref")})
    return {
        "dimension": dimension,
        "merged_range_count": len(merged_ranges),
        "validation_ranges": validation_ranges,
        "auto_filter": auto_filter,
        "protected": protected,
        "tables": tables,
    }


def inspect_sheet(archive: zipfile.ZipFile, sheet: dict[str, str]) -> dict[str, object]:
    column_stats: dict[int, ColumnStats] = collections.defaultdict(ColumnStats)
    dependency_counts: collections.Counter[str] = collections.Counter()
    external_formula_cells = 0
    formula_cells = 0
    cached_formula_cells = 0
    value_cells = 0
    inline_string_cells = 0
    cell_elements = 0
    row_numbers: set[int] = set()
    min_row = min_col = None
    max_row = max_col = None

    with archive.open(sheet["path"]) as source:
        context = etree.iterparse(source, events=("end",), tag=CELL_TAG, recover=True)
        for _, cell in context:
            match = CELL_REF_RE.match(cell.get("r", ""))
            if not match:
                cell.clear()
                continue
            col = column_number(match.group(1))
            row = int(match.group(2))
            formula = cell.find(FORMULA_TAG)
            value = cell.find(VALUE_TAG)
            inline = cell.find(INLINE_TAG)
            has_formula = formula is not None
            has_cached_value = value is not None
            has_serialized_value = value is not None and value.text not in {None, ""}
            has_inline_value = inline is not None and "".join(inline.itertext()).strip() != ""
            has_value = has_serialized_value or has_inline_value
            styled = cell.get("s") is not None
            cell_type = cell.get("t", "n")

            cell_elements += 1
            formula_cells += int(has_formula)
            cached_formula_cells += int(has_formula and has_cached_value)
            value_cells += int(has_serialized_value)
            inline_string_cells += int(has_inline_value)
            row_numbers.add(row)
            min_row = row if min_row is None else min(min_row, row)
            max_row = row if max_row is None else max(max_row, row)
            min_col = col if min_col is None else min(min_col, col)
            max_col = col if max_col is None else max(max_col, col)
            column_stats[col].add(row, has_value, has_formula, cell_type, styled)

            if has_formula and formula.text:
                formula_text = formula.text
                if "[" in formula_text and "]" in formula_text:
                    external_formula_cells += 1
                for dependency in SHEET_REF_RE.findall(formula_text):
                    name = (dependency[0] or dependency[1]).replace("''", "'").strip()
                    if name and name != sheet["name"]:
                        dependency_counts[name] += 1

            cell.clear()
            parent = cell.getparent()
            while parent is not None and parent.getprevious() is not None:
                del parent.getparent()[0]

    static = worksheet_static_metadata(archive, sheet["path"])
    nonempty_row_runs = []
    if row_numbers:
        start = previous = min(row_numbers)
        for row in sorted(row_numbers)[1:]:
            if row != previous + 1:
                nonempty_row_runs.append(f"{start}:{previous}")
                start = row
            previous = row
        nonempty_row_runs.append(f"{start}:{previous}")

    columns = {
        column_letters(index): asdict(stats)
        for index, stats in sorted(column_stats.items())
    }
    return {
        "order": sheet["order"],
        "name": sheet["name"],
        "state": sheet["state"],
        **static,
        "observed_range": (
            f"{column_letters(min_col)}{min_row}:{column_letters(max_col)}{max_row}"
            if min_row is not None and min_col is not None
            else None
        ),
        "cell_elements": cell_elements,
        "value_cells": value_cells,
        "inline_string_cells": inline_string_cells,
        "display_value_cells": value_cells + inline_string_cells,
        "formula_cells": formula_cells,
        "cached_formula_cells": cached_formula_cells,
        "external_formula_cells": external_formula_cells,
        "nonempty_row_runs": nonempty_row_runs,
        "dependencies": dict(dependency_counts.most_common()),
        "columns": columns,
    }


def inspect_workbook(path: Path) -> dict[str, object]:
    with zipfile.ZipFile(path) as archive:
        sheets = workbook_sheets(archive)
        result_sheets = [inspect_sheet(archive, sheet) for sheet in sheets]
        has_vba = "xl/vbaProject.bin" in archive.namelist()
    return {
        "file": path.name,
        "bytes": path.stat().st_size,
        "has_vba": has_vba,
        "sheet_count": len(result_sheets),
        "sheets": result_sheets,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("workbooks", nargs="+", type=Path)
    parser.add_argument("--compact", action="store_true", help="omit per-column statistics")
    args = parser.parse_args()
    report = {"workbooks": [inspect_workbook(path) for path in args.workbooks]}
    if args.compact:
        for workbook in report["workbooks"]:
            for sheet in workbook["sheets"]:
                sheet.pop("columns", None)
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
