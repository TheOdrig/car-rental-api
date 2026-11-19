package com.akif.testsupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FakeExchangeRateServer {

    private static final Map<String, BigDecimal> USD_RATES = new LinkedHashMap<>();
    private static final long LAST_UPDATE_UNIX = 1_893_456_000L;

    static {
        USD_RATES.put("USD", new BigDecimal("1"));
        USD_RATES.put("TRY", new BigDecimal("42.49"));
        USD_RATES.put("EUR", new BigDecimal("0.87"));
        USD_RATES.put("GBP", new BigDecimal("0.76"));
        USD_RATES.put("JPY", new BigDecimal("156.00"));
    }

    private static volatile FakeExchangeRateServer instance;

    private final HttpServer server;

    private FakeExchangeRateServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(null);
        server.start();
    }

    public static FakeExchangeRateServer instance() {
        if (instance == null) {
            synchronized (FakeExchangeRateServer.class) {
                if (instance == null) {
                    instance = new FakeExchangeRateServer();
                }
            }
        }
        return instance;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v6";
    }

    private void handle(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        String base = parts.length > 0 ? parts[parts.length - 1].toUpperCase() : "";
        BigDecimal baseRate = USD_RATES.get(base);
        if (baseRate == null) {
            respond(exchange, 404, "{\"result\":\"error\",\"error-type\":\"unsupported-code\"}");
            return;
        }
        StringBuilder rates = new StringBuilder();
        for (Map.Entry<String, BigDecimal> entry : USD_RATES.entrySet()) {
            if (rates.length() > 0) {
                rates.append(',');
            }
            rates.append('"').append(entry.getKey()).append("\":")
                    .append(entry.getValue().divide(baseRate, 6, RoundingMode.HALF_UP).toPlainString());
        }
        respond(exchange, 200, "{\"result\":\"success\",\"base_code\":\"" + base + "\",\"time_last_update_unix\":"
                + LAST_UPDATE_UNIX + ",\"conversion_rates\":{" + rates + "}}");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
