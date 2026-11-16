package com.stride.report;

import java.util.ArrayList;
import java.util.List;

/** One management report (RP01-RP05): a chart, the same data as a table, and notes. */
public final class Report {

    private final String id;
    private final String title;
    private final String purpose;
    private final List<String> columns;
    private final List<List<String>> rows = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private Chart chart;
    private String scope;

    public Report(String id, String title, String purpose, List<String> columns) {
        this.id = id;
        this.title = title;
        this.purpose = purpose;
        this.columns = columns;
    }

    public Report row(Object... cells) {
        List<String> r = new ArrayList<>();
        for (Object c : cells) {
            r.add(c == null ? "" : String.valueOf(c));
        }
        rows.add(r);
        return this;
    }

    public Report note(String n) {
        notes.add(n);
        return this;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public List<String> getColumns() { return columns; }
    public List<List<String>> getRows() { return rows; }
    public List<String> getNotes() { return notes; }
    public Chart getChart() { return chart; }
    public void setChart(Chart chart) { this.chart = chart; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    /** The table as CSV (RFC 4180), safe to open in a spreadsheet. */
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        line(sb, columns);
        for (List<String> r : rows) {
            line(sb, r);
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(cells.get(i)));
        }
        sb.append("\r\n");
    }

    /** Quotes a cell, and defuses text a spreadsheet would run as a formula (CSV injection). */
    static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+(\\.\\d+)?%?")) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
