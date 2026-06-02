package com.sk.domain.test;

import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/db-console")
@RequiredArgsConstructor
public class DbConsoleController {

    private final DbConsoleService dbConsoleService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String page() {
        return CONSOLE_HTML;
    }

    @PostMapping("/execute")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN)
    })
    public BaseResponse<DbConsoleResult> execute(@RequestBody DbConsoleExecuteReq request) {
        return BaseResponseFactory.create(dbConsoleService.execute(request.sql()));
    }

    private static final String CONSOLE_HTML = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>MI DB Console</title>
        <style>
          :root { color-scheme: dark; }
          * { box-sizing: border-box; }
          body { margin:0; font-family: ui-sans-serif, system-ui, "Segoe UI", Roboto, sans-serif; background:#0f1419; color:#e6edf3; }
          header { padding:14px 20px; background:#161b22; border-bottom:1px solid #30363d; display:flex; align-items:center; gap:12px; }
          header h1 { font-size:15px; margin:0; font-weight:600; letter-spacing:.3px; }
          .badge { background:#7d1f1f; color:#ffd7d7; font-size:11px; padding:3px 8px; border-radius:999px; font-weight:600; }
          .wrap { padding:20px; max-width:1200px; margin:0 auto; }
          label { display:block; font-size:12px; color:#8b949e; margin:0 0 6px; }
          input[type=text], textarea {
            width:100%; background:#0d1117; border:1px solid #30363d; color:#e6edf3;
            border-radius:8px; padding:10px 12px; font-size:13px; outline:none;
          }
          input[type=text]:focus, textarea:focus { border-color:#388bfd; }
          textarea { font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace; min-height:160px; resize:vertical; line-height:1.5; }
          .row { margin-bottom:14px; }
          .bar { display:flex; align-items:center; gap:12px; margin:4px 0 16px; }
          button {
            background:#238636; color:#fff; border:none; border-radius:8px; padding:9px 18px;
            font-size:13px; font-weight:600; cursor:pointer;
          }
          button:hover { background:#2ea043; }
          button:disabled { opacity:.6; cursor:default; }
          .hint { font-size:12px; color:#8b949e; }
          .status { font-size:12px; margin-left:auto; }
          .status.err { color:#ff7b72; }
          .status.ok { color:#7ee787; }
          .result { overflow:auto; border:1px solid #30363d; border-radius:8px; max-height:60vh; }
          table { border-collapse:collapse; width:100%; font-size:12px; }
          th, td { border-bottom:1px solid #21262d; padding:7px 10px; text-align:left; white-space:pre-wrap; vertical-align:top; max-width:480px; }
          th { background:#161b22; position:sticky; top:0; color:#c9d1d9; font-weight:600; }
          tr:hover td { background:#11161c; }
          .empty { padding:18px; color:#8b949e; font-size:13px; }
        </style>
        </head>
        <body>
        <header>
          <h1>MI DB Console</h1>
          <span class="badge">ADMIN ONLY · LIVE DB</span>
        </header>
        <div class="wrap">
          <div class="row">
            <label>JWT Access Token</label>
            <input id="token" type="text" placeholder="Bearer 없이 토큰 값만 붙여넣기" autocomplete="off" spellcheck="false">
          </div>
          <div class="row">
            <label>SQL (SELECT / SHOW / DESCRIBE / EXPLAIN / INSERT / UPDATE / DELETE)</label>
            <textarea id="sql" spellcheck="false" placeholder="SELECT * FROM tmd_gdsdatam_file_smry LIMIT 20"></textarea>
          </div>
          <div class="bar">
            <button id="run">실행 (Ctrl/Cmd+Enter)</button>
            <span class="hint">멀티 스테이트먼트·DDL 차단 · WHERE 없는 UPDATE/DELETE 차단 · 최대 2000행</span>
            <span id="status" class="status"></span>
          </div>
          <div id="result" class="result"><div class="empty">결과가 여기에 표시됩니다.</div></div>
        </div>
        <script>
          const $ = (id) => document.getElementById(id);
          const tokenEl = $("token"), sqlEl = $("sql"), runEl = $("run"), statusEl = $("status"), resultEl = $("result");
          tokenEl.value = localStorage.getItem("mi_db_token") || "";
          sqlEl.value = localStorage.getItem("mi_db_sql") || "";

          function setStatus(msg, kind) {
            statusEl.textContent = msg;
            statusEl.className = "status" + (kind ? " " + kind : "");
          }
          function clearResult() { resultEl.innerHTML = ""; }

          function renderRead(data) {
            clearResult();
            if (!data.rows || data.rows.length === 0) {
              const d = document.createElement("div");
              d.className = "empty";
              d.textContent = "0 rows";
              resultEl.appendChild(d);
              return;
            }
            const table = document.createElement("table");
            const thead = document.createElement("thead");
            const htr = document.createElement("tr");
            data.columns.forEach((c) => {
              const th = document.createElement("th");
              th.textContent = c;
              htr.appendChild(th);
            });
            thead.appendChild(htr);
            table.appendChild(thead);
            const tbody = document.createElement("tbody");
            data.rows.forEach((row) => {
              const tr = document.createElement("tr");
              data.columns.forEach((c) => {
                const td = document.createElement("td");
                const v = row[c];
                td.textContent = (v === null || v === undefined) ? "NULL" : String(v);
                tr.appendChild(td);
              });
              tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            resultEl.appendChild(table);
          }

          function renderWrite(data) {
            clearResult();
            const d = document.createElement("div");
            d.className = "empty";
            d.textContent = "affected rows: " + data.affectedRows;
            resultEl.appendChild(d);
          }

          async function run() {
            const token = tokenEl.value.trim();
            const sql = sqlEl.value.trim();
            localStorage.setItem("mi_db_token", token);
            localStorage.setItem("mi_db_sql", sql);
            if (!token) { setStatus("토큰을 입력하세요", "err"); return; }
            if (!sql) { setStatus("SQL을 입력하세요", "err"); return; }
            runEl.disabled = true;
            setStatus("실행 중...", "");
            try {
              const res = await fetch("/api/v1/mi/db-console/execute", {
                method: "POST",
                headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
                body: JSON.stringify({ sql: sql })
              });
              const json = await res.json().catch(() => null);
              if (!res.ok || !json || json.status === false) {
                clearResult();
                const msg = (json && json.message) ? json.message : ("HTTP " + res.status);
                setStatus(msg, "err");
                return;
              }
              const data = json.data;
              if (data.mode === "READ") {
                renderRead(data);
                setStatus(data.rowCount + " rows" + (data.truncated ? " (truncated)" : "") + " · " + data.elapsedMs + "ms", "ok");
              } else {
                renderWrite(data);
                setStatus(data.affectedRows + " affected · " + data.elapsedMs + "ms", "ok");
              }
            } catch (e) {
              setStatus("요청 실패: " + e.message, "err");
            } finally {
              runEl.disabled = false;
            }
          }

          runEl.addEventListener("click", run);
          sqlEl.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) { e.preventDefault(); run(); }
          });
        </script>
        </body>
        </html>
        """;

}
