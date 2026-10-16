#!/usr/bin/env python3
"""Build a read-only formula compatibility inventory for model-input XLSM files.

The output intentionally excludes cell values and formula text. Formula families
are represented only by hashes, structural attributes, function names, counts,
coordinates, sheet-level dependencies, and whether they are recalculation
candidates or external-reference preservation targets.
"""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import posixpath
import re
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

from lxml import etree


MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
CELL_TAG = f"{{{MAIN_NS}}}c"
FORMULA_TAG = f"{{{MAIN_NS}}}f"
VALUE_TAG = f"{{{MAIN_NS}}}v"

CELL_REF_RE = re.compile(r"^([A-Z]+)(\d+)$")
CELL_TOKEN_RE = re.compile(r"(?<![A-Z0-9_])\$?[A-Z]{1,3}\$?\d+")
RANGE_TOKEN_RE = re.compile(
    r"\$?[A-Z]{1,3}\$?\d+\s*:\s*\$?[A-Z]{1,3}\$?\d+"
)
ROW_RANGE_RE = re.compile(r"(?<![A-Z0-9_])\$?\d+\s*:\s*\$?\d+")
COL_RANGE_RE = re.compile(r"(?<![A-Z0-9_])\$?[A-Z]{1,3}\s*:\s*\$?[A-Z]{1,3}")
FUNCTION_RE = re.compile(r"(?<![A-Z0-9_.])([A-Z_][A-Z0-9_.]*)\s*\(", re.IGNORECASE)
IDENTIFIER_RE = re.compile(r"(?<![A-Z0-9_.])([A-Z_\\][A-Z0-9_.\\]*)", re.IGNORECASE)
SHEET_REF_RE = re.compile(
    r"(?:'((?:[^']|'')+)'|((?:\[[^\]]+\])?[A-Za-z0-9_{} .-]+))!"
    r"\$?[A-Z]{1,3}\$?\d+"
)
EXTERNAL_REF_RE = re.compile(r"\[[^\]]+\]")

VOLATILE_FUNCTIONS = {
    "CELL",
    "INDIRECT",
    "INFO",
    "NOW",
    "OFFSET",
    "RAND",
    "RANDBETWEEN",
    "TODAY",
}
EXTENSION_FUNCTION_PREFIXES = ("_XLFN.", "_XLUDF.", "_XLL.")
KNOWN_FUNCTIONS = {
    "AND",
    "AVERAGEIFS",
    "CHOOSE",
    "COUNTA",
    "COUNTBLANK",
    "COUNTIF",
    "COUNTIFS",
    "IF",
    "IFERROR",
    "INDEX",
    "ISBLANK",
    "ISNUMBER",
    "LEFT",
    "LEN",
    "MATCH",
    "MAX",
    "MIN",
    "OR",
    "SEARCH",
    "SUM",
    "SUMIF",
    "SUMIFS",
    "TEXT",
    "VLOOKUP",
}

MODEL_INPUT_ROLES = ("DEMAND", "SUPPLY", "TRADE", "OUTPUT")


def infer_workbook_role(path: Path) -> str | None:
    normalized_name = path.name.casefold()
    matches = [role for role in MODEL_INPUT_ROLES if role.casefold() in normalized_name]
    return matches[0] if len(matches) == 1 else None


def column_number(letters: str) -> int:
    result = 0
    for letter in letters.replace("$", ""):
        result = result * 26 + ord(letter.upper()) - ord("A") + 1
    return result


def normalize_target(source: str, target: str) -> str:
    if target.startswith("/"):
        return target.lstrip("/")
    return posixpath.normpath(posixpath.join(posixpath.dirname(source), target))


def sha256_path(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def formula_signature(formula: str) -> str:
    normalized = formula.upper()
    normalized = RANGE_TOKEN_RE.sub("CELL_RANGE", normalized)
    normalized = ROW_RANGE_RE.sub("ROW_RANGE", normalized)
    normalized = COL_RANGE_RE.sub("COL_RANGE", normalized)
    normalized = CELL_TOKEN_RE.sub("CELL", normalized)
    normalized = re.sub(r"\s+", "", normalized)
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]


def formula_functions(formula: str) -> list[str]:
    return [match.upper() for match in FUNCTION_RE.findall(formula)]


def sheet_references(formula: str) -> tuple[set[str], set[str], bool]:
    internal: set[str] = set()
    external_sheets: set[str] = set()
    external = bool(EXTERNAL_REF_RE.search(formula))
    for quoted, plain in SHEET_REF_RE.findall(formula):
        name = (quoted or plain).replace("''", "'").strip()
        if not name:
            continue
        if "]" in name:
            external = True
            name = name.split("]", 1)[1]
            external_sheets.add(name)
        else:
            internal.add(name)
    return internal, external_sheets, external


def unqualified_cell_reference(formula: str) -> bool:
    without_qualified = SHEET_REF_RE.sub("SHEET!CELL", formula)
    return bool(CELL_TOKEN_RE.search(without_qualified))


def split_markdown_row(line: str) -> list[str]:
    return [part.strip() for part in line.strip().strip("|").split("|")]


@dataclass(frozen=True)
class LogicalTable:
    role: str
    sheet: str
    key: str
    label: str
    boundary: str
    min_col: int
    min_row: int
    max_col: int
    max_row: int
    data_start_row: int
    editable: bool
    source_candidate: str | None
    status: str

    @property
    def area(self) -> int:
        return (self.max_col - self.min_col + 1) * (self.max_row - self.min_row + 1)

    def contains(self, col: int, row: int) -> bool:
        return self.min_col <= col <= self.max_col and self.min_row <= row <= self.max_row


def parse_a1_range(value: str) -> tuple[int, int, int, int]:
    start, _, end = value.replace("$", "").partition(":")
    end = end or start
    start_match = CELL_REF_RE.match(start)
    end_match = CELL_REF_RE.match(end)
    if start_match is None or end_match is None:
        raise ValueError(f"Unsupported A1 range: {value}")
    return (
        column_number(start_match.group(1)),
        int(start_match.group(2)),
        column_number(end_match.group(1)),
        int(end_match.group(2)),
    )


def table_key(sheet: str, label: str) -> str:
    raw = f"{sheet}__{label}".lower()
    return re.sub(r"[^a-z0-9]+", "_", raw).strip("_")


def source_candidate(editable: str, data_start_row: int) -> str | None:
    if "없음" in editable:
        return None
    explicit = re.search(r"(?<![A-Z])([A-Z]{1,3}\d+)(?!\d)", editable)
    if explicit:
        return explicit.group(1)
    for segment in editable.split(","):
        typed_column = re.match(r"\s*([A-Z]{1,3})(?=[:,(])", segment)
        if typed_column and re.search(r"\((?:N|I|B|T)\)", segment):
            return f"{typed_column.group(1)}{data_start_row}"
    column = re.search(r"(?<![A-Z])([A-Z]{1,3})(?=[:,(]|$)", editable)
    if column:
        return f"{column.group(1)}{data_start_row}"
    return None


def parse_catalog(path: Path) -> tuple[dict[str, dict[str, str]], list[LogicalTable]]:
    classifications: dict[str, dict[str, str]] = collections.defaultdict(dict)
    tables: list[LogicalTable] = []
    section: str | None = None
    role: str | None = None
    current_sheet: str | None = None

    for line in path.read_text(encoding="utf-8").splitlines():
        classification_heading = re.match(r"### 3\.\d+ (DEMAND|SUPPLY|TRADE|OUTPUT)", line)
        table_heading = re.match(r"### 4\.\d+ (DEMAND|SUPPLY|TRADE|OUTPUT)", line)
        if classification_heading:
            section = "classification"
            role = classification_heading.group(1)
            current_sheet = None
            continue
        if table_heading:
            section = "tables"
            role = table_heading.group(1)
            current_sheet = None
            continue
        if line.startswith("## ") or (line.startswith("### ") and not classification_heading and not table_heading):
            section = None
            role = None
            current_sheet = None
            continue
        if role is None or not line.startswith("|"):
            continue
        cells = split_markdown_row(line)
        if section == "classification" and cells and cells[0].isdigit() and len(cells) >= 5:
            classifications[role][cells[1].strip("`")] = cells[4]
            continue
        if section != "tables" or len(cells) < 6 or cells[0].startswith("---"):
            continue
        first = cells[0]
        if first in {"시트 / 표", "같은 시트"}:
            continue
        if first.startswith("같은 시트"):
            if current_sheet is None:
                continue
            label = first.split("/", 1)[1].strip() if "/" in first else current_sheet
        else:
            sheet_match = re.match(r"`([^`]+)`(?:\s*/\s*(.*))?", first)
            if sheet_match is None:
                continue
            current_sheet = sheet_match.group(1)
            label = (sheet_match.group(2) or current_sheet).strip()

        code_values = re.findall(r"`([^`]+)`", cells[1])
        if not code_values:
            continue
        boundary = code_values[0]
        try:
            min_col, min_row, max_col, max_row = parse_a1_range(boundary)
        except ValueError:
            continue
        data_start_row = min_row
        if len(code_values) >= 3:
            data_value = code_values[2]
            data_start_match = re.match(r"(\d+)", data_value)
            if data_start_match:
                data_start_row = int(data_start_match.group(1))
        editable = cells[3]
        tables.append(
            LogicalTable(
                role=role,
                sheet=current_sheet,
                key=table_key(current_sheet, label),
                label=label.strip("`"),
                boundary=boundary,
                min_col=min_col,
                min_row=min_row,
                max_col=max_col,
                max_row=max_row,
                data_start_row=data_start_row,
                editable="없음" not in editable,
                source_candidate=source_candidate(editable, data_start_row),
                status=cells[5],
            )
        )
    return dict(classifications), tables


def workbook_sheets(archive: zipfile.ZipFile) -> list[dict[str, str | int]]:
    workbook_path = "xl/workbook.xml"
    workbook = etree.fromstring(archive.read(workbook_path))
    rels = etree.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
    targets = {
        rel.get("Id"): normalize_target(workbook_path, rel.get("Target", ""))
        for rel in rels.findall(f"{{{PKG_REL_NS}}}Relationship")
    }
    return [
        {
            "order": index,
            "name": sheet.get("name", ""),
            "state": sheet.get("state", "visible"),
            "path": targets[sheet.get(f"{{{REL_NS}}}id", "")],
        }
        for index, sheet in enumerate(workbook.findall(f".//{{{MAIN_NS}}}sheet"), start=1)
    ]


def workbook_metadata(archive: zipfile.ZipFile) -> tuple[dict[str, object], set[str]]:
    workbook = etree.fromstring(archive.read("xl/workbook.xml"))
    calc = workbook.find(f"{{{MAIN_NS}}}calcPr")
    defined_names = workbook.findall(f".//{{{MAIN_NS}}}definedName")
    raw_names = {name.get("name", "").upper() for name in defined_names if name.get("name")}
    scope_counts: collections.Counter[str] = collections.Counter()
    for name in defined_names:
        if name.get("localSheetId") is None:
            scope_counts["workbook"] += 1
        else:
            scope_counts["sheet"] += 1
    connections = 0
    if "xl/connections.xml" in archive.namelist():
        root = etree.fromstring(archive.read("xl/connections.xml"))
        connections = len(root.findall(f".//{{{MAIN_NS}}}connection"))
    metadata = {
        "calculation": dict(calc.attrib) if calc is not None else {},
        "calcChainPresent": "xl/calcChain.xml" in archive.namelist(),
        "definedNameCount": len(defined_names),
        "definedNameScopes": dict(scope_counts),
        "externalLinkPartCount": sum(
            1
            for name in archive.namelist()
            if re.fullmatch(r"xl/externalLinks/externalLink\d+\.xml", name)
        ),
        "connectionCount": connections,
        "vbaPresent": "xl/vbaProject.bin" in archive.namelist(),
    }
    return metadata, raw_names


def worksheet_package_counts(archive: zipfile.ZipFile, sheet_path: str) -> dict[str, int]:
    counts = {"mergedRanges": 0, "validationRules": 0, "tableParts": 0}
    with archive.open(sheet_path) as source:
        for _, element in etree.iterparse(source, events=("end",), recover=True):
            local = etree.QName(element).localname
            if local == "mergeCell":
                counts["mergedRanges"] += 1
            elif local == "dataValidation":
                counts["validationRules"] += 1
            elif local == "tablePart":
                counts["tableParts"] += 1
            element.clear()
    return counts


@dataclass
class FamilyStats:
    family_id: str
    formula_type: str
    representative_cell: str
    count: int = 0
    cached_count: int = 0
    external_count: int = 0
    same_sheet_count: int = 0
    functions: set[str] = field(default_factory=set)
    volatile_functions: set[str] = field(default_factory=set)
    extension_function_candidates: set[str] = field(default_factory=set)
    custom_function_candidates: set[str] = field(default_factory=set)
    referenced_sheets: set[str] = field(default_factory=set)
    external_referenced_sheets: set[str] = field(default_factory=set)
    defined_name_ids: set[str] = field(default_factory=set)
    logical_table_keys: set[str] = field(default_factory=set)

    def as_dict(self) -> dict[str, object]:
        return {
            "familyId": self.family_id,
            "formulaType": self.formula_type,
            "representativeCell": self.representative_cell,
            "cellCount": self.count,
            "cachedCellCount": self.cached_count,
            "externalReferenceCellCount": self.external_count,
            "calculationEligibleFormulaCells": max(self.count - self.external_count, 0),
            "sameSheetReferenceCellCount": self.same_sheet_count,
            "functions": sorted(self.functions),
            "volatileFunctions": sorted(self.volatile_functions),
            "extensionFunctionCandidates": sorted(self.extension_function_candidates),
            "customFunctionCandidates": sorted(self.custom_function_candidates),
            "referencedSheets": sorted(self.referenced_sheets),
            "externalReferencedSheets": sorted(self.external_referenced_sheets),
            "definedNameIds": sorted(self.defined_name_ids),
            "logicalTableKeys": sorted(self.logical_table_keys),
        }


def name_id(name: str) -> str:
    return hashlib.sha256(name.upper().encode("utf-8")).hexdigest()[:12]


def formula_defined_names(formula: str, defined_names: set[str]) -> set[str]:
    # Whole-column ranges such as $AD:$AD are cell references, not names that
    # happen to spell the same letters.
    without_column_ranges = COL_RANGE_RE.sub(" ", formula)
    tokens = {token.upper() for token in IDENTIFIER_RE.findall(without_column_ranges)}
    return {name_id(name) for name in tokens & defined_names}


def matching_table(tables: Iterable[LogicalTable], col: int, row: int) -> LogicalTable | None:
    matches = [table for table in tables if table.contains(col, row)]
    return min(matches, key=lambda table: table.area) if matches else None


def risk_and_oracle(
    category: str,
    formula_count: int,
    external_count: int,
    downstream_targets: list[str],
) -> tuple[str, str]:
    calculation_eligible_count = max(formula_count - external_count, 0)
    if category == "화면 대상":
        oracle = (
            "EXCEL_GOLDEN_REQUIRED"
            if calculation_eligible_count or downstream_targets
            else "PRESERVATION_ONLY"
        )
        return "P0", oracle
    if external_count:
        if category == "제외 후보" or calculation_eligible_count == 0:
            return "P0", "PRESERVATION_ONLY"
        return "P0", "EXCEL_GOLDEN_REQUIRED"
    if category == "참조·계산 전용":
        return "P1", "EXCEL_GOLDEN_REQUIRED"
    if category == "빈 시트":
        return "P2", "NOT_APPLICABLE"
    if formula_count or downstream_targets:
        return "P1", "PRESERVATION_ONLY"
    return "P2", "PRESERVATION_ONLY"


def calculation_policy(formula_count: int, external_count: int) -> str:
    if formula_count == 0:
        return "NOT_APPLICABLE"
    if external_count == 0:
        return "RECALCULATE_CANDIDATE"
    if formula_count == external_count:
        return "PRESERVE_EXTERNAL_ONLY"
    return "MIXED_RECALCULATE_INTERNAL_PRESERVE_EXTERNAL"


def family_risk_and_oracle(
    category: str,
    formula_count: int,
    external_count: int,
    extension_candidates: set[str],
    custom_candidates: set[str],
) -> tuple[str, str]:
    if external_count and formula_count == external_count:
        return "P0", "PRESERVATION_ONLY"
    if external_count:
        return "P0", "EXCEL_GOLDEN_REQUIRED"
    if category == "화면 대상" or extension_candidates or custom_candidates:
        return "P0", "EXCEL_GOLDEN_REQUIRED"
    if category == "참조·계산 전용":
        return "P1", "EXCEL_GOLDEN_REQUIRED"
    if category == "빈 시트":
        return "P2", "NOT_APPLICABLE"
    return "P1", "PRESERVATION_ONLY"


def inspect_sheet(
    archive: zipfile.ZipFile,
    sheet: dict[str, str | int],
    category: str,
    logical_tables: list[LogicalTable],
    defined_names: set[str],
    include_families: bool,
) -> tuple[dict[str, object], dict[str, dict[str, object]]]:
    family_map: dict[str, FamilyStats] = {}
    formula_types: collections.Counter[str] = collections.Counter()
    function_counts: collections.Counter[str] = collections.Counter()
    dependencies: collections.Counter[str] = collections.Counter()
    external_dependencies: collections.Counter[str] = collections.Counter()
    table_stats: dict[str, dict[str, object]] = {}
    for table in logical_tables:
        table_stats[table.key] = {
            "role": table.role,
            "sheet": table.sheet,
            "tableKey": table.key,
            "label": table.label,
            "boundary": table.boundary,
            "editableSource": table.editable,
            "sourceCandidate": table.source_candidate,
            "catalogStatus": table.status,
            "formulaCells": 0,
            "cachedFormulaCells": 0,
            "externalReferenceFormulaCells": 0,
            "calculationEligibleFormulaCells": 0,
            "sameSheetReferenceFormulaCells": 0,
            "definedNameFormulaCells": 0,
            "definedNameIds": set(),
            "formulaTypes": collections.Counter(),
            "familyIds": set(),
            "functions": collections.Counter(),
            "referencedSheets": collections.Counter(),
            "externalReferencedSheets": collections.Counter(),
        }

    formula_cells = 0
    cached_formula_cells = 0
    external_formula_cells = 0
    same_sheet_formula_cells = 0
    defined_name_formula_cells = 0
    unmapped_formula_cells = 0
    shared_master_by_index: dict[str, str] = {}

    with archive.open(str(sheet["path"])) as source:
        context = etree.iterparse(source, events=("end",), tag=CELL_TAG, recover=True)
        for _, cell in context:
            reference = cell.get("r", "")
            cell_match = CELL_REF_RE.match(reference)
            formula = cell.find(FORMULA_TAG)
            if cell_match is None or formula is None:
                cell.clear()
                continue

            col = column_number(cell_match.group(1))
            row = int(cell_match.group(2))
            formula_type = formula.get("t", "normal")
            formula_text = formula.text or ""
            shared_index = formula.get("si")
            if formula_type == "shared" and shared_index is not None:
                family_id = shared_master_by_index.get(shared_index)
                if formula_text:
                    family_id = "ff-" + formula_signature(formula_text)
                    shared_master_by_index[shared_index] = family_id
                elif family_id is None:
                    family_id = "shared-" + hashlib.sha256(
                        f"{sheet['name']}|{shared_index}".encode("utf-8")
                    ).hexdigest()[:16]
            else:
                signature_source = formula_text or f"{formula_type}|NO_TEXT|{reference}"
                family_id = "ff-" + formula_signature(signature_source)

            functions = formula_functions(formula_text)
            referenced_sheets, external_sheets, has_external = sheet_references(formula_text)
            same_sheet = unqualified_cell_reference(formula_text) or str(sheet["name"]) in referenced_sheets
            defined_name_ids = formula_defined_names(formula_text, defined_names)
            cached = cell.find(VALUE_TAG) is not None

            family = family_map.setdefault(
                family_id,
                FamilyStats(family_id, formula_type, reference),
            )
            family.count += 1
            family.cached_count += int(cached)
            family.external_count += int(has_external)
            family.same_sheet_count += int(same_sheet)
            family.functions.update(functions)
            family.volatile_functions.update(set(functions) & VOLATILE_FUNCTIONS)
            family.extension_function_candidates.update(
                function
                for function in functions
                if function.startswith(EXTENSION_FUNCTION_PREFIXES)
            )
            family.custom_function_candidates.update(
                function
                for function in functions
                if function not in KNOWN_FUNCTIONS
                and not function.startswith(EXTENSION_FUNCTION_PREFIXES)
            )
            family.referenced_sheets.update(referenced_sheets)
            family.external_referenced_sheets.update(external_sheets)
            family.defined_name_ids.update(defined_name_ids)

            formula_cells += 1
            cached_formula_cells += int(cached)
            external_formula_cells += int(has_external)
            same_sheet_formula_cells += int(same_sheet)
            defined_name_formula_cells += int(bool(defined_name_ids))
            formula_types[formula_type] += 1
            function_counts.update(functions)
            dependencies.update(
                dependency
                for dependency in referenced_sheets
                if dependency != str(sheet["name"])
            )
            external_dependencies.update(external_sheets)

            table = matching_table(logical_tables, col, row)
            if table is None:
                unmapped_formula_cells += 1
            else:
                stats = table_stats[table.key]
                family.logical_table_keys.add(table.key)
                stats["formulaCells"] += 1
                stats["cachedFormulaCells"] += int(cached)
                stats["externalReferenceFormulaCells"] += int(has_external)
                stats["calculationEligibleFormulaCells"] += int(not has_external)
                stats["sameSheetReferenceFormulaCells"] += int(same_sheet)
                stats["definedNameFormulaCells"] += int(bool(defined_name_ids))
                stats["definedNameIds"].update(defined_name_ids)
                stats["formulaTypes"][formula_type] += 1
                stats["familyIds"].add(family_id)
                stats["functions"].update(functions)
                stats["referencedSheets"].update(
                    dependency
                    for dependency in referenced_sheets
                    if dependency != str(sheet["name"])
                )
                stats["externalReferencedSheets"].update(external_sheets)

            cell.clear()
            parent = cell.getparent()
            while parent is not None and parent.getprevious() is not None:
                del parent.getparent()[0]

    package_counts = worksheet_package_counts(archive, str(sheet["path"]))
    family_values = sorted(family_map.values(), key=lambda item: (-item.count, item.family_id))
    result = {
        "order": sheet["order"],
        "name": sheet["name"],
        "state": sheet["state"],
        "category": category,
        "formulaCells": formula_cells,
        "cachedFormulaCells": cached_formula_cells,
        "formulaTypes": dict(formula_types),
        "formulaFamilyCount": len(family_values),
        "externalReferenceFormulaCells": external_formula_cells,
        "calculationEligibleFormulaCells": formula_cells - external_formula_cells,
        "sameSheetReferenceFormulaCells": same_sheet_formula_cells,
        "definedNameFormulaCells": defined_name_formula_cells,
        "unmappedFormulaCells": unmapped_formula_cells,
        "functions": dict(function_counts.most_common()),
        "volatileFunctions": sorted(set(function_counts) & VOLATILE_FUNCTIONS),
        "extensionFunctionCandidates": sorted(
            function
            for function in function_counts
            if function.startswith(EXTENSION_FUNCTION_PREFIXES)
        ),
        "customFunctionCandidates": sorted(
            function
            for function in function_counts
            if function not in KNOWN_FUNCTIONS
            and not function.startswith(EXTENSION_FUNCTION_PREFIXES)
        ),
        "dependencies": dict(dependencies.most_common()),
        "externalReferencedSheets": dict(external_dependencies.most_common()),
        **package_counts,
    }
    if include_families:
        result["families"] = [family.as_dict() for family in family_values]

    normalized_tables: dict[str, dict[str, object]] = {}
    for key, stats in table_stats.items():
        normalized_tables[key] = {
            **stats,
            "formulaTypes": dict(stats["formulaTypes"]),
            "formulaFamilyCount": len(stats["familyIds"]),
            "familyIds": sorted(stats["familyIds"]),
            "definedNameIds": sorted(stats["definedNameIds"]),
            "functions": dict(stats["functions"].most_common()),
            "extensionFunctionCandidates": sorted(
                function
                for function in stats["functions"]
                if function.startswith(EXTENSION_FUNCTION_PREFIXES)
            ),
            "customFunctionCandidates": sorted(
                function
                for function in stats["functions"]
                if function not in KNOWN_FUNCTIONS
                and not function.startswith(EXTENSION_FUNCTION_PREFIXES)
            ),
            "referencedSheets": dict(stats["referencedSheets"].most_common()),
            "externalReferencedSheets": dict(
                stats["externalReferencedSheets"].most_common()
            ),
        }
    return result, normalized_tables


def inspect_workbook(
    path: Path,
    role: str,
    classifications: dict[str, dict[str, str]],
    logical_tables: list[LogicalTable],
    include_families: bool,
) -> dict[str, object]:
    with zipfile.ZipFile(path) as archive:
        metadata, defined_names = workbook_metadata(archive)
        sheet_results: list[dict[str, object]] = []
        table_results: dict[str, dict[str, object]] = {}
        role_tables = [table for table in logical_tables if table.role == role]
        tables_by_sheet: dict[str, list[LogicalTable]] = collections.defaultdict(list)
        for table in role_tables:
            tables_by_sheet[table.sheet].append(table)

        for sheet in workbook_sheets(archive):
            name = str(sheet["name"])
            result, tables = inspect_sheet(
                archive,
                sheet,
                classifications.get(role, {}).get(name, "미분류"),
                sorted(tables_by_sheet.get(name, []), key=lambda item: item.area),
                defined_names,
                include_families,
            )
            sheet_results.append(result)
            table_results.update(tables)

    reverse_dependencies: dict[str, collections.Counter[str]] = collections.defaultdict(collections.Counter)
    for target in sheet_results:
        for source, count in target["dependencies"].items():
            reverse_dependencies[source][str(target["name"])] += count

    for sheet in sheet_results:
        downstream = sorted(reverse_dependencies.get(str(sheet["name"]), {}))
        risk, oracle = risk_and_oracle(
            str(sheet["category"]),
            int(sheet["formulaCells"]),
            int(sheet["externalReferenceFormulaCells"]),
            downstream,
        )
        sheet["downstreamTargets"] = downstream
        sheet["risk"] = risk
        sheet["oracle"] = oracle
        sheet["calculationPolicy"] = calculation_policy(
            int(sheet["formulaCells"]), int(sheet["externalReferenceFormulaCells"])
        )
        for family in sheet.get("families", []):
            family_risk, family_oracle = family_risk_and_oracle(
                str(sheet["category"]),
                int(family["cellCount"]),
                int(family["externalReferenceCellCount"]),
                set(family["extensionFunctionCandidates"]),
                set(family["customFunctionCandidates"]),
            )
            family["sheet"] = sheet["name"]
            family["downstreamTargets"] = downstream
            family["risk"] = family_risk
            family["oracle"] = family_oracle
            family["calculationPolicy"] = calculation_policy(
                int(family["cellCount"]), int(family["externalReferenceCellCount"])
            )

    for table in table_results.values():
        source_sheet = str(table["sheet"])
        downstream = sorted(reverse_dependencies.get(source_sheet, {}))
        category = classifications.get(role, {}).get(source_sheet, "미분류")
        source_sheet_result = next(
            sheet for sheet in sheet_results if str(sheet["name"]) == source_sheet
        )
        risk, oracle = risk_and_oracle(
            category,
            int(source_sheet_result["formulaCells"]),
            int(table["externalReferenceFormulaCells"]),
            downstream,
        )
        table["downstreamTargets"] = downstream
        table["risk"] = risk
        table["oracle"] = oracle if table["editableSource"] else "PRESERVATION_ONLY"
        table["calculationPolicy"] = calculation_policy(
            int(table["formulaCells"]), int(table["externalReferenceFormulaCells"])
        )

    return {
        "role": role,
        "file": path.name,
        "sha256": sha256_path(path),
        "sheetCount": len(sheet_results),
        "classificationCounts": dict(
            collections.Counter(str(sheet["category"]) for sheet in sheet_results)
        ),
        **metadata,
        "sheetFormulaCells": sum(int(sheet["formulaCells"]) for sheet in sheet_results),
        "cachedFormulaCells": sum(int(sheet["cachedFormulaCells"]) for sheet in sheet_results),
        "externalReferenceFormulaCells": sum(
            int(sheet["externalReferenceFormulaCells"]) for sheet in sheet_results
        ),
        "calculationEligibleFormulaCells": sum(
            int(sheet["calculationEligibleFormulaCells"]) for sheet in sheet_results
        ),
        "formulaFamilyCount": sum(int(sheet["formulaFamilyCount"]) for sheet in sheet_results),
        "sheets": sheet_results,
        "logicalTables": sorted(
            table_results.values(), key=lambda item: (str(item["sheet"]), str(item["tableKey"]))
        ),
    }


def build_report(
    workbooks: list[Path],
    catalog: Path,
    include_families: bool,
) -> dict[str, object]:
    classifications, logical_tables = parse_catalog(catalog)
    results = []
    for path in workbooks:
        role = infer_workbook_role(path)
        if role is None:
            raise ValueError(f"Cannot infer workbook role from file name: {path.name}")
        results.append(
            inspect_workbook(path, role, classifications, logical_tables, include_families)
        )
    return {
        "safety": {
            "rawCellValuesEmitted": False,
            "rawFormulaTextEmitted": False,
            "formulaFamiliesRepresentedByHash": True,
        },
        "calculationPolicy": {
            "externalWorkbookReferences": "PRESERVE_EXTERNAL_ONLY",
            "externalSourceFiles": "NOT_REQUIRED_DO_NOT_REQUEST",
            "externalFormulaAndCachedValue": "KEEP_SOURCE_SNAPSHOT",
            "externalLinksAndConnections": "KEEP_WITHOUT_REFRESH",
        },
        "catalog": str(catalog),
        "workbooks": results,
        "totals": {
            "workbooks": len(results),
            "sheets": sum(int(workbook["sheetCount"]) for workbook in results),
            "logicalTables": sum(len(workbook["logicalTables"]) for workbook in results),
            "formulaCells": sum(int(workbook["sheetFormulaCells"]) for workbook in results),
            "cachedFormulaCells": sum(int(workbook["cachedFormulaCells"]) for workbook in results),
            "externalReferenceFormulaCells": sum(
                int(workbook["externalReferenceFormulaCells"]) for workbook in results
            ),
            "calculationEligibleFormulaCells": sum(
                int(workbook["calculationEligibleFormulaCells"]) for workbook in results
            ),
            "formulaFamilies": sum(int(workbook["formulaFamilyCount"]) for workbook in results),
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("workbooks", nargs="+", type=Path)
    parser.add_argument(
        "--catalog",
        type=Path,
        default=Path("docs/references/mi-model-input-catalog-inventory.md"),
    )
    parser.add_argument(
        "--include-families",
        action="store_true",
        help="include safe per-family hashes and metadata in JSON output",
    )
    parser.add_argument("--output", type=Path, help="write JSON to this path instead of stdout")
    args = parser.parse_args()

    report = build_report(args.workbooks, args.catalog, args.include_families)
    output = json.dumps(report, ensure_ascii=False, indent=2)
    if args.output:
        args.output.write_text(output + "\n", encoding="utf-8")
    else:
        print(output)


if __name__ == "__main__":
    main()
