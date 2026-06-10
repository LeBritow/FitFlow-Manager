package com.mycompany.academia.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GifSearchService {

    private static final String GIPHY_API_KEY = "9mNtPeAd0KOhIJl0gwc2gAKi8EQVhSOF";
    private static final String GIPHY_URL = "https://api.giphy.com/v1/gifs/search?api_key=%s&q=%s&limit=5&rating=g";

    private final HttpClient httpClient;

    public GifSearchService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String buscarMelhorGif(String termo) {
        return buscarMelhorGif(termo, null);
    }

    public String buscarMelhorGif(String nome, String grupoMuscular) {
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
