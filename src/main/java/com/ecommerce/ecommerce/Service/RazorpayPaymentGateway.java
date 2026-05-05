package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.dto.PaymentRequest;
import com.ecommerce.ecommerce.entity.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String keyId;
    private final String keySecret;
    private final String currency;

    public RazorpayPaymentGateway(@Value("${app.razorpay.key-id:}") String keyId,
                                  @Value("${app.razorpay.key-secret:}") String keySecret,
                                  @Value("${app.razorpay.currency:INR}") String currency) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.currency = currency;
    }

    @Override
    public String createProviderOrder(Payment payment) {
        assertConfigured();
        long amountInSubunits = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        String body = """
                {
                  "amount": %d,
                  "currency": "%s",
                  "receipt": "order_%d_payment_%d"
                }
                """.formatted(amountInSubunits, currency, payment.getOrder().getId(), payment.getId());

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + basicAuth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw new RuntimeException("Razorpay order creation failed: " + response.body());
            }
            return extractId(response.body());
        } catch (Exception exception) {
            throw new RuntimeException("Unable to create Razorpay order", exception);
        }
    }

    @Override
    public String verifyAndCapture(Payment payment, PaymentRequest request) {
        assertConfigured();
        if (request.getRazorpayOrderId() == null || request.getRazorpayPaymentId() == null
                || request.getRazorpaySignature() == null) {
            throw new RuntimeException("Razorpay order id, payment id and signature are required");
        }
        if (!request.getRazorpayOrderId().equals(payment.getProviderReference())) {
            throw new RuntimeException("Razorpay order id does not match this order");
        }

        String generatedSignature = hmacSha256(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                keySecret
        );
        if (!generatedSignature.equals(request.getRazorpaySignature())) {
            throw new RuntimeException("Invalid Razorpay payment signature");
        }
        return request.getRazorpayPaymentId();
    }

    private String extractId(String responseBody) {
        Matcher matcher = ID_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new RuntimeException("Razorpay response did not contain an order id");
        }
        return matcher.group(1);
    }

    private String basicAuth() {
        return Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new RuntimeException("Unable to verify Razorpay signature", exception);
        }
    }

    private void assertConfigured() {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new RuntimeException("Razorpay key id and secret must be configured");
        }
    }
}
