package com.refactor.web;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.engine.RefactoringEngine;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.report.AnalysisReport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles POST /analyze
 *
 * Request body (plain text): raw Java source code
 * Response body (JSON):      analysis results
 */
public class AnalyzeHandler implements HttpHandler {

    private final RefactoringEngine engine = new RefactoringEngine();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS headers so the browser can call the API
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        // Read request body (raw Java source)
        String body;
        try (InputStream is = exchange.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Strip outer JSON wrapper if the client sent {"code":"..."}
        String javaSource = extractCode(body);

        if (javaSource == null || javaSource.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"No Java source code provided\"}");
            return;
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(javaSource);
            AnalysisReport report = engine.analyseAndRefactorUntilClean(cu, 5);
            // cu has been fully mutated across all passes — capture final source
            String refactoredSource = cu.toString();
            // Count remaining smells after all passes
            int remaining = engine.detectSmells(cu).size();
            sendJson(exchange, 200, toJson(report, refactoredSource, remaining));
        } catch (Exception e) {
            String msg = escape(e.getMessage() == null ? "Parse error" : e.getMessage());
            sendJson(exchange, 400, "{\"error\":\"" + msg + "\"}");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Very simple JSON extraction — avoids pulling in a JSON library. */
    private String extractCode(String body) {
        body = body.trim();
        // If wrapped in {"code":"..."} unwrap it
        if (body.startsWith("{")) {
            int start = body.indexOf("\"code\"");
            if (start != -1) {
                int colon = body.indexOf(':', start);
                int quote1 = body.indexOf('"', colon + 1);
                int quote2 = findClosingQuote(body, quote1 + 1);
                if (quote1 != -1 && quote2 != -1) {
                    return unescape(body.substring(quote1 + 1, quote2));
                }
            }
            return null;
        }
        return body; // plain text
    }

    /** Find closing quote, respecting \" escapes. */
    private int findClosingQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return -1;
    }

    private String unescape(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String toJson(AnalysisReport report, String refactoredSource, int remainingSmells) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalSmells\":").append(report.totalSmells()).append(",");
        sb.append("\"totalRefactored\":").append(report.totalRefactored()).append(",");
        sb.append("\"totalSkipped\":").append(report.totalSkipped()).append(",");
        sb.append(String.format("\"refactorRate\":%.1f,", report.refactorRate()));

        // smells array
        sb.append("\"smells\":[");
        var smells = report.getDetectedSmells();
        for (int i = 0; i < smells.size(); i++) {
            CodeSmell s = smells.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"type\":\"").append(escape(s.getType().getDisplayName())).append("\",");
            sb.append("\"className\":\"").append(escape(s.getClassName())).append("\",");
            sb.append("\"member\":\"").append(escape(s.getMemberName() != null ? s.getMemberName() : "")).append("\",");
            sb.append("\"line\":").append(s.getLine()).append(",");
            sb.append("\"detail\":\"").append(escape(s.getDetail())).append("\"");
            sb.append("}");
        }
        sb.append("],");

        // results array
        sb.append("\"results\":[");
        var results = report.getRefactorResults();
        for (int i = 0; i < results.size(); i++) {
            RefactorResult r = results.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"smell\":\"").append(escape(r.getSmellAddressed().getDisplayName())).append("\",");
            sb.append("\"className\":\"").append(escape(r.getClassName())).append("\",");
            sb.append("\"success\":").append(r.isSuccess()).append(",");
            sb.append("\"message\":\"").append(escape(r.getMessage())).append("\"");
            sb.append("}");
        }
        sb.append("],");

        // remaining smells after all passes
        sb.append("\"remainingSmells\":").append(remainingSmells).append(",");

        // final refactored source (cu after all transformations)
        sb.append("\"refactoredCode\":\"").append(escape(refactoredSource)).append("\"");

        sb.append("}");
        return sb.toString();
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
