#!/usr/bin/env python3
"""Read-only Excel function census; never evaluate or save a workbook.

Use a fixed baseline manifest and the project's actual POI classpath. The JSON
contains counts, function names and coordinates, never formulas or cell values.
Calculation permissions are intentionally not inferred by this scanner.
"""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import zipfile

from lxml import etree


NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
CELL = f"{{{NS}}}c"
FORMULA = f"{{{NS}}}f"
VALUE = f"{{{NS}}}v"
FUNCTION_RE = re.compile(r"(?<![A-Z0-9_.\\])([A-Z_][A-Z0-9_.]*)\s*\(", re.I)
OTHER_FORMULAS = {"formula", "formula1", "formula2", "definedName",
                  "calculatedColumnFormula", "totalsRowFormula", "f"}
JAVA_REGISTRY = """
import java.util.TreeSet;
import org.apache.poi.Version;
import org.apache.poi.ss.formula.WorkbookEvaluator;

public final class ModelInputPoiFunctionRegistry {
    public static void main(String[] args) {
        System.out.println("VERSION\\t" + Version.getVersion());
        for (String name : new TreeSet<>(WorkbookEvaluator.getSupportedFunctionNames())) {
            System.out.println("SUPPORTED\\t" + name);
        }
        for (String name : new TreeSet<>(WorkbookEvaluator.getNotSupportedFunctionNames())) {
            System.out.println("UNSUPPORTED\\t" + name);
        }
    }
}
"""


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def mask_literals(text: str) -> str:
    """Mask quoted strings/sheet names and structured-reference labels."""
    result = list(text)
    index = 0
    while index < len(text):
        start = index
        if text[index] in {'"', "'"}:
            quote = text[index]
            index += 1
            while index < len(text):
                if text[index] == quote:
                    index += 1
                    if index < len(text) and text[index] == quote:
                        index += 1
                        continue
                    break
                index += 1
        elif text[index] == "[":
            depth = 1
            index += 1
            while index < len(text) and depth:
                depth += (text[index] == "[") - (text[index] == "]")
                index += 1
        else:
            index += 1
            continue
        result[start:index] = " " * (index - start)
    return "".join(result)


def functions(text: str) -> Counter[str]:
    return Counter(name.upper() for name in FUNCTION_RE.findall(mask_literals(text)))


def release(element: etree._Element) -> None:
    element.clear()
    while element.getprevious() is not None:
        del element.getparent()[0]


def shared_masters(archive: zipfile.ZipFile, part: str) -> dict[str, str]:
    result = {}
    with archive.open(part) as source:
        for _, cell in etree.iterparse(source, events=("end",), tag=CELL,
                                       resolve_entities=False, no_network=True):
            formula = cell.find(FORMULA)
            if formula is not None and formula.get("t") == "shared" and formula.text:
                index = formula.get("si")
                if index is None or index in result:
                    raise ValueError("Missing or duplicate shared master index")
                result[index] = formula.text
            release(cell)
    return result


def formula_kind(element: etree._Element, part: str) -> tuple[str, str]:
    ancestors = [etree.QName(parent).localname for parent in element.iterancestors()]
    if "cfRule" in ancestors:
        kind = "conditionalFormatting"
        owner = next((p for p in element.iterancestors()
                      if etree.QName(p).localname == "conditionalFormatting"), None)
        location = owner.get("sqref", "extension-rule") if owner is not None else "rule"
    elif "dataValidation" in ancestors:
        kind = "dataValidation"
        owner = next(p for p in element.iterancestors()
                     if etree.QName(p).localname == "dataValidation")
        location = owner.get("sqref", "extension-rule")
    else:
        kind = etree.QName(element).localname
        location = ("name-" + hashlib.sha256(element.get("name", "").encode()).hexdigest()[:12]
                    if kind == "definedName" else part)
    return kind, location


def registry(classpath: str) -> dict:
    jars = [Path(path) for path in classpath.split(":")]
    if not jars or any(not path.is_file() for path in jars):
        raise ValueError("POI classpath must contain existing local jars")
    with tempfile.TemporaryDirectory(prefix="model-input-poi-registry-") as directory:
        java_file = Path(directory) / "ModelInputPoiFunctionRegistry.java"
        java_file.write_text(JAVA_REGISTRY)
        subprocess.run(["javac", "-cp", classpath, "-d", directory, str(java_file)], check=True)
        process = subprocess.run(["java", "-Xmx128m", "-cp", directory + ":" + classpath,
                                  "ModelInputPoiFunctionRegistry"], check=True,
                                 capture_output=True, text=True, timeout=60)
    output = defaultdict(list)
    messages = process.stderr.splitlines()
    for line in process.stdout.splitlines():
        if not line.startswith(("VERSION\t", "SUPPORTED\t", "UNSUPPORTED\t")):
            messages.append(line)
            continue
        kind, name = line.split("\t", 1)
        output[kind].append(name)
    version = output["VERSION"][0]
    if version != "5.4.0":
        raise ValueError("Expected project POI 5.4.0")
    return {"version": version, "method": "WorkbookEvaluator runtime registry; no evaluation",
            "supported": output["SUPPORTED"], "unsupported": output["UNSUPPORTED"],
            "jarHashes": {path.name: sha256(path) for path in jars},
            "runtimeMessages": messages}


def poi_status(name: str, poi: dict) -> dict:
    canonical = re.sub(r"^(?:_XLFN\.|_XLUDF\.|_XLL\.|_XLWS\.)+", "", name)
    if name in poi["supported"]:
        status = "REGISTERED_SUPPORTED"
    elif name in poi["unsupported"]:
        status = "REGISTERED_UNSUPPORTED"
    elif canonical != name and canonical in poi["supported"]:
        status = "PREFIX_REQUIRES_PARSER_VALIDATION"
    else:
        status = "NO_DEFAULT_REGISTRATION"
    return {"status": status, "canonicalName": canonical,
            "canonicalRegistered": canonical in poi["supported"],
            "formulaExecutionVerified": False}


def scan(manifest: dict, poi: dict) -> dict:
    # Reuse source part resolution; never consume the older catalog's policies.
    spec = importlib.util.spec_from_file_location(
        "model_input_formula_inventory", Path(__file__).with_name("inventory-model-input-formulas.py"))
    inventory = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = inventory
    spec.loader.exec_module(inventory)
    entries = manifest["files"]
    if len(entries) != 4 or {entry["role"] for entry in entries} != {"DEMAND", "SUPPLY", "TRADE", "OUTPUT"}:
        raise ValueError("Baseline must contain exactly four roles")
    for entry in entries:
        if sha256(Path(entry["normalizedPath"])) != entry["normalizedSha256"]:
            raise ValueError("Source hash differs from baseline manifest")
    contexts = {}
    rejected = Counter()
    workbooks = []
    extra_counts = Counter()
    total_cells = 0

    def record(text: str, role: str, sheet: str, kind: str, location: str) -> None:
        found = functions(text)
        rejected.update(Counter(name.upper() for name in FUNCTION_RE.findall(text)) - found)
        for name, calls in found.items():
            key = (name, role, sheet, kind)
            item = contexts.setdefault(key, {"expressionCount": 0, "callCount": 0, "samples": []})
            item["expressionCount"] += 1
            item["callCount"] += calls
            if len(item["samples"]) < 5 and location not in item["samples"]:
                item["samples"].append(location)

    for entry in entries:
        role = entry["role"]
        path = Path(entry["normalizedPath"])
        sheets_out = []
        with zipfile.ZipFile(path) as archive:
            sheets = inventory.workbook_sheets(archive)
            if len(sheets) != entry["sheetCount"]:
                raise ValueError("Sheet count differs from baseline manifest")
            for sheet in sheets:
                part = sheet["path"]
                masters = shared_masters(archive, part)
                counts = Counter({key: 0 for key in (
                    "formulaCells", "cachedFormulaCells", "nonemptyCachedFormulaCells",
                    "errorCachedFormulaCells", "noFunctionFormulaCells",
                    "sharedFollowerCells", "externalReferenceFormulaCells")})
                fn_counts = Counter()
                with archive.open(part) as source:
                    tags = (CELL,) + tuple("{*}" + name for name in OTHER_FORMULAS - {"f"}) + ("{*}f",)
                    for _, element in etree.iterparse(source, events=("end",), tag=tags,
                                                      resolve_entities=False, no_network=True):
                        local = etree.QName(element).localname
                        if element.tag == FORMULA and element.getparent().tag == CELL:
                            # Process only at cell end, where the cached value is also available.
                            continue
                        if element.tag == CELL:
                            formula = element.find(FORMULA)
                            if formula is not None:
                                text = formula.text or ""
                                if formula.get("t") == "shared" and not text:
                                    text = masters.get(formula.get("si"), "")
                                    if not text:
                                        raise ValueError("Unresolved shared formula follower")
                                    counts["sharedFollowerCells"] += 1
                                counts["formulaCells"] += 1
                                counts["type:" + formula.get("t", "normal")] += 1
                                cached = element.find(VALUE)
                                counts["cachedFormulaCells"] += cached is not None
                                counts["nonemptyCachedFormulaCells"] += cached is not None and cached.text is not None
                                counts["errorCachedFormulaCells"] += element.get("t") == "e"
                                counts["noFunctionFormulaCells"] += not bool(functions(text))
                                counts["externalReferenceFormulaCells"] += bool(inventory.EXTERNAL_REF_RE.search(text))
                                fn_counts.update(functions(text))
                                record(text, role, sheet["name"], "cell", element.get("r", ""))
                            release(element)
                        elif local in OTHER_FORMULAS:
                            kind, location = formula_kind(element, part)
                            extra_counts[kind] += 1
                            record(element.text or "", role, sheet["name"], kind, location)
                expected = next(item["formulas"] for item in entry["sheets"] if item["name"] == sheet["name"])
                if counts["formulaCells"] != expected:
                    raise ValueError("Formula cell count differs from baseline manifest")
                sheets_out.append({"name": sheet["name"], "state": sheet["state"],
                                   **dict(counts), "functionCalls": dict(sorted(fn_counts.items())),
                                   "sharedMasterCount": len(masters)})
                total_cells += counts["formulaCells"]
            worksheet_parts = {sheet["path"] for sheet in sheets}
            for part in archive.namelist():
                if not part.startswith("xl/") or not part.endswith(".xml") or part in worksheet_parts:
                    continue
                root = etree.fromstring(archive.read(part), parser=etree.XMLParser(resolve_entities=False, no_network=True))
                for element in root.iter():
                    if isinstance(element.tag, str) and etree.QName(element).localname in OTHER_FORMULAS:
                        kind, location = formula_kind(element, part)
                        extra_counts[kind] += 1
                        record(element.text or "", role, "package:" + part, kind, location)
        if sha256(path) != entry["normalizedSha256"]:
            raise ValueError("Source hash changed during scan")
        workbooks.append({"role": role, "sha256": entry["normalizedSha256"], "sheetCount": len(sheets_out),
                          "formulaCells": sum(s["formulaCells"] for s in sheets_out), "sheets": sheets_out})

    names = sorted({key[0] for key in contexts})
    findings = []
    for name in names:
        uses = [{"role": role, "sheet": sheet, "kind": kind, **item}
                for (fn, role, sheet, kind), item in sorted(contexts.items()) if fn == name]
        findings.append({"name": name, "poi": poi_status(name, poi),
                         "cellCount": sum(item["expressionCount"] for item in uses if item["kind"] == "cell"),
                         "cellCallCount": sum(item["callCount"] for item in uses if item["kind"] == "cell"),
                         "uses": uses})
    return {"documentControl": {"status": "Complete", "canonicalFor": "Reference only",
                                "date": "2026-10-07", "baselineId": manifest["baselineId"]},
            "scope": {"rawCellValuesEmitted": False, "rawFormulaTextEmitted": False,
                      "policyInference": False, "workbookEvaluation": False,
                      "sharedFollowersIncluded": True, "extraFormulaLocationsIncluded": True},
            "poi": poi, "workbooks": workbooks, "functions": findings,
            "totals": {"workbooks": 4, "sheets": sum(w["sheetCount"] for w in workbooks),
                       "formulaCells": total_cells, "rawFunctionNames": len(names),
                       "canonicalFunctionNames": len({f["poi"]["canonicalName"] for f in findings}),
                       "extraFormulaExpressions": dict(extra_counts),
                       "rejectedLiteralFunctionMatches": dict(sorted(rejected.items()))}}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--poi-classpath", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError("Output already exists; choose a new path")
    manifest = json.loads(args.manifest.read_text())
    report = scan(manifest, registry(args.poi_classpath))
    with args.output.open("x", encoding="utf-8") as output:
        json.dump(report, output, ensure_ascii=False, indent=2)
        output.write("\n")
    print(json.dumps(report["totals"], ensure_ascii=False))


if __name__ == "__main__":
    main()
