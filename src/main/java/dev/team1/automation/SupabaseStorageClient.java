package dev.team1.automation;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.team1.contracts.ICloudStorage;

// Sube archivos a Supabase Storage por su API REST (basado en el trabajo de José Luis, GS-122).
@Component
public class SupabaseStorageClient implements ICloudStorage {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final String url;
    private final String bucket;
    private final String apiKey;

    public SupabaseStorageClient(
            @Value("${cloud.supabase.url:}") String url,
            @Value("${cloud.supabase.bucket:}") String bucket,
            @Value("${cloud.supabase.api-key:}") String apiKey) {
        this.url = url == null ? "" : url.trim();
        this.bucket = bucket == null ? "" : bucket.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public boolean isConfigured() {
        return !url.isBlank() && !bucket.isBlank() && !apiKey.isBlank();
    }

    @Override
    public void upload(String path, byte[] content) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(objectUri(path))
            .timeout(TIMEOUT)
            .header("apikey", apiKey)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/pdf")
            // Si ya existe el resumen de ese día, se sustituye.
            .header("x-upsert", "true")
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
            .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase respondió HTTP " + response.statusCode());
        }
    }

    private URI objectUri(String path) {
        String encodedPath = Arrays.stream(path.split("/"))
            .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
            .collect(Collectors.joining("/"));
        return URI.create(url.replaceAll("/$", "") + "/storage/v1/object/"
            + URLEncoder.encode(bucket, StandardCharsets.UTF_8) + "/" + encodedPath);
    }
}