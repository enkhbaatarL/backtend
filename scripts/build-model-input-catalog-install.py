#!/usr/bin/env python3
"""Rebuild first-install Catalog SQL from repository-owned definitions; never connects to a DB.

Usage: python3 scripts/build-model-input-catalog-install.py [--check]
The generated SQL owns installation/verification; source SQL owns existing policy decisions.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
SOURCES = [
    'model-input-catalog-structure-data-draft.sql',
    'model-input-upload-validation-testdata-draft.sql',
    'model-input-editor-write-template-version-draft.sql',
    'model-input-png-delete-template-v3-draft.sql',
    'model-input-lng-demand-control-template-v4-draft.sql',
    'model-input-lng-demand-mode-hidden-v5-registration-draft.sql',
]

def compact(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def digest(value):
    return hashlib.sha256(value.encode('utf-8')).hexdigest()


def literal_json(text, variable):
    match = re.search(r'SET @' + variable + r"\s*=\s*'([^']*)';", text)
    assert match, variable
    return json.loads(match[1])


def hex_json(text, variable):
    match = re.search(r'SET @' + variable + r'\s*=\s*CONVERT\(CONCAT\((.*?)\) USING utf8mb4\);', text, re.S)
    assert match, variable
    return json.loads(bytes.fromhex(''.join(re.findall(r'0x([0-9a-fA-F]+)', match[1]))).decode('utf-8'))


def assemble():
    source = {name: (DOCS / name).read_text() for name in SOURCES}
    structure = source[SOURCES[0]]
    definitions = {int(q): json.loads(text) for q, text in re.findall(r"SET @c_q(\d{3})='([^']*)';", structure)}
    start = structure.index('INSERT INTO tmd_mdl_inpttbld')
    insert = structure[start:structure.index(') x JOIN', start)]
    pattern = (
        r"SELECT '(\w+)'(?: AS role)?,'((?:''|[^'])*)'(?: AS sheet)?,'((?:''|[^'])*)'(?: AS name)?,"
        r"(\d+)(?: AS ord)?,(\d+)(?: AS br)?,(\d+)(?: AS er)?,(\d+)(?: AS bc)?,(\d+)(?: AS ec)?,"
        r"(\d+)(?: AS hr)?,(\d+)(?: AS he)?,(\d+)(?: AS dr)?,'(.)'(?: AS ra)?,'(.)'(?: AS ca)?,"
        r"'((?:''|[^'])*)'(?: AS description)?,@c_q(\d{3})"
    )
    rows = []
    for match in re.finditer(pattern, insert):
        row = list(match.groups())
        row[1], row[2], row[13] = (row[i].replace("''", "'") for i in (1, 2, 13))
        row[3:11] = map(int, row[3:11])
        row[14] = copy.deepcopy(definitions[int(row[14])])
        rows.append(row)
    assert len(definitions) == len(rows) == 92
    by_key = {tuple(row[:3]): row for row in rows}
    assert len(by_key) == 92
    upload = source[SOURCES[1]]
    files = [row + ['XLSM'] for row in hex_json(upload, 'u_files')]
    sheets = hex_json(upload, 'u_sheets')
    upload_rows = hex_json(upload, 'u_rows')
    assert len(files) == 4 and len(sheets) == 70 and len(upload_rows) == 92
    assert {tuple(row[:3]) for row in upload_rows} == set(by_key)
    patches = literal_json(source[SOURCES[2]], 'e_patches')
    assert len(patches) == 52
    # Validate the complete current base definition and all geometry before adding upload metadata.
    for patch in patches:
        row = by_key[tuple(patch[key] for key in ('role', 'sheet', 'name'))]
        assert row[14] == patch['baseline'], row[:3]
        assert row[3:13] == [patch['geometry'][k] for k in ('ord', 'br', 'er', 'bc', 'ec', 'hr', 'he', 'dr', 'ra', 'ca')], row[:3]
    for role, sheet, name, old_digest, validation, headers in upload_rows:
        row = by_key[(role, sheet, name)]
        row[14]['uploadValidation'] = json.loads(validation)
        row[14]['headerRows'] = json.loads(headers)
    scopes = []
    for patch in patches:
        key = tuple(patch[k] for k in ('role', 'sheet', 'name'))
        query = by_key[key][14]
        query['fields'] = copy.deepcopy(patch['fields'])
        query['capabilities'] = copy.deepcopy(patch['capabilities'])
        if 'columnCreation' in patch:
            query['columnCreation'] = copy.deepcopy(patch['columnCreation'])
        for scope in patch['scopes']:
            scopes.append(list(key) + [scope[k] for k in ('ord', 'startRow', 'endRow', 'startColumn', 'endColumn')])
    assert len(scopes) == 126
    png = by_key[('TRADE', 'MidstreamCapacityInput', 'PNG Infrastructure')][14]
    assert len(png['fields']) == 11
    # Exactly the v3 JSON_SET paths. Current editor source may already contain this approved effect.
    v3 = source[SOURCES[3]]
    assert "'$.capabilities.DELETE_ROW', 'READY', '$.capabilities.DELETE_COLUMN', 'READY'" in v3
    assert all(f"'$.fields[{i}].policyStatus.DELETE_COLUMN', 'NOT_ALLOWED'" in v3 for i in range(11))
    png['capabilities'].update(DELETE_ROW='READY', DELETE_COLUMN='READY')
    for field in png['fields']:
        field['policyStatus']['DELETE_COLUMN'] = 'NOT_ALLOWED'
    v4 = source[SOURCES[4]]
    mode = literal_json(v4, 'lc_mode_qry')
    annual = literal_json(v4, 'lc_annual_qry')
    v5 = source[SOURCES[5]]
    assert "JSON_SET(b.qry_json,'$.navigationVisible',CAST('false' AS JSON))" in v5
    mode['navigationVisible'] = False
    rows.extend([
        ['DEMAND', 'LNG Demand', 'LNG Demand Mode', 1, 1, 1, 2, 3, 0, 0, 1, 'N', 'N', None, mode],
        ['DEMAND', 'LNG Demand', 'Annual LNG Demand Reference', 2, 3, 109, 2, 14, 3, 3, 4, 'N', 'Y', None, annual],
    ])
    for ordinal, scope_order, r1, r2, c1, c2 in literal_json(v4, 'lc_scopes'):
        name = 'LNG Demand Mode' if ordinal == 1 else 'Annual LNG Demand Reference'
        scopes.append(['DEMAND', 'LNG Demand', name, scope_order, r1, r2, c1, c2])
    assert len(rows) == 94 and len(scopes) == 146
    assert len({tuple(row[:3]) for row in rows}) == 94
    assert len({tuple(row[:4]) for row in scopes}) == 146
    sheet_keys = {tuple(row[:2]) for row in sheets}
    assert all(tuple(row[:2]) in sheet_keys for row in rows)
    assert sum(row[14]['capabilities']['SET'] == 'READY' for row in rows) == 53
    assert sum(row[14].get('navigationVisible', True) is False for row in rows) == 1
    # These historical INCOMPLETE tables must not be promoted by installation.
    assert by_key[('TRADE', 'ShippingCostInput', 'Shipping Route Parameters')][14]['capabilities']['SET'] == 'INCOMPLETE'
    for row in rows:
        assert row[14]['uploadValidation']['headerCells'], row[:3]
        assert row[13] is None or row[13] != '', row[:3]
        row[13] = row[13] if row[13] is not None else ''  # Empty transport text maps back to SQL NULL.
        row[14] = compact(row[14])  # JSON_TABLE reads a string; the complete JSON text is inserted unchanged.
    payload = {'files': files, 'sheets': sheets, 'tables': rows, 'scopes': scopes}
    return payload, source


def data_sql(variable, value):
    data = compact(value).encode('utf-8').hex()
    chunks = [data[i:i + 4000] for i in range(0, len(data), 4000)]
    return f'SET @{variable} = CONVERT(CONCAT(\n' + ',\n'.join('0x' + c for c in chunks) + '\n) USING utf8mb4);\n'


def table_projection():
    names = ['role', 'sheet', 'name', 'ord', 'br', 'er', 'bc', 'ec', 'hr', 'he', 'dr', 'ra', 'ca', 'description', 'qry']
    types = ['VARCHAR(20)', 'VARCHAR(150)', 'VARCHAR(100)'] + ['INT'] * 8 + ['CHAR(1)', 'CHAR(1)', 'VARCHAR(500)', 'LONGTEXT']
    return ',\n '.join(f"{name} {kind} PATH '$[{i}]'" for i, (name, kind) in enumerate(zip(names, types)))


def jt(variable, definition):
    return f"JSON_TABLE(@{variable},'$[*]' COLUMNS(\n {definition})) x"


FILES = "role VARCHAR(20) PATH '$[0]',name VARCHAR(255) PATH '$[1]',ord INT PATH '$[2]',kind VARCHAR(20) PATH '$[3]'"
SHEETS = "role VARCHAR(20) PATH '$[0]',name VARCHAR(150) PATH '$[1]',ord INT PATH '$[2]',state VARCHAR(20) PATH '$[3]'"
SCOPES = "role VARCHAR(20) PATH '$[0]',sheet VARCHAR(150) PATH '$[1]',name VARCHAR(100) PATH '$[2]',ord INT PATH '$[3]',br INT PATH '$[4]',er INT PATH '$[5]',bc INT PATH '$[6]',ec INT PATH '$[7]'"
OWNER = 'JOIN tmd_mdl_inptfiled f USING(inpt_file_id)'
TABLE_OWNER = 'JOIN tmd_mdl_inptshtd s USING(inpt_sht_id) ' + OWNER
SCOPE_OWNER = 'JOIN tmd_mdl_inpttbld b USING(inpt_tbl_id) ' + TABLE_OWNER
NATURAL_TABLE = "BINARY f.mdl_file_cd=BINARY x.role AND BINARY s.sht_nm=BINARY x.sheet AND BINARY b.inpt_tbl_nm=BINARY x.name"


def matches(publication):
    query_match = 'SHA2(b.qry_json,256)=x.qry' if publication else 'BINARY b.qry_json=BINARY x.qry'
    return f"""-- Exact natural-key metadata and full qry bytes (or their SHA-256), not counts alone.
SET @ci_files_count=(SELECT COUNT(*) FROM tmd_mdl_inptfiled WHERE tmplt_id=@ci_new);
SET @ci_files_match=(SELECT COUNT(*) FROM {jt('ci_files', FILES)}
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role
 WHERE BINARY f.file_nm=BINARY x.name AND f.file_ord=x.ord AND BINARY f.file_type_cd=BINARY x.kind
 AND f.file_strt_hash_val IS NULL AND f.del_yn='N');
SET @ci_sheets_count=(SELECT COUNT(*) FROM tmd_mdl_inptshtd s {OWNER} WHERE f.tmplt_id=@ci_new);
SET @ci_sheets_match=(SELECT COUNT(*) FROM {jt('ci_sheets', SHEETS)}
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.name
 WHERE s.sht_ord=x.ord AND BINARY s.sht_stat_cd=BINARY x.state AND s.del_yn='N');
SET @ci_tables_count=(SELECT COUNT(*) FROM tmd_mdl_inpttbld b {TABLE_OWNER} WHERE f.tmplt_id=@ci_new);
SET @ci_tables_match=(SELECT COUNT(*) FROM {jt('ci_tables', table_projection())}
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet
 JOIN tmd_mdl_inpttbld b ON b.inpt_sht_id=s.inpt_sht_id AND BINARY b.inpt_tbl_nm=BINARY x.name
 WHERE b.tbl_ord=x.ord AND b.tbl_bgn_row_no=x.br AND b.tbl_end_row_no=x.er
 AND b.tbl_bgn_col_no=x.bc AND b.tbl_end_col_no=x.ec AND b.hdr_bgn_row_no=x.hr
 AND b.hdr_end_row_no=x.he AND b.data_bgn_row_no=x.dr AND b.row_add_psbl_yn=x.ra
 AND b.col_add_psbl_yn=x.ca AND (BINARY b.inpt_tbl_expn <=> BINARY NULLIF(x.description,''))
 AND {query_match} AND b.del_yn='N');
SET @ci_scopes_count=(SELECT COUNT(*) FROM tmd_mdl_inptmodscopd m {SCOPE_OWNER} WHERE f.tmplt_id=@ci_new);
SET @ci_scopes_match=(SELECT COUNT(*) FROM {jt('ci_scopes', SCOPES)}
 JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role
 JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet
 JOIN tmd_mdl_inpttbld b ON b.inpt_sht_id=s.inpt_sht_id AND BINARY b.inpt_tbl_nm=BINARY x.name
 JOIN tmd_mdl_inptmodscopd m ON m.inpt_tbl_id=b.inpt_tbl_id AND m.mod_scop_ord=x.ord
 WHERE m.bgn_row_no=x.br AND m.end_row_no=x.er AND m.bgn_col_no=x.bc AND m.end_col_no=x.ec AND m.del_yn='N');
SET @ci_verified=IF(@ci_ok=1 AND @ci_new IS NOT NULL
 AND @ci_files_count=4 AND @ci_files_match=4 AND @ci_sheets_count=70 AND @ci_sheets_match=70
 AND @ci_tables_count=94 AND @ci_tables_match=94 AND @ci_scopes_count=146 AND @ci_scopes_match=146,1,0);
SELECT @ci_new AS target_template_id,@ci_files_match AS files_must_be_4,
 @ci_sheets_match AS sheets_must_be_70,@ci_tables_match AS tables_must_be_94,
 @ci_scopes_match AS scopes_must_be_146,@ci_verified AS verified_must_be_1;
"""


def environment_guard():
    return """COALESCE(BINARY @ci_environment IN ('DEVELOPMENT','PRODUCTION'),0)=1
 AND COALESCE(BINARY @@GLOBAL.server_uuid=BINARY @ci_expected_server_uuid,0)=1
 AND COALESCE(BINARY DATABASE()=BINARY @ci_expected_schema,0)=1
 AND @@GLOBAL.read_only=0 AND @@GLOBAL.super_read_only=0 AND @@SESSION.foreign_key_checks=1
 AND @ci_actor IS NOT NULL AND REGEXP_LIKE(@ci_actor,'^[a-zA-Z0-9._@-]{1,50}$','c')"""


def header(publication, payload_digest, source):
    operation = 'ACTIVE 발행' if publication else 'DRAFT 최초 등록'
    text = f"""-- Model Input Catalog 개발계·운영계 공통 {operation} — v5 최종 정의
-- Document Control
-- status: Draft / MANUAL APPLY ONLY / NOT EXECUTED / DEFAULT ROLLBACK
-- canonical for: Schema rollout — 최초 Catalog {operation}/verify/보존 rollback SQL
-- owner: BE / DBA; date / last synced: 2026-10-22
-- depends on: model-input-draft-install-ddl.sql; design-docs/gas/mi-model-input-template-catalog-domain-model-design.md
--   section 3.3 / rules 45,50,58,65,66; feature-specs/mi-fundamental-model-input-provision-feature-spec.md section 3.1B
-- source documents: 아래 해시의 기존 repository SQL. 업무 정책은 원본 정본을 따르며 새 READY 승격 없음.
"""
    for name, contents in source.items():
        text += f'--   {name} sha256={digest(contents)}\n'
    text += f"""-- generated by: python3 scripts/build-model-input-catalog-install.py (--check로 원본 변경/생성물 차이 확인)
-- payload sha256: {payload_digest} (환경별 DB ID/감사값 없는 전체 최종 데이터)
-- supersedes: 위 중간 버전의 신규 환경 등록·발행 순서만 대체. testdb/기존 ACTIVE 전환에는 사용하지 않음.
-- Open Questions: 양 환경의 실제 DB 정의/권한/버전·collation/배포 SHA/정규화 파일/API·storage 검증 미확인.
--
-- 전제: 같은 모델의 이전 등록/ACTIVE가 없는 최초 설치. 기존 버전 교체·DEPRECATED 전환·Case 재연결 없음.
-- 개발계에서 먼저 동일 파일 SHA-256/배포 코드를 검증한 뒤 운영계의 새 master 연결에서 독립 적용한다.
-- 환경별 서버/schema/운영자/백업·권한/입력과 결과는 각각 확인한다. testdb/개발계 ID·이력·파일을 복사하지 않는다.
-- MySQL 8.0.16+ / InnoDB / CHECK·FK=1 / utf8mb4. 관련 catalog writer/접수/worker를 중지하고 새 전용 연결 사용.
-- sql_mode/기본 utf8mb4 collation/시간대/packet을 DDL §1 결과와 대조. 불일치면 적용 중단 후 확인한다.
-- DDL을 섞지 않는다. 첫 SQL 오류/STOP/건수·내용 불일치면 즉시 ROLLBACK 후 중단. --force/자동 commit 금지.
-- 전체 파일의 기본 마지막은 ROLLBACK 예행이다. 실제 저장은 다시 실행한 뒤 최종 VERIFIED 결과를 확인하고
-- 마지막 ROLLBACK 대신 COMMIT을 같은 연결에서 선택한다. ROLLBACK 예행에서 출력된 임시 ID는 재사용하지 않는다.
-- 기본 NULL/0 입력은 쓰기를 차단한다. 검증 변수/내장 데이터/marker를 임의로 바꾸어 통과시키지 않는다.
-- 데이터: 역할4/시트70/표94/범위146. SET READY는53표(기존52+Annual), 전체 편집 가능을 뜻하지 않음.
-- LNG Demand Mode는 내부에 보존/navigationVisible=false, Annual 표가 lngMode control을 참조한다.
-- Shipping/기존 보류·INCOMPLETE/NOT_ALLOWED는 그대로 유지한다. 원본 92표의 업로드 fixed header 검증을 포함한다.
--
SET NAMES utf8mb4;
-- 입력 순서: 적용할 환경의 쓰기 DB에 연결/DB 선택 → 다음 SELECT만 먼저 실행 → 입력부 NULL 수정.
SELECT @@GLOBAL.server_uuid AS server_uuid, DATABASE() AS schema_name;
-- server_uuid는 서버 고유 ID, schema_name은 DB 이름이다. schema_name이 NULL이면 대상 DB를 선택한 뒤 재조회한다.
-- DB 클라이언트의 접속 대상도 확인하여 해당 환경의 쓰기 DB임을 대조한 다음 아래 SET의 NULL을 바꾼다.
-- actor는 작업 이력에 남길 실제 적용자 로그인 ID다. 자동 생성하지 않으며 비밀번호를 넣지 않는다.
-- 작성 예시(주석이며 실제 운영값이 아님):
-- SET @ci_environment='DEVELOPMENT';
-- SET @ci_expected_server_uuid='<위에서 조회하고 대상 환경과 대조한 server_uuid>';
-- SET @ci_expected_schema='midb'; -- schema_name이 midb인 경우에만 사용.
-- SET @ci_actor='release.operator'; -- 실제 적용자 로그인 ID로 교체.
-- 운영계는 environment='PRODUCTION'과 운영계 UUID/schema를 사용한다. 기존 기본값 줄을 수정한 뒤 실행한다.
SET @ci_environment=NULL; -- 'DEVELOPMENT' 또는 'PRODUCTION', 작업 기록 표식이며 실제 환경의 증거는 아님.
SET @ci_expected_server_uuid=NULL; -- DBA가 승인한 대상 쓰기 서버의 UUID. 관측값을 확인 없이 복사하지 않는다.
SET @ci_expected_schema=NULL; -- 위 SELECT의 schema_name, 이번 Catalog를 등록/발행할 DB 이름.
SET @ci_actor=NULL; -- 승인된 감사 로그인 ID, 자격증명 아님. 영문/숫자/._@- 1~50자.
SET @ci_model='DRAFT_C_STRUCTURE',@ci_version=5;
SET @ci_marker='catalog-install-v5-20261022; payload={payload_digest}';
SET @ci_name='2026-10-22 Model Input v5';
SET @ci_ok=0,@ci_verified=0,@ci_new=NULL;
"""
    if publication:
        text += """-- 발행 전 추가 입력: 등록 SQL의 실제 COMMIT 후 새 연결에서 아래 조회로 해당 환경의 tmplt_id를 확인한다.
-- SELECT tmplt_id,tmplt_stat_cd,tmplt_expn FROM tmd_mdl_inpttmpltm
-- WHERE mdl_spt_cd='DRAFT_C_STRUCTURE' AND tmplt_ver_no=5;
-- DRAFT 상태와 이 파일의 marker가 일치하는 행의 tmplt_id를 넣는다. ROLLBACK 예행의 임시 ID는 사용하지 않는다.
SET @ci_expected_template_id=NULL; -- 위 조회에서 확인한 이 환경의 실제 등록 ID(숫자).
SET @ci_reader_ready=0; -- 배포 담당자가 v5 지원 코드 배포를 확인하면 1. 아직 미확인이면 0 유지.
-- v5 지원: navigationVisible, 숨긴 내부 표의 lngMode control, 기존 편집 정책을 지원하는 코드.
"""
    text += """SELECT @ci_environment AS deployment_environment,@@GLOBAL.server_uuid AS actual_server_uuid,
 @ci_expected_server_uuid AS expected_server_uuid,DATABASE() AS actual_schema,@ci_expected_schema AS expected_schema,
 VERSION() AS mysql_version,@@GLOBAL.read_only AS server_read_only,@@GLOBAL.super_read_only AS server_super_read_only,
 @@SESSION.sql_mode AS sql_mode,@@SESSION.foreign_key_checks AS fk_checks,@@max_allowed_packet AS max_allowed_packet,
 @@SESSION.default_collation_for_utf8mb4 AS default_utf8mb4_collation,@@SESSION.time_zone AS session_time_zone;
-- 각 환경의 설치 DDL 결과와 실제 정의를 대조한다. 적용 상태·reader 호환 여부를 이 출력이 증명하지 않는다.
SHOW CREATE TABLE tmd_mdl_inpttmpltm;
SHOW CREATE TABLE tmd_mdl_inptfiled;
SHOW CREATE TABLE tmd_mdl_inptshtd;
SHOW CREATE TABLE tmd_mdl_inpttbld;
SHOW CREATE TABLE tmd_mdl_inptmodscopd;
"""
    return text


def registration(payload, source, payload_digest):
    text = header(False, payload_digest, source)
    text += '\n-- 1. 내장 최종 데이터 (UTF-8 hex). description 빈 문자열은 전송 표식이며 INSERT에서 SQL NULL로 복원한다.\n'
    for key, value in payload.items():
        text += data_sql('ci_' + key, value)
    text += f"""
-- 2. 같은 모델의 등록 범위를 잠근다. 같은 모델이 이미 있으면 재실행/덮어쓰기하지 않는다.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd=@ci_model FOR UPDATE;
SET @ci_existing=(SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@ci_model);
SET @ci_ok=IF({environment_guard()}
 AND @ci_existing=0 AND JSON_VALID(@ci_files) AND JSON_LENGTH(@ci_files)=4
 AND JSON_VALID(@ci_sheets) AND JSON_LENGTH(@ci_sheets)=70
 AND JSON_VALID(@ci_tables) AND JSON_LENGTH(@ci_tables)=94
 AND JSON_VALID(@ci_scopes) AND JSON_LENGTH(@ci_scopes)=146,1,0);
SELECT IF(@ci_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision,@ci_existing AS existing_model_rows_must_be_0;
INSERT INTO tmd_mdl_inpttmpltm(mdl_spt_cd,tmplt_ver_no,tmplt_ver_nm,tmplt_stat_cd,aply_bgn_dtm,tmplt_expn,crt_usr_logn_id,mod_usr_logn_id)
SELECT @ci_model,@ci_version,@ci_name,'DRAFT',NULL,@ci_marker,@ci_actor,@ci_actor FROM DUAL WHERE @ci_ok=1;
-- The pre-existing guard was read separately; the INSERT never subqueries its own target table.
SET @ci_new=(SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE @ci_ok=1 AND mdl_spt_cd=@ci_model
 AND tmplt_ver_no=@ci_version AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL
 AND BINARY tmplt_expn=BINARY @ci_marker AND del_yn='N');
INSERT INTO tmd_mdl_inptfiled(tmplt_id,mdl_file_cd,file_nm,file_ord,file_type_cd,file_strt_hash_val,crt_usr_logn_id,mod_usr_logn_id)
SELECT @ci_new,x.role,x.name,x.ord,x.kind,NULL,@ci_actor,@ci_actor
FROM {jt('ci_files', FILES)} WHERE @ci_ok=1 AND @ci_new IS NOT NULL;
INSERT INTO tmd_mdl_inptshtd(inpt_file_id,sht_nm,sht_ord,sht_stat_cd,crt_usr_logn_id,mod_usr_logn_id)
SELECT f.inpt_file_id,x.name,x.ord,x.state,@ci_actor,@ci_actor FROM {jt('ci_sheets', SHEETS)}
JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role WHERE @ci_ok=1;
INSERT INTO tmd_mdl_inpttbld(inpt_sht_id,inpt_tbl_nm,tbl_ord,tbl_bgn_row_no,tbl_end_row_no,tbl_bgn_col_no,tbl_end_col_no,
 hdr_bgn_row_no,hdr_end_row_no,data_bgn_row_no,row_add_psbl_yn,col_add_psbl_yn,inpt_tbl_expn,qry_json,crt_usr_logn_id,mod_usr_logn_id)
SELECT s.inpt_sht_id,x.name,x.ord,x.br,x.er,x.bc,x.ec,x.hr,x.he,x.dr,x.ra,x.ca,NULLIF(x.description,''),x.qry,@ci_actor,@ci_actor
FROM {jt('ci_tables', table_projection())}
JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role
JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet WHERE @ci_ok=1;
INSERT INTO tmd_mdl_inptmodscopd(inpt_tbl_id,mod_scop_ord,bgn_row_no,end_row_no,bgn_col_no,end_col_no,crt_usr_logn_id,mod_usr_logn_id)
SELECT b.inpt_tbl_id,x.ord,x.br,x.er,x.bc,x.ec,@ci_actor,@ci_actor FROM {jt('ci_scopes', SCOPES)}
JOIN tmd_mdl_inptfiled f ON f.tmplt_id=@ci_new AND BINARY f.mdl_file_cd=BINARY x.role
JOIN tmd_mdl_inptshtd s ON s.inpt_file_id=f.inpt_file_id AND BINARY s.sht_nm=BINARY x.sheet
JOIN tmd_mdl_inpttbld b ON b.inpt_sht_id=s.inpt_sht_id AND BINARY b.inpt_tbl_nm=BINARY x.name WHERE @ci_ok=1;

-- 3. 正確な階層内容を検証 (すべて同一 transaction)。
""".replace('-- 3. 正確な階層内容を検証 (すべて同一 transaction)。', '-- 3. 같은 transaction에서 전체 계층의 정확한 내용을 검증한다.')
    text += matches(False)
    text += """SET @ci_verified=IF(@ci_verified=1 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm
 WHERE tmplt_id=@ci_new AND BINARY mdl_spt_cd=BINARY @ci_model AND tmplt_ver_no=5
 AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL AND BINARY tmplt_expn=BINARY @ci_marker
 AND BINARY tmplt_ver_nm=BINARY @ci_name AND del_yn='N')=1,1,0);
SELECT IF(@ci_verified=1,'VERIFIED_UNPUBLISHED','STOP_AND_ROLLBACK') AS decision,
 @ci_new AS registered_template_id,@ci_marker AS registration_marker;
-- 성공1 + VERIFIED_UNPUBLISHED를 확인한 실제 실행만 마지막 ROLLBACK 대신 COMMIT 선택.
ROLLBACK;
-- COMMIT 후에도 DRAFT다. 새 연결에서 모델/버전/marker/ID를 재확인하고 publication.sql에 정확한 ID를 입력한다.
-- 실패는 transaction 전체 ROLLBACK. COMMIT 이후는 원본/Case/파일을 보존하고 기능 비활성화 후 조사한다.
-- 등록 이력 삭제/중간 버전 재생성/ACTIVE 덮어쓰기/Case 연결은 이 SQL 범위가 아니다.
-- Implementation readiness: ready for manual rehearsal; 실제 MySQL/API 검증은 별도.
"""
    return text


def publication(payload, source, payload_digest):
    text = header(True, payload_digest, source)
    manifest = copy.deepcopy(payload)
    for row in manifest['tables']:
        row[14] = digest(row[14])
    text += '\n-- 1. 기대 manifest: 전체 metadata/수정 범위와 qry 원문의 SHA-256. description 빈 전송값은 SQL NULL을 뜻한다.\n'
    for key, value in manifest.items():
        text += data_sql('ci_' + key, value)
    text += f"""
-- 2. 정확한 DRAFT와 참조를 잠근다. 최초 발행만 허용하고 기존 ACTIVE는 변경하지 않는다.
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd FROM tmd_mdl_inpttmpltm
WHERE mdl_spt_cd=@ci_model FOR UPDATE;
SET @ci_new=(SELECT tmplt_id FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@ci_expected_template_id
 AND BINARY mdl_spt_cd=BINARY @ci_model AND tmplt_ver_no=@ci_version AND tmplt_stat_cd='DRAFT'
 AND aply_bgn_dtm IS NULL AND BINARY tmplt_expn=BINARY @ci_marker AND BINARY tmplt_ver_nm=BINARY @ci_name AND del_yn='N');
SELECT inpt_file_id FROM tmd_mdl_inptfiled WHERE tmplt_id=@ci_new FOR SHARE;
SELECT s.inpt_sht_id FROM tmd_mdl_inptshtd s {OWNER} WHERE f.tmplt_id=@ci_new FOR SHARE;
SELECT b.inpt_tbl_id FROM tmd_mdl_inpttbld b {TABLE_OWNER} WHERE f.tmplt_id=@ci_new FOR SHARE;
SELECT m.mod_scop_id FROM tmd_mdl_inptmodscopd m {SCOPE_OWNER} WHERE f.tmplt_id=@ci_new FOR SHARE;
SELECT case_id FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@ci_new FOR SHARE;
SELECT drft_id FROM tmd_mdl_inptdrftm WHERE tmplt_id=@ci_new FOR SHARE;
SELECT upld_file_id FROM tmd_mdl_inptupldfiled WHERE tmplt_id=@ci_new FOR SHARE;
SELECT base_id FROM tmd_mdl_inptbasem WHERE tmplt_id=@ci_new FOR SHARE;
SET @ci_ok=IF({environment_guard()}
 AND @ci_reader_ready=1 AND @ci_new IS NOT NULL AND @ci_expected_template_id IS NOT NULL
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@ci_model)=1
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_scnrcasetmpltd WHERE tmplt_id=@ci_new)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptdrftm WHERE tmplt_id=@ci_new)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptupldfiled WHERE tmplt_id=@ci_new)
 AND NOT EXISTS(SELECT 1 FROM tmd_mdl_inptbasem WHERE tmplt_id=@ci_new),1,0);
SELECT IF(@ci_ok=1,'PRECHECK_OK','STOP_AND_ROLLBACK') AS decision,@ci_new AS exact_draft_id;
"""
    text += matches(True)
    text += """
-- 3. 전체 manifest 일치 때 root 한 행만 발행. 하위 정의·기존 Case/Draft는 수정하지 않는다.
SET @ci_time=CURRENT_TIMESTAMP(6);
UPDATE tmd_mdl_inpttmpltm SET tmplt_stat_cd='ACTIVE',aply_bgn_dtm=@ci_time,mod_usr_logn_id=@ci_actor,mod_dtm=CURRENT_TIMESTAMP
WHERE @ci_verified=1 AND tmplt_id=@ci_new AND tmplt_stat_cd='DRAFT' AND aply_bgn_dtm IS NULL
 AND BINARY mdl_spt_cd=BINARY @ci_model AND tmplt_ver_no=5 AND BINARY tmplt_expn=BINARY @ci_marker AND del_yn='N';
SET @ci_published=IF(@ci_verified=1 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm
 WHERE tmplt_id=@ci_new AND BINARY mdl_spt_cd=BINARY @ci_model AND tmplt_ver_no=5
 AND tmplt_stat_cd='ACTIVE' AND aply_bgn_dtm=@ci_time AND BINARY tmplt_expn=BINARY @ci_marker
 AND BINARY mod_usr_logn_id=BINARY @ci_actor AND del_yn='N')=1
 AND (SELECT COUNT(*) FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=@ci_model AND tmplt_stat_cd='ACTIVE' AND del_yn='N')=1,1,0);
SELECT @ci_published AS published_must_be_1,IF(@ci_published=1,'VERIFIED_PUBLISHED','STOP_AND_ROLLBACK') AS decision;
SELECT tmplt_id,mdl_spt_cd,tmplt_ver_no,tmplt_stat_cd,aply_bgn_dtm,tmplt_expn FROM tmd_mdl_inpttmpltm WHERE tmplt_id=@ci_new;
-- published_must_be_1=1 / VERIFIED_PUBLISHED일 때만 마지막 ROLLBACK 대신 COMMIT 선택.
ROLLBACK;
-- COMMIT 후 새 연결에서 정확한 ID/marker/ACTIVE 1개를 재확인한다. 발행 후 정의는 불변이다.
-- 새 Case/업로드는 모델 DRAFT_C_STRUCTURE의 ACTIVE를 사용한다. source-key는 기존 API 계약/배포 설정과 대조한다.
-- 기존 Case binding은 별도 승인 작업이며 여기서는 INSERT/UPDATE하지 않는다.
-- 실제 격리 fixture로 구조94(탐색93)/Annual+lngMode/업로드 header/승인된 편집/FINALIZE 성공·파일을 확인한다.
-- 업로드·CHANGE·FINALIZE의 최초202/HTTP200만으로 완료를 판단하지 말고 data.status=SUCCEEDED와 결과 재조회를 확인한다.
-- COMMIT 전 실패: ROLLBACK. 이후 실패: 접수/소비를 비활성화하고 발행본·이력·Case/파일을 보존하여 수정 배포한다.
-- 이미 참조된 ACTIVE를 DRAFT로 되돌리거나 삭제하지 않는다. 정책 변경은 새 버전으로 진행한다.
-- 운영계는 개발계와 별도 pre-check/reader 배포·백업·접수 활성화·동작 결과를 확인한다.
-- Implementation readiness: ready for manual rehearsal after compatible reader deployment; 실제 MySQL/API 검증은 별도.
"""
    return text


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='fail if the checked-in generated SQL differs; no writes')
    args = parser.parse_args()
    payload, source = assemble()
    payload_digest = digest(compact(payload))
    outputs = {
        'model-input-catalog-install-registration.sql': registration(payload, source, payload_digest),
        'model-input-catalog-install-publication.sql': publication(payload, source, payload_digest),
    }
    for name, content in outputs.items():
        path = DOCS / name
        if args.check:
            assert path.exists() and path.read_text() == content, f'Generated SQL differs: {path}'
        else:
            path.write_text(content)
        print(f'{"CHECK" if args.check else "WRITE"} {name}: {len(content.encode())} bytes')
    print(f'PASS: 4 files / 70 sheets / 94 tables / 146 scopes / 53 SET READY; payload sha256={payload_digest}')


if __name__ == '__main__':
    main()
