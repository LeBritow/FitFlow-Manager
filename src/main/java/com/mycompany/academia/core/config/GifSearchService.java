package com.mycompany.academia.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class GifSearchService {

    private static final String GIPHY_API_KEY = carregarChave();

    private static String carregarChave() {
        String env = System.getenv("GIPHY_API_KEY");
        if (env != null && !env.isEmpty()) return env;

        String prop = System.getProperty("giphy.api.key");
        if (prop != null && !prop.isEmpty()) return prop;

        try (InputStream in = GifSearchService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String file = p.getProperty("giphy.api.key");
                if (file != null && !file.isEmpty() && !file.equals("SUA_CHAVE_AQUI")) return file;
            }
        } catch (Exception ignored) {}

        return "";
    }
    private static final String GIPHY_URL = "https://api.giphy.com/v1/gifs/search?api_key=%s&q=%s&limit=5&rating=g";

    private final HttpClient httpClient;

    public GifSearchService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String buscarMelhorGif(String nome, String grupoMuscular) {
        if (GIPHY_API_KEY.isEmpty()) {
            return null;
        }
        try {
            StringBuilder queryBuilder = new StringBuilder(nome);
            queryBuilder.append(" gym exercise");
            if (grupoMuscular != null && !grupoMuscular.isEmpty()) {
                queryBuilder.append(" ").append(grupoMuscular);
            }

            String query = URLEncoder.encode(queryBuilder.toString(), StandardCharsets.UTF_8);
            String url = String.format(GIPHY_URL, GIPHY_API_KEY, query);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data");

            if (data == null || data.size() == 0) {
                return null;
            }

            JsonObject primeiro = data.get(0).getAsJsonObject();
            JsonObject images = primeiro.getAsJsonObject("images");
            JsonObject fixedHeight = images.getAsJsonObject("fixed_height");
            return fixedHeight.get("url").getAsString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}