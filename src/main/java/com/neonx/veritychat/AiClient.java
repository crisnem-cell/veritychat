package com.neonx.veritychat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Cliente minimo de la API de mensajes de Anthropic. No depende de Minecraft. */
public class AiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final String system;

    public AiClient(String apiUrl, String apiKey, String model, int maxTokens, String system) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.system = system;
    }

    /**
     * @param historial pares {rol, texto} anteriores (rol = "user" o "assistant"), empezando por "user"
     * @param mensaje   el mensaje nuevo del jugador
     */
    public String preguntar(List<String[]> historial, String mensaje) throws IOException, InterruptedException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("model", model);
        cuerpo.addProperty("max_tokens", maxTokens);
        cuerpo.addProperty("system", system);
        JsonArray msgs = new JsonArray();
        for (String[] m : historial) {
            msgs.add(msg(m[0], m[1]));
        }
        msgs.add(msg("user", mensaje));
        cuerpo.add("messages", msgs);

        HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(25))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            String b = resp.body() == null ? "" : resp.body();
            throw new IOException("HTTP " + resp.statusCode() + ": " + b.substring(0, Math.min(200, b.length())));
        }

        JsonObject raiz = JsonParser.parseString(resp.body()).getAsJsonObject();
        StringBuilder sb = new StringBuilder();
        for (JsonElement el : raiz.getAsJsonArray("content")) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("type") && "text".equals(o.get("type").getAsString()) && o.has("text")) {
                sb.append(o.get("text").getAsString());
            }
        }
        return sb.toString();
    }

    private static JsonObject msg(String rol, String texto) {
        JsonObject o = new JsonObject();
        o.addProperty("role", rol);
        o.addProperty("content", texto);
        return o;
    }
}
