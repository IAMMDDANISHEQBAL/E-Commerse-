package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.dto.GoogleLoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleOAuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern AUD_PATTERN = Pattern.compile("\"aud\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final boolean verifyToken;
    private final String clientId;

    public GoogleOAuthService(@Value("${app.google.verify-token:false}") boolean verifyToken,
                              @Value("${app.google.client-id:}") String clientId) {
        this.verifyToken = verifyToken;
        this.clientId = clientId;
    }

    public VerifiedGoogleUser verify(GoogleLoginRequest request) {
        if (!verifyToken) {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new RuntimeException("Google email is required in local OAuth mode");
            }
            return new VerifiedGoogleUser(request.getEmail(), request.getName());
        }

        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new RuntimeException("Google ID token is required");
        }
        if (clientId.isBlank()) {
            throw new RuntimeException("Google client id must be configured");
        }

        try {
            String token = URLEncoder.encode(request.getIdToken(), StandardCharsets.UTF_8);
            HttpRequest httpRequest = HttpRequest.newBuilder(
                            URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw new RuntimeException("Google token verification failed");
            }

            String body = response.body();
            String audience = extract(AUD_PATTERN, body);
            if (!clientId.equals(audience)) {
                throw new RuntimeException("Google token audience does not match this app");
            }

            String email = extract(EMAIL_PATTERN, body);
            if (email.isBlank()) {
                throw new RuntimeException("Google token did not include an email");
            }
            return new VerifiedGoogleUser(email, extract(NAME_PATTERN, body));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Unable to verify Google ID token", exception);
        }
    }

    private String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    public record VerifiedGoogleUser(String email, String name) {
    }
}
