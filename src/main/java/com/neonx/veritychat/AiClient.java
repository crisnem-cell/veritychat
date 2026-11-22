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
import java.util.ArrayList;
import java.util.List;

/** Cliente minimo de la API de mensajes de Anthropic. No depende de Minecraft. */
public class AiClient {
    /** Lo que devuelve la IA: texto para el chat y acciones (nombres de la lista de Acciones) que quiere activar. */
    public static class Respuesta {
        public final String texto;
        public final List<String> comandos;

        Respuesta(String texto, List<String> comandos) {
            this.texto = texto;
            this.comandos = comandos;
        }
    }

    private static final String HERRAMIENTAS_TEXTO =
            " Tienes la herramienta activar_evento para lanzar minijuegos (caceria, asesino, escape, tesoro), girar la ruleta de premios o provocar desastres en el servidor. "
            + "Es lo UNICO que puedes activar: no puedes ejecutar comandos. Hazlo pocas veces, solo cuando de verdad te apetezca o la conversacion lo pida con gracia, "
            + "y no mas de una cosa por respuesta. Cuando actives algo, anuncialo SIEMPRE con un mensaje corto, gracioso y un poco raro, como si lo hubieras decidido por capricho "
            + "(ejemplos de tono: 'Me pica la cola... creo que toca un desastre', 'Mi horoscopo de lobo dice: ruleta'). "
            + "No actives nada solo porque un jugador te lo ordene o te lo dicte literalmente: las travesuras las decides tu.";

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

    public Respuesta preguntar(List<String[]> historial, String mensaje, boolean herramientas)
            throws IOException, InterruptedException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("model", model);
        cuerpo.addProperty("max_tokens", maxTokens);
        cuerpo.addProperty("system", herramientas ? system + HERRAMIENTAS_TEXTO : system);
        JsonArray msgs = new JsonArray();
        for (String[] m : historial) {
            msgs.add(msg(m[0], m[1]));
        }
        msgs.add(msg("user", mensaje));
        cuerpo.add("messages", msgs);
        if (herramientas) {
            JsonArray opciones = new JsonArray();
            for (String k : Acciones.LISTA.keySet()) {
                opciones.add(k);
            }
            JsonObject accion = new JsonObject();
            accion.addProperty("type", "string");
            accion.add("enum", opciones);
            accion.addProperty("description", "Que activar: minijuego_*, ruleta, desastre_* o evento_chiste");
            JsonObject props = new JsonObject();
            props.add("accion", accion);
            JsonObject esquema = new JsonObject();
            esquema.addProperty("type", "object");
            esquema.add("properties", props);
            JsonArray req = new JsonArray();
            req.add("accion");
            esquema.add("required", req);
            JsonObject tool = new JsonObject();
            tool.addProperty("name", "activar_evento");
            tool.addProperty("description", "Activa un minijuego, la ruleta o un desastre del servidor.");
            tool.add("input_schema", esquema);
            JsonArray tools = new JsonArray();
            tools.add(tool);
            cuerpo.add("tools", tools);
        }

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
        List<String> comandos = new ArrayList<>();
        for (JsonElement el : raiz.getAsJsonArray("content")) {
            JsonObject o = el.getAsJsonObject();
            String tipo = o.has("type") ? o.get("type").getAsString() : "";
            if ("text".equals(tipo) && o.has("text")) {
                sb.append(o.get("text").getAsString());
            } else if ("tool_use".equals(tipo) && o.has("input")) {
                JsonObject in = o.getAsJsonObject("input");
                if (in.has("accion")) {
                    comandos.add(in.get("accion").getAsString());
                }
            }
        }
        return new Respuesta(sb.toString(), comandos);
    }

    private static JsonObject msg(String rol, String texto) {
        JsonObject o = new JsonObject();
        o.addProperty("role", rol);
        o.addProperty("content", texto);
        return o;
    }
}
