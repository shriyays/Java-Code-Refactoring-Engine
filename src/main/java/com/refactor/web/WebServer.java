package com.refactor.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Minimal embedded HTTP server.
 *
 * Routes:
 *   GET  /           → serves index.html
 *   POST /analyze    → AnalyzeHandler (AST analysis + refactoring)
 *
 * Start: mvn exec:java -Dexec.mainClass="com.refactor.web.WebServer"
 * Then open: http://localhost:8080
 */
public class WebServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/analyze", new AnalyzeHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Server running at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop.");
    }

    // -----------------------------------------------------------------------
    // Serves index.html from the classpath (src/main/resources/web/)
    // -----------------------------------------------------------------------
    static class StaticFileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                serveResource(exchange, "/web/index.html", "text/html; charset=UTF-8");
            } else {
                String body = "404 Not Found";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        }

        private void serveResource(HttpExchange exchange, String resource, String contentType)
                throws IOException {
            try (InputStream is = WebServer.class.getResourceAsStream(resource)) {
                if (is == null) {
                    byte[] err = "Frontend not found".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, err.length);
                    exchange.getResponseBody().write(err);
                    exchange.getResponseBody().close();
                    return;
                }
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            }
        }
    }
}
