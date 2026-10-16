#!/usr/bin/env python3
"""Remove approved development sheets without rewriting business XLSM parts.

This is an offline evidence tool, not an API writer or calculation engine.
Inputs contain paths and expected SHA-256 only; outputs never contain cell data.
XLSM/VBA/pivot/link byte preservation needs direct OPC/ZIP editing rather than
the spreadsheet library's XLSX import/export round trip.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import posixpath
import shutil
import zipfile
from pathlib import Path

from lxml import etree as ET

M = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
V = "http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"
DEV = {"SUMMARY", "플랫폼 입력 안내"}
NS = {"m": M}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(stream):
    h = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
        h.update(chunk)
    return h.hexdigest()


def file_hash(path):
    with path.open("rb") as f:
        return digest(f)


def part_hash(z, name):
    with z.open(name) as f:
        return digest(f)


def rel_part(owner):
    return posixpath.join(posixpath.dirname(owner), "_rels", posixpath.basename(owner) + ".rels") if owner else "_rels/.rels"


def target(owner, value):
    result = posixpath.normpath(posixpath.join(posixpath.dirname(owner), value)) if not value.startswith("/") else value[1:]
    require(not result.startswith("../"), "relationship escapes package")
    return result


def graph(z):
    edges = {}
    for name in z.namelist():
        if not name.endswith(".rels"):
            continue
        owner = "" if name == "_rels/.rels" else name.replace("/_rels/", "/")[:-5]
        edges[owner] = [target(owner, r.get("Target")) for r in ET.fromstring(z.read(name)) if r.get("TargetMode") != "External"]
    return edges


def reachable(edges, start):
    seen, todo = set(), list(start)
    while todo:
        name = todo.pop()
        if name not in seen:
            seen.add(name)
            todo.extend(edges.get(name, []))
    return seen


def xml(root):
    return ET.tostring(root, encoding="UTF-8", xml_declaration=True, standalone=True)


def references_dev(text):
    return any(f"'{name}'!" in text or f"{name}!" in text for name in DEV)


def sheet_stats(z, part):
    cells = formulas = dev_refs = 0
    with z.open(part) as f:
        for _, node in ET.iterparse(f, events=("end",), resolve_entities=False, no_network=True):
            tag = ET.QName(node).localname
            if tag == "c":
                cells += 1
            if tag == "f":
                formulas += 1
            if tag in {"f", "formula", "formula1", "formula2"} and references_dev(node.text or ""):
                dev_refs += 1
            if tag == "row":
                node.clear()
                while node.getprevious() is not None:
                    del node.getparent()[0]
    return {"cells": cells, "formulas": formulas, "developmentReferences": dev_refs}


def normalize(source, output):
    with zipfile.ZipFile(source) as z:
        require(len(set(z.namelist())) == len(z.namelist()), "duplicate ZIP entries")
        require(not any(n.startswith("_xmlsignatures/") for n in z.namelist()), "signed package needs explicit signature handling")
        workbook = ET.fromstring(z.read("xl/workbook.xml"))
        sheets = workbook.find("m:sheets", NS)
        old_sheets = list(sheets)
        removed = [s for s in old_sheets if s.get("name") in DEV]
        require({s.get("name") for s in removed} == DEV, "expected both development sheets")
        kept = [s for s in old_sheets if s not in removed]
        indexes = {old_sheets.index(s): i for i, s in enumerate(kept)}
        wb_rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        by_id = {r.get("Id"): target("xl/workbook.xml", r.get("Target")) for r in wb_rels if r.get("TargetMode") != "External"}
        removed_ids = {s.get(f"{{{R}}}id") for s in removed}
        removed_parts = {by_id[r] for r in removed_ids}
        for part in removed_parts:
            require(sheet_stats(z, part)["formulas"] == 0, "development sheet has formulas; calcChain handling required")
        stats = []
        for s in kept:
            part = by_id[s.get(f"{{{R}}}id")]
            data = sheet_stats(z, part)
            require(data["developmentReferences"] == 0, "business sheet references a development sheet")
            stats.append({"name": s.get("name"), "sheetId": s.get("sheetId"), "state": s.get("state", "visible"), "part": part, "sha256": part_hash(z, part), **data})
        edges = graph(z)
        after_edges = {**edges, "xl/workbook.xml": [p for p in edges["xl/workbook.xml"] if p not in removed_parts]}
        deleted = reachable(edges, removed_parts) - reachable(after_edges, [""])
        require(removed_parts <= deleted, "development sheet shared by retained package")
        deleted |= {rel_part(p) for p in list(deleted) if rel_part(p) in z.namelist()}
        for s in removed:
            sheets.remove(s)
        for r in list(wb_rels):
            if r.get("Id") in removed_ids:
                wb_rels.remove(r)
        names_removed = names_shifted = 0
        names = workbook.find("m:definedNames", NS)
        if names is not None:
            for n in list(names):
                scope = n.get("localSheetId")
                if scope is not None and int(scope) not in indexes:
                    names.remove(n)
                    names_removed += 1
                else:
                    require(not references_dev(n.text or ""), "retained defined name references removed sheet")
                    if scope is not None:
                        new_scope = str(indexes[int(scope)])
                        names_shifted += new_scope != scope
                        n.set("localSheetId", new_scope)
        for view in workbook.findall("m:bookViews/m:workbookView", NS):
            for attr in ["activeTab", "firstSheet"]:
                if attr in view.attrib:
                    view.set(attr, str(indexes.get(int(view.get(attr)), 0)))
        content = ET.fromstring(z.read("[Content_Types].xml"))
        for n in list(content):
            if n.get("PartName", "").lstrip("/") in deleted:
                content.remove(n)
        changed = {"xl/workbook.xml": xml(workbook), "xl/_rels/workbook.xml.rels": xml(wb_rels), "[Content_Types].xml": xml(content)}
        if "docProps/app.xml" in z.namelist():
            app = ET.fromstring(z.read("docProps/app.xml"))
            titles = app.find("{*}TitlesOfParts/{*}vector")
            headings = app.find("{*}HeadingPairs/{*}vector")
            if titles is not None:
                old_names = [s.get("name") for s in old_sheets]
                require([n.text for n in list(titles)[:len(old_names)]] == old_names, "unexpected app sheet-title ordering")
                for i in reversed(range(len(old_sheets))):
                    if i not in indexes:
                        titles.remove(titles[i])
                titles.set("size", str(len(titles)))
                require(headings is not None and len(headings) >= 2, "missing app worksheet heading count")
                count = headings[1].find(f"{{{V}}}i4")
                require(count is not None and int(count.text) == len(old_sheets), "unexpected app worksheet count")
                count.text = str(len(kept))
                changed["docProps/app.xml"] = xml(app)
        with output.open("xb") as f, zipfile.ZipFile(f, "w") as result:
            for item in z.infolist():
                if item.filename in deleted:
                    continue
                # ZipFile writes update offsets on ZipInfo; don't mutate the
                # source archive's index while verifying its retained parts.
                output_item = copy.copy(item)
                if item.filename in changed:
                    result.writestr(output_item, changed[item.filename])
                else:
                    with z.open(item) as src, result.open(output_item, "w") as dst:
                        shutil.copyfileobj(src, dst, 1024 * 1024)
        with zipfile.ZipFile(output) as result:
            require(result.testzip() is None, "ZIP CRC mismatch")
            require(set(result.namelist()) == set(z.namelist()) - deleted, "unexpected package part changes")
            unchanged = set(result.namelist()) - set(changed)
            for part in unchanged:
                require(part_hash(z, part) == part_hash(result, part), "retained package bytes changed: " + part)
            missing_before = {(o, t) for o, targets in edges.items() for t in targets if t not in z.namelist()}
            missing_after = {(o, t) for o, targets in graph(result).items() for t in targets if t not in result.namelist()}
            require(missing_after <= missing_before, "new dangling package relationships")
            retained_names = list(names) if names is not None else []
            require(all(0 <= int(n.get("localSheetId")) < len(kept) for n in retained_names if n.get("localSheetId") is not None), "invalid defined-name scope")
        return {"removedSheets": [s.get("name") for s in removed], "sheetCount": len(kept), "sheets": stats,
                "removedParts": sorted(deleted), "changedMetadataParts": sorted(changed), "preservedPartCount": len(unchanged),
                "definedNamesRemoved": names_removed, "definedNameScopesShifted": names_shifted,
                "verification": {"zipCrc": "pass", "retainedPartsByteIdentical": "pass", "newDanglingRelationships": 0,
                                 "businessDevelopmentReferences": 0, "nativeExcelOpen": "not_run", "recalculation": "not_run"}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True, help="JSON array of role/path/sha256")
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    args = parser.parse_args()
    inputs = json.loads(args.inputs.read_text())
    require(len(inputs) == 4 and {i["role"] for i in inputs} == {"DEMAND", "SUPPLY", "TRADE", "OUTPUT"}, "exactly four input roles required")
    require(not args.output_dir.exists() and not args.manifest.exists(), "output/manifest exists; refusing overwrite")
    for i in inputs:
        require(file_hash(Path(i["path"])) == i["sha256"], "source SHA-256 mismatch for " + i["role"])
    args.output_dir.mkdir(parents=True)
    source_dir = args.output_dir / "source"
    source_dir.mkdir()
    entries = []
    for i in inputs:
        original = Path(i["path"])
        snapshot = source_dir / (i["role"] + "-source.xlsm")
        with original.open("rb") as src, snapshot.open("xb") as dst:
            shutil.copyfileobj(src, dst)
        snapshot.chmod(0o400)
        output = args.output_dir / (i["role"] + "-normalized.xlsm")
        info = normalize(snapshot, output)
        require(file_hash(snapshot) == i["sha256"] == file_hash(original), "source changed during normalization")
        output.chmod(0o400)
        entries.append({"role": i["role"], "sourcePath": str(original), "sourceSnapshotPath": str(snapshot.resolve()),
                        "sourceSha256": i["sha256"], "normalizedPath": str(output.resolve()), "normalizedSha256": file_hash(output), **info})
        print(i["role"], info["sheetCount"], "sheets; package preservation PASS", flush=True)
    args.manifest.write_text(json.dumps({"documentControl": {"status": "Complete", "canonicalFor": "Reference only: normalized baseline identity and package evidence", "owner": "BE", "lastSynced": "2026-10-07"}, "baselineId": "2026-10-07-delivery-261006-v8-v6", "files": entries}, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
