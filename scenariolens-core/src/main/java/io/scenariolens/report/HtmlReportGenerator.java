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

        // Group reports by className (preserve insertion order)
        java.util.LinkedHashMap<String, java.util.List<GapReport>> byClass = new java.util.LinkedHashMap<>();
        for (GapReport r : reports) {
            if (r.getTotalScenarios() <= 1 && r.getMissingScenarios().isEmpty() && r.getCoveredRows().isEmpty()) continue;
            byClass.computeIfAbsent(r.getClassName(), k -> new java.util.ArrayList<>()).add(r);
        }

        // Build class-grouped nav
        StringBuilder nav = new StringBuilder();
        for (java.util.Map.Entry<String, java.util.List<GapReport>> entry : byClass.entrySet()) {
            String fqn = entry.getKey();
            String simpleName = fqn;
            int lastDot = fqn.lastIndexOf('.');
            if (lastDot != -1) {
                simpleName = fqn.substring(lastDot + 1);
            }
            java.util.List<GapReport> methods = entry.getValue();
            int clsTotal   = methods.stream().mapToInt(GapReport::getTotalScenarios).sum();
            int clsCovered = methods.stream().mapToInt(GapReport::getCoveredScenarios).sum();
            int clsPct = clsTotal == 0 ? 100 : (int)((clsCovered * 100.0) / clsTotal);
            String color = clsPct == 100 ? "var(--green)" : clsPct == 0 ? "var(--red)" : "var(--yellow)";
            nav.append("<a href=\"#cls-").append(fqn).append("\" class=\"nav-pill\" title=\"").append(fqn).append("\" style=\"border-color:").append(color).append("\">")
               .append(simpleName).append(" <span style=\"color:").append(color).append("\">").append(clsPct).append("%</span></a>");
        }

        // Build class-grouped sections
        StringBuilder sections = new StringBuilder();
        for (java.util.Map.Entry<String, java.util.List<GapReport>> entry : byClass.entrySet()) {
            String fqn = entry.getKey();
            String simpleName = fqn;
            int lastDot = fqn.lastIndexOf('.');
            if (lastDot != -1) {
                simpleName = fqn.substring(lastDot + 1);
            }
            java.util.List<GapReport> methods = entry.getValue();
            int clsTotal   = methods.stream().mapToInt(GapReport::getTotalScenarios).sum();
            int clsCovered = methods.stream().mapToInt(GapReport::getCoveredScenarios).sum();
            int clsMissing = clsTotal - clsCovered;
            int clsPct = clsTotal == 0 ? 100 : (int)((clsCovered * 100.0) / clsTotal);
            String clsColor = clsPct == 100 ? "var(--green)" : clsPct == 0 ? "var(--red)" : "var(--yellow)";

            String classOpenAttr = clsMissing > 0 ? " open" : "";

            // Class header (Collapsible)
            sections.append("<details id=\"cls-").append(fqn).append("\" class=\"class-section\"").append(classOpenAttr).append(">\n")
                .append("<summary class=\"class-header\">\n")
                .append("  <div class=\"class-title-row\">\n")
                .append("    <span class=\"class-icon\">☕</span>\n")
                .append("    <div style=\"flex:1\">\n")
                .append("      <div class=\"class-simple\">").append(simpleName).append("</div>\n")
                .append("      <div class=\"class-fqn\">").append(fqn).append("</div>\n")
                .append("    </div>\n")
                .append("    <span class=\"class-dsc\" style=\"color:").append(clsColor).append("\">DSC ").append(clsPct).append("%</span>\n")
                .append("    <span class=\"chevron\" style=\"margin-left:12px;font-size:12px\">▶</span>\n")
                .append("  </div>\n")
                .append("  <div class=\"class-stats\">\n")
                .append("    <span class=\"cs\"><span class=\"cs-val\">").append(methods.size()).append("</span> methods</span>\n")
                .append("    <span class=\"cs\"><span class=\"cs-val\">").append(clsTotal).append("</span> scenarios</span>\n")
                .append("    <span class=\"badge covered\">✓ ").append(clsCovered).append(" covered</span>\n")
                .append("    <span class=\"badge missing\">✗ ").append(clsMissing).append(" missing</span>\n")
                .append("  </div>\n")
                .append("</summary>\n")
                .append("<div class=\"class-body\">\n");

            // Per-method subsections
            for (GapReport r : methods) {
                String id = "m-" + fqn + "-" + r.getMethodName();
                int missing = r.getTotalScenarios() - r.getCoveredScenarios();
                int pct = r.getScenarioCoveragePercent();
                String pctColor = pct == 100 ? "var(--green)" : pct == 0 ? "var(--red)" : "var(--yellow)";
                
                StringBuilder rows = new StringBuilder();
                for (ScenarioRow sr : r.getCoveredRows()) rows.append(row(sr, "covered", "✓ COVERED"));
                for (ScenarioRow sr : r.getMissingScenarios()) rows.append(row(sr, "missing", "✗ MISSING"));

                sections.append("<div id=\"").append(id).append("\" class=\"method-section\">\n")
                    .append("  <div class=\"method-header\">\n")
                    .append("    <span class=\"method-name\">").append(r.getMethodName()).append("()</span>\n")
                    .append("    <div class=\"summary-right\">\n")
                    .append("      <span class=\"method-pct\" style=\"color:").append(pctColor).append("\">").append(pct).append("% DSC</span>\n")
                    .append("      <span class=\"badge covered\">✓ ").append(r.getCoveredScenarios()).append("</span>\n")
                    .append("      <span class=\"badge missing\">✗ ").append(missing).append("</span>\n")
                    .append("    </div>\n")
                    .append("  </div>\n")
                    .append("  <div class=\"table-wrap\">\n")
                    .append("    <table>\n")
                    .append("      <thead><tr><th style=\"width:60px\">ID</th><th style=\"width:30%\">Stub Configuration</th><th style=\"width:20%\">Expected Outcome</th><th style=\"width:35%\">Gap Rationale</th><th style=\"width:15%\">Status</th></tr></thead>\n")
                    .append("      <tbody>").append(rows).append("</tbody>\n")
                    .append("    </table>\n")
                    .append("  </div>\n")
                    .append("</div>\n");
            }
            sections.append("</div>\n</details>\n"); // end class-section
        }

        // Compute derived stats for redesigned cards
        long classesWithGaps = byClass.values().stream()
            .filter(ms -> ms.stream().anyMatch(r -> !r.getMissingScenarios().isEmpty()))
            .count();
        int totalClasses = byClass.size();
        String dscColor = covPct >= 80 ? "var(--green)" : covPct >= 60 ? "var(--yellow)" : "var(--red)";

        return "<!DOCTYPE html>\n<html lang=\"en\" data-theme=\"dark\">\n<head>\n" +
            "<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<title>ScenarioLens Report</title>\n" +
            "<style>\n" +
            ":root{--bg:#0f1117;--surface:#1a1d27;--surface2:#252836;--border:#2e3247;--text:#e2e8f0;--muted:#8892a4;--green:#22c55e;--red:#ef4444;--blue:#3b82f6;--yellow:#f59e0b;--purple:#a855f7;--header-bg:linear-gradient(135deg,#1e2235 0%,#12141f 100%);--hover-row:rgba(255,255,255,.02);}\n" +
            "[data-theme=light]{--bg:#f8fafc;--surface:#ffffff;--surface2:#f1f5f9;--border:#e2e8f0;--text:#0f172a;--muted:#64748b;--header-bg:linear-gradient(135deg,#e0e7ff 0%,#f0f4ff 100%);--hover-row:rgba(0,0,0,.02);}\n" +
            "*{box-sizing:border-box;margin:0;padding:0;}\n" +
            "body{background:var(--bg);color:var(--text);font-family:'Inter','Segoe UI',system-ui,sans-serif;font-size:14px;line-height:1.6;transition:background .2s,color .2s;}\n" +
            "a{color:var(--blue);text-decoration:none;}\n" +
            ".header{background:var(--header-bg);border-bottom:1px solid var(--border);padding:18px 40px;display:flex;align-items:center;justify-content:space-between;}\n" +
            ".logo{display:flex;align-items:center;gap:12px;}\n" +
            ".logo-icon{width:34px;height:34px;background:linear-gradient(135deg,var(--blue),var(--purple));border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:17px;}\n" +
            ".logo-text{font-size:19px;font-weight:700;letter-spacing:-0.3px;}\n" +
            ".logo-text span{color:var(--blue);}\n" +
            ".header-right{display:flex;align-items:center;gap:16px;}\n" +
            ".header-score{text-align:right;}\n" +
            ".header-score .score-val{font-size:28px;font-weight:800;line-height:1;}\n" +
            ".header-score .score-label{font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;}\n" +
            ".header-score .score-sub{font-size:12px;color:var(--muted);margin-top:2px;}\n" +
            ".header-divider{width:1px;height:40px;background:var(--border);}\n" +
            ".theme-btn{display:flex;align-items:center;gap:5px;padding:6px 12px;border-radius:20px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-size:12px;font-weight:500;cursor:pointer;transition:background .15s;white-space:nowrap;}\n" +
            ".theme-btn:hover{background:var(--surface2);}\n" +
            ".main{max-width:1160px;margin:0 auto;padding:28px 24px;}\n" +
            ".stats{display:grid;grid-template-columns:1.4fr 1fr 1fr 1fr;gap:14px;margin-bottom:28px;}\n" +
            ".stat{background:var(--surface);border:1px solid var(--border);border-radius:12px;padding:18px 22px;transition:background .2s;}\n" +
            ".stat.hero{border-color:var(--blue);box-shadow:0 0 0 1px var(--blue) inset;}\n" +
            ".stat-label{color:var(--muted);font-size:11px;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px;}\n" +
            ".stat-value{font-size:30px;font-weight:800;line-height:1;}\n" +
            ".stat-value.dsc{font-size:38px;}\n" +
            ".stat-value.green{color:var(--green);} .stat-value.red{color:var(--red);} .stat-value.blue{color:var(--blue);} .stat-value.yellow{color:var(--yellow);}\n" +
            ".stat-sub{color:var(--muted);font-size:11px;margin-top:5px;}\n" +
            ".progress-bar{background:var(--surface2);border-radius:4px;height:5px;margin-top:10px;overflow:hidden;}\n" +
            ".progress-fill{height:100%;border-radius:4px;background:linear-gradient(90deg,var(--blue),var(--purple));}\n" +
            ".nav{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:24px;align-items:center;}\n" +
            ".nav-label{font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;margin-right:4px;}\n" +
            ".nav-pill{padding:4px 12px;border-radius:20px;border:1px solid var(--border);background:var(--surface);color:var(--text);font-size:12px;font-weight:500;transition:background .15s;}\n" +
            ".nav-pill:hover{background:var(--surface2);}\n" +
            ".class-section{margin-bottom:36px;}\n" +
            "details.class-section summary{list-style:none;cursor:pointer;}\n" +
            "details.class-section summary::-webkit-details-marker{display:none;}\n" +
            ".class-header{background:var(--surface);border:1px solid var(--border);border-radius:12px;padding:16px 20px;margin-bottom:10px;transition:background .2s;}\n" +
            ".class-header:hover{border-color:var(--muted);}\n" +
            ".class-title-row{display:flex;align-items:center;gap:10px;margin-bottom:8px;}\n" +
            ".class-icon{font-size:16px;}\n" +
            ".class-simple{font-size:17px;font-weight:700;color:var(--text);font-family:'JetBrains Mono','Fira Code',monospace;}\n" +
            ".class-fqn{font-size:11px;color:var(--muted);font-family:'JetBrains Mono','Fira Code',monospace;word-break:break-all;margin-top:2px;}\n" +
            ".class-dsc{margin-left:auto;font-size:13px;font-weight:700;white-space:nowrap;}\n" +
            ".class-stats{display:flex;flex-wrap:wrap;align-items:center;gap:10px;}\n" +
            ".cs{color:var(--muted);font-size:12px;}\n" +
            ".cs-val{font-weight:700;color:var(--text);}\n" +
            ".chevron{display:inline-block;transition:transform .2s;}\n" +
            "details[open] .chevron{transform:rotate(90deg);}\n" +
            ".class-body{padding-top:4px;}\n" +
            ".method-section{margin-bottom:12px;margin-left:18px;border-left:2px solid var(--border);padding-left:14px;}\n" +
            ".method-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;padding:9px 14px;background:var(--surface2);border:1px solid var(--border);border-radius:8px;}\n" +
            ".method-name{font-family:'JetBrains Mono','Fira Code',monospace;font-size:13px;font-weight:600;color:var(--blue);}\n" +
            ".method-pct{font-size:12px;font-weight:600;}\n" +
            ".badge{display:inline-flex;align-items:center;gap:4px;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:600;}\n" +
            ".badge.missing{background:rgba(239,68,68,.15);color:var(--red);border:1px solid rgba(239,68,68,.3);}\n" +
            ".badge.covered{background:rgba(34,197,94,.15);color:var(--green);border:1px solid rgba(34,197,94,.3);}\n" +
            ".table-wrap{background:var(--surface);border:1px solid var(--border);border-radius:8px;overflow:hidden;margin-bottom:8px;}\n" +
            "table{width:100%;border-collapse:collapse;}\n" +
            "thead tr{background:var(--surface2);}\n" +
            "th{padding:9px 14px;text-align:left;font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:.5px;color:var(--muted);border-bottom:1px solid var(--border);}\n" +
            "td{padding:9px 14px;border-bottom:1px solid var(--border);vertical-align:top;}\n" +
            "tr:last-child td{border-bottom:none;}\n" +
            "tr:hover td{background:var(--hover-row);}\n" +
            ".id{font-family:'JetBrains Mono','Fira Code',monospace;font-size:12px;color:var(--muted);white-space:nowrap;}\n" +
            ".stub-line{font-family:'JetBrains Mono','Fira Code',monospace;font-size:12px;color:var(--blue);margin-bottom:3px;}\n" +
            ".stub-line span{color:var(--purple);}\n" +
            ".status-covered{display:inline-flex;align-items:center;gap:5px;color:var(--green);font-weight:600;font-size:12px;}\n" +
            ".status-missing{display:inline-flex;align-items:center;gap:5px;color:var(--red);font-weight:600;font-size:12px;}\n" +
            ".footer{text-align:center;color:var(--muted);font-size:12px;padding:20px;border-top:1px solid var(--border);margin-top:16px;}\n" +
            "@media(max-width:800px){.stats{grid-template-columns:repeat(2,1fr);}.header{flex-direction:column;gap:12px;}.header-right{flex-direction:row-reverse;}.header-score{text-align:left;}}\n" +
            "</style></head>\n<body>\n" +
            "<header class=\"header\">\n" +
            "  <div class=\"logo\"><div class=\"logo-icon\">🔬</div><div class=\"logo-text\">Scenario<span>Lens</span></div></div>\n" +
            "  <div class=\"header-right\">\n" +
            "    <button class=\"theme-btn\" id=\"themeToggle\" onclick=\"toggleTheme()\" title=\"Toggle light/dark mode\">\n" +
            "      <span id=\"themeIcon\">☀️</span><span id=\"themeLabel\">Light</span>\n" +
            "    </button>\n" +
            "    <div class=\"header-divider\"></div>\n" +
            "    <div class=\"header-score\">\n" +
            "      <div class=\"score-label\">DSC Score</div>\n" +
            "      <div class=\"score-val\" style=\"color:" + dscColor + "\">" + covPct + "%</div>\n" +
            "      <div class=\"score-sub\">" + totalMissing + " gaps · " + generated + "</div>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</header>\n" +
            "<div class=\"main\">\n" +
            "  <div class=\"stats\">\n" +
            "    <div class=\"stat hero\">\n" +
            "      <div class=\"stat-label\">DSC Score</div>\n" +
            "      <div class=\"stat-value dsc " + (covPct >= 80 ? "green" : covPct >= 60 ? "yellow" : "red") + "\">" + covPct + "%</div>\n" +
            "      <div class=\"stat-sub\">Dependency Scenario Coverage</div>\n" +
            "      <div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width:" + covPct + "%\"></div></div>\n" +
            "    </div>\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Gaps to Fix</div><div class=\"stat-value red\">" + totalMissing + "</div><div class=\"stat-sub\">missing scenarios</div></div>\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Classes with Gaps</div><div class=\"stat-value " + (classesWithGaps == 0 ? "green" : "yellow") + "\">" + classesWithGaps + " <span style=\"font-size:16px;font-weight:400;color:var(--muted)\">/ " + totalClasses + "</span></div><div class=\"stat-sub\">need attention</div></div>\n" +
            "    <div class=\"stat\"><div class=\"stat-label\">Scenarios Covered</div><div class=\"stat-value green\">" + totalCovered + "</div><div class=\"stat-sub\">of " + totalScenarios + " total</div></div>\n" +
            "  </div>\n" +
            "  <div class=\"nav\"><span class=\"nav-label\">Jump to</span>" + nav + "</div>\n" +
            sections +
            "</div>\n" +
            "<footer class=\"footer\">Generated by <a href=\"https://github.com/scenariolens/scenariolens\">ScenarioLens</a> · Dependency Scenario Coverage (DSC)</footer>\n" +
            "<script>\n" +
            "  const html = document.documentElement;\n" +
            "  const icon = document.getElementById('themeIcon');\n" +
            "  const label = document.getElementById('themeLabel');\n" +
            "  const saved = localStorage.getItem('sl-theme') || 'dark';\n" +
            "  applyTheme(saved);\n" +
            "  function toggleTheme() {\n" +
            "    const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';\n" +
            "    applyTheme(next); localStorage.setItem('sl-theme', next);\n" +
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
            "        <th style=\"width:30%\">Stub Configuration</th>\n" +
            "        <th style=\"width:20%\">Expected Outcome</th>\n" +
            "        <th style=\"width:35%\">Gap Rationale</th>\n" +
            "        <th style=\"width:15%\">Status</th>\n" +
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
            
        if (stubs.isEmpty()) {
            stubs = "<span style=\"color:var(--muted);font-style:italic;font-size:12px\">No dependencies</span>";
        }

        String reasoning;
        if ("covered".equals(cssClass)) {
            reasoning = "<span style=\"color:var(--muted);font-size:12px\">No gap. Scenario is tested.</span>";
        } else {
            String out = scenarioRow.getExpectedOutcome().toLowerCase();
            String exactOut = escape(scenarioRow.getExpectedOutcome());
            if (out.contains("throw") || out.contains("exception")) {
                reasoning = "<span style=\"color:var(--text);font-size:12px\"><strong>Missing exception test.</strong> The system is expected to result in <code>" + exactOut + "</code>, but there is no test verifying it handles this failure correctly.</span>";
            } else if (out.contains("null") || out.contains("empty")) {
                reasoning = "<span style=\"color:var(--text);font-size:12px\"><strong>Missing edge case test.</strong> The system is expected to result in <code>" + exactOut + "</code> for this empty/null data, but this edge case is untested.</span>";
            } else if (scenarioRow.getStubs().isEmpty()) {
                reasoning = "<span style=\"color:var(--text);font-size:12px\"><strong>Missing core flow test.</strong> The basic execution path without any external dependency interactions is completely untested.</span>";
            } else {
                reasoning = "<span style=\"color:var(--text);font-size:12px\"><strong>Missing combination test.</strong> A scenario where the dependencies behave exactly like this is missing. We cannot guarantee the system will result in <code>" + exactOut + "</code>.</span>";
            }
        }

        String statusHtml = "covered".equals(cssClass)
            ? "<span class=\"status-covered\">✓ COVERED</span>"
            : "<span class=\"status-missing\">✗ MISSING</span>";

        return "<tr>" +
            "<td><span class=\"id\">" + scenarioRow.getId() + "</span></td>" +
            "<td>" + stubs + "</td>" +
            "<td><span class=\"outcome\">" + escape(scenarioRow.getExpectedOutcome()) + "</span></td>" +
            "<td>" + reasoning + "</td>" +
            "<td>" + statusHtml + "</td>" +
            "</tr>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
