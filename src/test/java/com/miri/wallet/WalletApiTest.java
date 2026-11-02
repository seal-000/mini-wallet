package com.miri.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletApiTest {
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void usersSeeOnlyTheirOwnAccountsAndCannotSpendFromOthers() throws Exception {
        var aliceEmail = "alice-" + UUID.randomUUID() + "@wallet.test";
        var bobEmail = "bob-" + UUID.randomUUID() + "@wallet.test";
        var aliceToken = register(aliceEmail);
        var bobToken = register(bobEmail);
        var aliceAccount = usdWallet(aliceToken);
        var bobAccount = usdWallet(bobToken);

        var aliceAccounts = send("GET", "/api/accounts", aliceToken, null, null).body();
        var reconcile = send("GET", "/api/ledger/reconcile", aliceToken, null, null).body();
        assertTrue(reconcile.contains("\"balanced\":true") && !reconcile.contains("\"balanced\":false"));
        assertEquals(RegistrationService.SUPPORTED_CURRENCIES.size(), countOccurrences(aliceAccounts, "\"id\""));
        for (var currency : RegistrationService.SUPPORTED_CURRENCIES) {
            assertTrue(aliceAccounts.contains("\"currency\":\"" + currency + "\""));
        }
        assertTrue(send("POST", "/api/accounts", aliceToken, "{\"currency\":\"usd\"}", null).statusCode() >= 400);

        var stolen = transfer(aliceToken, bobAccount.id(), aliceEmail, "10.0000");
        assertEquals(403, stolen.statusCode());

        var legitimate = transfer(aliceToken, aliceAccount.id(), bobEmail, "10.0000");
        assertEquals(200, legitimate.statusCode());

        var noWallet = transfer(aliceToken, aliceAccount.id(), "nobody-" + UUID.randomUUID() + "@wallet.test", "1.0000");
        assertEquals(404, noWallet.statusCode());

        var aliceHistory = send("GET", "/api/transfers", aliceToken, null, null);
        assertEquals(200, aliceHistory.statusCode());
        assertTrue(aliceHistory.body().contains("\"amount\":10.0000")
                || aliceHistory.body().contains("\"amount\":10"));

        assertEquals(401, send("GET", "/api/accounts", null, null, null).statusCode());

        var debits = send("GET", "/api/statements?type=debit&size=1", aliceToken, null, null).body();
        assertTrue(debits.contains("\"totalItems\":1") && debits.contains("\"amount\":-10"), debits);
        assertTrue(debits.contains("Transfer to account ending"), debits);
        var all = send("GET", "/api/statements?size=2&page=0", aliceToken, null, null).body();
        assertTrue(all.contains("\"totalItems\":5") && all.contains("\"totalPages\":3"), all);
        var csv = send("GET", "/api/statements/export.csv?type=credit", bobToken, null, null);
        assertEquals(200, csv.statusCode());
        assertTrue(csv.headers().firstValue("Content-Disposition").orElse("").contains("wallet-statement.csv"));
        assertTrue(csv.body().startsWith("Date,Description,Type,Amount,Currency")
                && csv.body().contains("\"Transfer from account ending"), csv.body());
        assertEquals(404, send("GET", "/api/statements?accountId=" + bobAccount.id(), aliceToken, null, null)
                .statusCode());
        assertEquals(401, send("GET", "/api/statements", null, null, null).statusCode());
    }

    private String register(String email) throws Exception {
        var response = send("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"password123\"}", null);
        assertEquals(201, response.statusCode());
        var matcher = TOKEN.matcher(response.body());
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private record Created(long id, String number) {
    }

    private Created usdWallet(String token) throws Exception {
        var body = send("GET", "/api/accounts", token, null, null).body();
        var matcher = Pattern.compile(
                "\\{\"id\":(\\d+),\"currency\":\"USD\",\"balance\":[^,]+,\"accountNumber\":\"(\\d{10})\"\\}")
                .matcher(body);
        assertTrue(matcher.find(), body);
        return new Created(Long.parseLong(matcher.group(1)), matcher.group(2));
    }

    private HttpResponse<String> transfer(String token, long from, String toEmail, String amount)
            throws Exception {
        return send("POST", "/api/transfers", token,
                "{\"fromAccountId\":" + from + ",\"toEmail\":\"" + toEmail
                        + "\",\"amount\":" + amount + "}",
                UUID.randomUUID().toString());
    }

    private HttpResponse<String> send(String method, String path, String token, String body,
                                      String idempotencyKey) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static int countOccurrences(String text, String needle) {
        return text.split(Pattern.quote(needle), -1).length - 1;
    }
}
