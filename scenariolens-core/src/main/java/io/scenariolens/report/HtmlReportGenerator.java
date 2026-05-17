package io.scenariolens.report;

import io.scenariolens.matrix.ScenarioRow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class HtmlReportGenerator {

    public void generate(GapReport report, File outputDir) {
        generateAll(java.util.Collections.singletonList(report), outputDir);
    }

    public void generateAll(java.util.List<GapReport> reports, File outputDir) {
        if (!outputDir.exists()) outputDir.mkdirs();
        File file = new File(outputDir, "report.html");
        try (FileWriter w = new FileWriter(file)) {
            w.write(htmlAll(reports));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String htmlAll(java.util.List<GapReport> reports) {
        int totalScenarios = reports.stream().mapToInt(GapReport::getTotalScenarios).sum();
        int totalCovered   = reports.stream().mapToInt(GapReport::getCoveredScenarios).sum();
        int totalMissing   = totalScenarios - totalCovered;
        int covPct = totalScenarios == 0 ? 100 : (int)((totalCovered * 100.0) / totalScenarios);
        String generated   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // Build method nav pills
        StringBuilder nav = new StringBuilder();
        for (GapReport r : reports) {
            if (r.getTotalScenarios() <= 1 && r.getMissingScenarios().isEmpty() && r.getCoveredRows().isEmpty()) continue;
            String id = "m-" + r.getMethodName();
            int pct = r.getScenarioCoveragePercent();
            String color = pct == 100 ? "var(--green)" : pct == 0 ? "var(--red)" : "var(--yellow)";
            nav.append("<a href=\"#").append(id).append("\" class=\"nav-pill\" style=\"border-color:").append(color).append("\">")
               .append(r.getMethodName()).append(" <span style=\"color:").append(color).append("\">").append(pct).append("%</span></a>");
        }

        // Build per-method sections
        StringBuilder sections = new StringBuilder();
        for (GapReport r : reports) {
            if (r.getTotalScenarios() <= 1 && r.getMissingScenarios().isEmpty() && r.getCoveredRows().isEmpty()) continue;
            String id = "m-" + r.getMethodName();
            int missing = r.getTotalScenarios() - r.getCoveredScenarios();
            StringBuilder rows = new StringBuilder();
            for (ScenarioRow sr : r.getCoveredRows()) rows.append(row(sr, "covered", "✓ COVERED"));
            for (ScenarioRow sr : r.getMissingScenarios()) rows.append(row(sr, "missing", "✗ MISSING"));

            sections.append("<section id=\"").append(id).append("\" class=\"method-section\">\n")
                .append("<div class=\"method-header\">")
                .append("<span class=\"method-name\">").append(r.getMethodName()).append("()</span>")
                .append("<span style=\"display:flex;gap:8px;\">")
                .append("<span class=\"badge covered\">✓ ").append(r.getCoveredScenarios()).append(" covered</span>")
                .append("<span class=\"badge missing\">✗ ").append(missing).append(" missing</span>")
                .append("</span></div>\n")
                .append("<div class=\"table-wrap\"><table>")
                .append("<thead><tr><th style=\"width:60px\">ID</th><th>Stub Configuration</th><th>Expected Outcome</th><th style=\"width:110px\">Status</th></tr></thead>")
                .append("<tbody>").append(rows).append("</tbody></table></div>\n</section>\n");
        }

        return "<!DOCTYPE html>\n<html lang=\"en\" data-theme=\"dark\">\n<head>\n" +
            "<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<title>ScenarioLens Report</title>\n" +
            "<style>\n" +
            /* ── Dark theme (default) ── */
            ":root{--bg:#0f1117;--surface:#1a1d27;--surface2:#252836;--border:#2e3247;--text:#e2e8f0;--muted:#8892a4;--green:#22c55e;--red:#ef4444;--blue:#3b82f6;--yellow:#f59e0b;--purple:#a855f7;--header-bg:linear-gradient(135deg,#1e2235 0%,#12141f 100%);--hover-row:rgba(255,255,255,.02);}\n" +
            /* ── Light theme overrides ── */
            "[data-theme=light]{--bg:#f8fafc;--surface:#ffffff;--surface2:#f1f5f9;--border:#e2e8f0;--text:#0f172a;--muted:#64748b;--header-bg:linear-gradient(135deg,#e0e7ff 0%,#f0f4ff 100%);--hover-row:rgba(0,0,0,.02);}\n" +
            "*{box-sizing:border-box;margin:0;padding:0;}\n" +
            "body{background:var(--bg);color:var(--text);font-family:'Inter','Segoe UI',system-ui,sans-serif;font-size:14px;line-height:1.6;transition:background .2s,color .2s;}\n" +
            "a{color:var(--blue);text-decoration:none;}\n" +
            ".header{background:var(--header-bg);border-bottom:1px solid var(--border);padding:22px 40px;display:flex;align-items:center;justify-content:space-between;}\n" +
            ".logo{display:flex;align-items:center;gap:12px;}\n" +
            ".logo-icon{width:36px;height:36px;background:linear-gradient(135deg,var(--blue),var(--purple));border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:18px;}\n" +
            ".logo-text{font-size:20px;font-weight:700;letter-spacing:-0.3px;}\n" +
            ".logo-text span{color:var(--blue);}\n" +
            ".header-right{display:flex;align-items:center;gap:20px;}\n" +
            ".meta{text-align:right;color:var(--muted);font-size:12px;}\n" +
            ".meta strong{color:var(--text);display:block;font-size:16px;font-weight:600;margin-bottom:2px;}\n" +
            /* ── Theme toggle button ── */
            ".theme-btn{display:flex;align-items:center;gap:6px;padding:7px 14px;border-radius:20px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-size:12px;font-weight:500;cursor:pointer;transition:background .15s,border-color .15s;white-space:nowrap;}\n" +
            ".theme-btn:hover{background:var(--surface2);}\n" +
            ".theme-btn .icon{font-size:15px;transition:transform .3s;}\n" +
            ".main{max-width:1100px;margin:0 auto;padding:32px 24px;}\n" +
            ".stats{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:32px;}\n" +
            ".stat{background:var(--surface);border:1px solid var(--border);border-radius:12px;padding:20px 24px;transition:background .2s;}\n" +
            ".stat-label{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.5px;margin-bottom:8px;}\n" +
            ".stat-value{font-size:32px;font-weight:700;line-height:1;}\n" +
            ".stat-value.green{color:var(--green);} .stat-value.red{color:var(--red);} .stat-value.blue{color:var(--blue);} .stat-value.yellow{color:var(--yellow);}\n" +
            ".stat-sub{color:var(--muted);font-size:11px;margin-top:4px;}\n" +
            ".progress-bar{background:var(--surface2);border-radius:4px;height:6px;margin-top:10px;overflow:hidden;}\n" +
            ".progress-fill{height:100%;border-radius:4px;background:linear-gradient(90deg,var(--blue),var(--purple));}\n" +
            ".nav{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:28px;}\n" +
            ".nav-pill{padding:5px 14px;border-radius:20px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-size:12px;font-weight:500;transition:background .15s;}\n" +
            ".nav-pill:hover{background:var(--surface2);}\n" +
            ".method-section{margin-bottom:40px;}\n" +
            ".method-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;padding:14px 18px;background:var(--surface);border:1px solid var(--border);border-radius:10px;transition:background .2s;}\n" +
            ".method-name{font-family:'JetBrains Mono','Fira Code',monospace;font-size:15px;font-weight:600;color:var(--blue);}\n" +
            ".badge{display:inline-flex;align-items:center;gap:4px;padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;}\n" +
            ".badge.missing{background:rgba(239,68,68,.15);color:var(--red);border:1px solid rgba(239,68,68,.3);}\n" +
            ".badge.covered{background:rgba(34,197,94,.15);color:var(--green);border:1px solid rgba(34,197,94,.3);}\n" +
            ".table-wrap{background:var(--surface);border:1px solid var(--border);border-radius:12px;overflow:hidden;margin-bottom:8px;transition:background .2s;}\n" +
            "table{width:100%;border-collapse:collapse;}\n" +
            "thead tr{background:var(--surface2);transition:background .2s;}\n" +
            "th{padding:10px 16px;text-align:left;font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:.5px;color:var(--muted);border-bottom:1px solid var(--border);}\n" +
            "td{padding:10px 16px;border-bottom:1px solid var(--border);vertical-align:top;}\n" +
            "tr:last-child td{border-bottom:none;}\n" +
            "tr:hover td{background:var(--hover-row);}\n" +
            ".id{font-family:'JetBrains Mono','Fira Code',monospace;font-size:12px;color:var(--muted);white-space:nowrap;}\n" +
            ".stub-line{font-family:'JetBrains Mono','Fira Code',monospace;font-size:12px;color:var(--blue);margin-bottom:3px;}\n" +
            ".stub-line span{color:var(--purple);}\n" +
            ".status-covered{display:inline-flex;align-items:center;gap:5px;color:var(--green);font-weight:600;font-size:12px;}\n" +
            ".status-missing{display:inline-flex;align-items:center;gap:5px;color:var(--red);font-weight:600;font-size:12px;}\n" +
            ".footer{text-align:center;color:var(--muted);font-size:12px;padding:24px;border-top:1px solid var(--border);margin-top:16px;}\n" +
            "@media(max-width:700px){.stats{grid-template-columns:repeat(2,1fr);}.header{flex-direction:column;gap:12px;}.header-right{flex-direction:row-reverse;}}\n" +
            "</style></head>\n<body>\n" +
            "<header class=\"header\">\n" +
            "  <div class=\"logo\"><div class=\"logo-icon\">🔬</div><div class=\"logo-text\">Scenario<span>Lens</span></div></div>\n" +
            "  <div class=\"header-right\">\n" +
            "    <button class=\"theme-btn\" id=\"themeToggle\" onclick=\"toggleTheme()\" title=\"Toggle light/dark mode\">\n" +
            "      <span class=\"icon\" id=\"themeIcon\">☀️</span><span id=\"themeLabel\">Light</span>\n" +
            "    </button>\n" +
            "    <div class=\"meta\"><strong>" + reports.size() + " methods analyzed</strong>Generated " + generated + "</div>\n" +
            "  </div>\n" +
            "</header>\n" +
            "<div class=\"main\">\n" +
            "  <div class=\"stats\">\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Total Scenarios</div><div class=\"stat-value blue\">" + totalScenarios + "</div><div class=\"stat-sub\">after CFG pruning</div></div>\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Covered</div><div class=\"stat-value green\">" + totalCovered + "</div><div class=\"stat-sub\">by existing tests</div></div>\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Missing</div><div class=\"stat-value red\">" + totalMissing + "</div><div class=\"stat-sub\">gaps to fill</div></div>\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Coverage</div><div class=\"stat-value yellow\">" + covPct + "%</div><div class=\"stat-sub\">scenario coverage</div><div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width:" + covPct + "%\"></div></div></div>\n" +
            "  </div>\n" +
            "  <div class=\"nav\">" + nav + "</div>\n" +
            sections +
            "</div>\n" +
            "<footer class=\"footer\">Generated by <a href=\"https://github.com/scenariolens/scenariolens\">ScenarioLens</a> · Dependency Scenario Coverage (DSC)</footer>\n" +
            "<script>\n" +
            "  const html = document.documentElement;\n" +
            "  const icon = document.getElementById('themeIcon');\n" +
            "  const label = document.getElementById('themeLabel');\n" +
            "  // Apply saved preference on load\n" +
            "  const saved = localStorage.getItem('sl-theme') || 'dark';\n" +
            "  applyTheme(saved);\n" +
            "  function toggleTheme() {\n" +
            "    const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';\n" +
            "    applyTheme(next);\n" +
            "    localStorage.setItem('sl-theme', next);\n" +
            "  }\n" +
            "  function applyTheme(t) {\n" +
            "    html.setAttribute('data-theme', t);\n" +
            "    icon.textContent = t === 'dark' ? '\\u2600\\uFE0F' : '\\uD83C\\uDF19';\n" +
            "    label.textContent = t === 'dark' ? 'Light' : 'Dark';\n" +
            "  }\n" +
            "</script>\n" +
            "</body></html>\n";
    }


    private String html(GapReport report) {
        int total   = report.getTotalScenarios();
        int covered = report.getCoveredScenarios();
        int missing = total - covered;
        int covPct  = report.getScenarioCoveragePercent();
        int asPct   = report.getAssertionStrengthPercent();
        String generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        StringBuilder rows = new StringBuilder();
        for (ScenarioRow row : report.getCoveredRows()) {
            rows.append(row(row, "covered", "✓ COVERED"));
        }
        for (ScenarioRow row : report.getMissingScenarios()) {
            rows.append(row(row, "missing", "✗ MISSING"));
        }

        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<title>ScenarioLens Report — " + report.getMethodName() + "</title>\n" +
            "<style>\n" +
            "  :root {\n" +
            "    --bg: #0f1117; --surface: #1a1d27; --surface2: #252836;\n" +
            "    --border: #2e3247; --text: #e2e8f0; --muted: #8892a4;\n" +
            "    --green: #22c55e; --red: #ef4444; --blue: #3b82f6;\n" +
            "    --yellow: #f59e0b; --purple: #a855f7;\n" +
            "  }\n" +
            "  * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "  body { background: var(--bg); color: var(--text); font-family: 'Inter', 'Segoe UI', system-ui, sans-serif; font-size: 14px; line-height: 1.6; }\n" +
            "  a { color: var(--blue); text-decoration: none; }\n" +
            "\n" +
            "  /* Header */\n" +
            "  .header { background: linear-gradient(135deg, #1e2235 0%, #12141f 100%); border-bottom: 1px solid var(--border); padding: 28px 40px; display: flex; align-items: center; justify-content: space-between; }\n" +
            "  .logo { display: flex; align-items: center; gap: 12px; }\n" +
            "  .logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg, var(--blue), var(--purple)); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; }\n" +
            "  .logo-text { font-size: 20px; font-weight: 700; letter-spacing: -0.3px; }\n" +
            "  .logo-text span { color: var(--blue); }\n" +
            "  .meta { text-align: right; color: var(--muted); font-size: 12px; }\n" +
            "  .meta strong { color: var(--text); display: block; font-size: 16px; font-weight: 600; margin-bottom: 2px; }\n" +
            "\n" +
            "  /* Main */\n" +
            "  .main { max-width: 1100px; margin: 0 auto; padding: 32px 24px; }\n" +
            "\n" +
            "  /* Stats */\n" +
            "  .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }\n" +
            "  .stat { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 20px 24px; }\n" +
            "  .stat-label { color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }\n" +
            "  .stat-value { font-size: 32px; font-weight: 700; line-height: 1; }\n" +
            "  .stat-value.green { color: var(--green); }\n" +
            "  .stat-value.red   { color: var(--red); }\n" +
            "  .stat-value.blue  { color: var(--blue); }\n" +
            "  .stat-value.yellow { color: var(--yellow); }\n" +
            "  .stat-sub { color: var(--muted); font-size: 11px; margin-top: 4px; }\n" +
            "\n" +
            "  /* Progress bar */\n" +
            "  .progress-bar { background: var(--surface2); border-radius: 4px; height: 6px; margin-top: 10px; overflow: hidden; }\n" +
            "  .progress-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, var(--blue), var(--purple)); transition: width 0.8s ease; }\n" +
            "\n" +
            "  /* Section heading */\n" +
            "  .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }\n" +
            "  .section-title { font-size: 16px; font-weight: 600; }\n" +
            "  .badge { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; }\n" +
            "  .badge.missing { background: rgba(239,68,68,0.15); color: var(--red); border: 1px solid rgba(239,68,68,0.3); }\n" +
            "  .badge.covered { background: rgba(34,197,94,0.15); color: var(--green); border: 1px solid rgba(34,197,94,0.3); }\n" +
            "\n" +
            "  /* Table */\n" +
            "  .table-wrap { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; margin-bottom: 32px; }\n" +
            "  table { width: 100%; border-collapse: collapse; }\n" +
            "  thead tr { background: var(--surface2); }\n" +
            "  th { padding: 12px 16px; text-align: left; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; color: var(--muted); border-bottom: 1px solid var(--border); }\n" +
            "  td { padding: 12px 16px; border-bottom: 1px solid var(--border); vertical-align: top; }\n" +
            "  tr:last-child td { border-bottom: none; }\n" +
            "  tr:hover td { background: rgba(255,255,255,0.02); }\n" +
            "  .id { font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 12px; color: var(--muted); white-space: nowrap; }\n" +
            "  .stub-line { font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 12px; color: var(--blue); margin-bottom: 3px; }\n" +
            "  .stub-line span { color: var(--purple); }\n" +
            "  .outcome { font-size: 12px; color: var(--text); }\n" +
            "  .status-covered { display: inline-flex; align-items: center; gap: 5px; color: var(--green); font-weight: 600; font-size: 12px; }\n" +
            "  .status-missing  { display: inline-flex; align-items: center; gap: 5px; color: var(--red);   font-weight: 600; font-size: 12px; }\n" +
            "\n" +
            "  /* Footer */\n" +
            "  .footer { text-align: center; color: var(--muted); font-size: 12px; padding: 24px; border-top: 1px solid var(--border); margin-top: 16px; }\n" +
            "  .footer a { color: var(--blue); }\n" +
            "\n" +
            "  @media (max-width: 700px) { .stats { grid-template-columns: repeat(2, 1fr); } .header { flex-direction: column; gap: 12px; text-align: center; } .meta { text-align: center; } }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body>\n" +

            "<header class=\"header\">\n" +
            "  <div class=\"logo\">\n" +
            "    <div class=\"logo-icon\">🔬</div>\n" +
            "    <div class=\"logo-text\">Scenario<span>Lens</span></div>\n" +
            "  </div>\n" +
            "  <div class=\"meta\">\n" +
            "    <strong>" + report.getMethodName() + "</strong>\n" +
            "    Generated " + generated + "\n" +
            "  </div>\n" +
            "</header>\n" +

            "<div class=\"main\">\n" +

            "  <div class=\"stats\">\n" +
            "    <div class=\"stat\">\n" +
            "      <div class=\"stat-label\">Total Scenarios</div>\n" +
            "      <div class=\"stat-value blue\">" + total + "</div>\n" +
            "      <div class=\"stat-sub\">after CFG pruning</div>\n" +
            "    </div>\n" +
            "    <div class=\"stat\">\n" +
            "      <div class=\"stat-label\">Covered</div>\n" +
            "      <div class=\"stat-value green\">" + covered + "</div>\n" +
            "      <div class=\"stat-sub\">by existing tests</div>\n" +
            "    </div>\n" +
            "    <div class=\"stat\">\n" +
            "      <div class=\"stat-label\">Missing</div>\n" +
            "      <div class=\"stat-value red\">" + missing + "</div>\n" +
            "      <div class=\"stat-sub\">gaps to fill</div>\n" +
            "    </div>\n" +
            "    <div class=\"stat\">\n" +
            "      <div class=\"stat-label\">Coverage</div>\n" +
            "      <div class=\"stat-value yellow\">" + covPct + "%</div>\n" +
            "      <div class=\"stat-sub\">scenario coverage</div>\n" +
            "      <div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width:" + covPct + "%\"></div></div>\n" +
            "    </div>\n" +
            "  </div>\n" +

            "  <div class=\"section-header\">\n" +
            "    <div class=\"section-title\">Scenario Matrix</div>\n" +
            "    <div style=\"display:flex;gap:8px;\">\n" +
            "      <span class=\"badge covered\">✓ " + covered + " covered</span>\n" +
            "      <span class=\"badge missing\">✗ " + missing + " missing</span>\n" +
            "    </div>\n" +
            "  </div>\n" +

            "  <div class=\"table-wrap\">\n" +
            "    <table>\n" +
            "      <thead><tr>\n" +
            "        <th style=\"width:60px\">ID</th>\n" +
            "        <th>Stub Configuration</th>\n" +
            "        <th>Expected Outcome</th>\n" +
            "        <th style=\"width:110px\">Status</th>\n" +
            "      </tr></thead>\n" +
            "      <tbody>" + rows + "</tbody>\n" +
            "    </table>\n" +
            "  </div>\n" +

            "</div>\n" +

            "<footer class=\"footer\">\n" +
            "  Generated by <a href=\"https://github.com/scenariolens/scenariolens\">ScenarioLens</a> · " +
            "Dependency Scenario Coverage (DSC)\n" +
            "</footer>\n" +
            "</body></html>\n";
    }

    private String row(ScenarioRow scenarioRow, String cssClass, String statusText) {
        String stubs = scenarioRow.getStubs().stream()
            .map(s -> "<div class=\"stub-line\">" +
                s.getCallNode().getVariableName() + "." + s.getCallNode().getMethodName() +
                "() <span>→ " + escape(s.getExactValue()) + "</span></div>")
            .collect(Collectors.joining());

        String statusHtml = "covered".equals(cssClass)
            ? "<span class=\"status-covered\">✓ COVERED</span>"
            : "<span class=\"status-missing\">✗ MISSING</span>";

        return "<tr>" +
            "<td><span class=\"id\">" + scenarioRow.getId() + "</span></td>" +
            "<td>" + stubs + "</td>" +
            "<td><span class=\"outcome\">" + escape(scenarioRow.getExpectedOutcome()) + "</span></td>" +
            "<td>" + statusHtml + "</td>" +
            "</tr>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
