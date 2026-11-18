package com.akif.testsupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakeStripeServer implements AutoCloseable {

    public record Recorded(String method, String path, Map<String, String> headers, Map<String, String> form) {
        public String header(String name) {
            return headers.get(name.toLowerCase());
        }
    }

    private record ErrorResponse(int status, String body) {
    }

    private final HttpServer server;
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());
    private final Deque<ErrorResponse> errors = new ArrayDeque<>();
    private final Map<String, Map<String, String>> intents = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public FakeStripeServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<Recorded> requests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    public List<Recorded> requestsTo(String method, String pathPrefix) {
        return requests().stream()
                .filter(r -> r.method().equals(method) && r.path().startsWith(pathPrefix))
                .toList();
    }

    public void enqueueError(int status, String type, String code, String message) {
        String body = "{\"error\":{\"type\":\"" + type + "\",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}";
        synchronized (errors) {
            errors.add(new ErrorResponse(status, body));
        }
    }

    public void setIntentStatus(String intentId, String status) {
        intents.computeIfAbsent(intentId, key -> new HashMap<>()).put("status", status);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> headers = new HashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
        String path = exchange.getRequestURI().getPath();
        Recorded recorded = new Recorded(exchange.getRequestMethod(), path, headers, parseForm(body));
        requests.add(recorded);

        ErrorResponse error;
        synchronized (errors) {
            error = errors.poll();
        }
        if (error != null) {
            respond(exchange, error.status(), error.body());
            return;
        }
        respond(exchange, 200, route(recorded));
    }

    private String route(Recorded request) {
        String path = request.path();
        String method = request.method();
        if (path.equals("/v1/customers") && method.equals("POST")) {
            return "{\"id\":\"cus_fake\",\"object\":\"customer\"}";
        }
        if (path.equals("/v1/payment_intents") && method.equals("POST")) {
            String id = "pi_fake_" + sequence.incrementAndGet();
            Map<String, String> state = intents.computeIfAbsent(id, key -> new HashMap<>());
            boolean confirm = "true".equals(request.form().get("confirm"));
            state.put("status", confirm ? "succeeded" : "requires_payment_method");
            state.put("amount", request.form().getOrDefault("amount", "0"));
            state.put("currency", request.form().getOrDefault("currency", "usd"));
            state.put("customer", request.form().getOrDefault("customer", "cus_fake"));
            return intentJson(id, state);
        }
        if (path.startsWith("/v1/payment_intents/")) {
            String rest = path.substring("/v1/payment_intents/".length());
            String id = rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest;
            Map<String, String> state = intents.computeIfAbsent(id, key -> {
                Map<String, String> created = new HashMap<>();
                created.put("status", "requires_capture");
                created.put("amount", "1000");
                created.put("currency", "usd");
                created.put("customer", "cus_fake");
                return created;
            });
            if (path.endsWith("/capture")) {
                state.put("status", "succeeded");
            } else if (path.endsWith("/cancel")) {
                state.put("status", "canceled");
            }
            return intentJson(id, state);
        }
        if (path.equals("/v1/refunds") && method.equals("POST")) {
            return "{\"id\":\"re_fake_" + sequence.incrementAndGet() + "\",\"object\":\"refund\",\"status\":\"succeeded\","
                    + "\"amount\":" + request.form().getOrDefault("amount", "0") + ",\"currency\":\"usd\"}";
        }
        if (path.startsWith("/v1/charges") && method.equals("GET")) {
            return "{\"object\":\"list\",\"data\":[],\"has_more\":false,\"url\":\"/v1/charges\"}";
        }
        return "{}";
    }

    private String intentJson(String id, Map<String, String> state) {
        return "{\"id\":\"" + id + "\",\"object\":\"payment_intent\",\"status\":\"" + state.get("status") + "\","
                + "\"amount\":" + state.get("amount") + ",\"currency\":\"" + state.get("currency") + "\","
                + "\"client_secret\":\"" + id + "_secret_fake\",\"customer\":\"" + state.get("customer") + "\","
                + "\"payment_method\":\"pm_fake\"}";
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        if (body.isBlank()) {
            return form;
        }
        for (String pair : body.split("&")) {
            int index = pair.indexOf('=');
            String key = URLDecoder.decode(index < 0 ? pair : pair.substring(0, index), StandardCharsets.UTF_8);
            String value = index < 0 ? "" : URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            form.put(key, value);
        }
        return form;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Request-Id", "req_fake");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
