package com.echanneling.service;
public final class CsvExport {
    private CsvExport() {}
    public static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        String first = text.stripLeading();
        if ((!first.isEmpty() && "=+-@".indexOf(first.charAt(0)) >= 0) || text.startsWith("\t") || text.startsWith("\r") || text.startsWith("\n")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
