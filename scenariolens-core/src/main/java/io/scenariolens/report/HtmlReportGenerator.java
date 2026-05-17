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
        if (!outputDir.exists()) outputDir.mkdirs();
        File file = new File(outputDir, "report.html");

        try (FileWriter w = new FileWriter(file)) {
            w.write(html(report));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            "Mock-Aware Combinatorial Dependency Coverage (MCDC²)\n" +
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
