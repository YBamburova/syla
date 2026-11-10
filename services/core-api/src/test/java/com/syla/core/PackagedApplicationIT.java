package com.syla.core;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.*;
import org.testcontainers.utility.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/** Real provider login, packaged core, and PostgreSQL. No fake auth or in-memory domain database. */
class PackagedApplicationIT {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(75);
    private static final Pattern PORT = Pattern.compile("Tomcat started on port ([0-9]+)");
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String ADMIN_PASSWORD = UUID.randomUUID().toString();
    private static final String SERVICE_SECRET = UUID.randomUUID().toString();
    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.8.2@sha256:723b1d6e254c0fc96d5cffec860c7e0744d6a3277822b865bb538cf891575e41";
    private static final Network NETWORK = Network.newNetwork();
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse(
            "pgvector/pgvector:0.8.7-pg17-trixie@sha256:01215e09bd4bd582d20230e025b6c971deeeffee49ceef1aae167e839cb62a7e")
            .asCompatibleSubstituteFor("postgres")).withNetwork(NETWORK).withNetworkAliases("postgres")
            .withInitScript("provider-db.sql").withStartupTimeout(Duration.ofSeconds(60));
    private static GenericContainer<?> provider;
    private static Process process;
    private static URI base;
    private static String issuer;
    private static Path realm;

    @BeforeAll
    static void start() throws Exception {
        POSTGRES.start();
        int port;
        try (var socket = new java.net.ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
        var definition = (ObjectNode) JSON.readTree(Files.readString(Path.of("dev/syla-realm.json"))
                .replace("${CORE_ORIGIN}", "http://127.0.0.1:" + port)
                .replace("${KEYCLOAK_SERVICE_SECRET}", SERVICE_SECRET));
        var users = (com.fasterxml.jackson.databind.node.ArrayNode) definition.get("users");
        for (var name : List.of("crud", "other", "validation", "concurrent", "consent", "deleting", "racing")) {
            var user = users.addObject();
            user.put("username", name).put("enabled", true).put("emailVerified", true)
                    .put("firstName", "Fixture").put("lastName", name).put("email", name + "@example.invalid");
            user.putArray("credentials").addObject().put("type", "password").put("value", PASSWORD).put("temporary", false);
        }
        realm = Files.createTempFile("syla-fixture-realm-", ".json");
        Files.writeString(realm, JSON.writeValueAsString(definition));
        provider = new GenericContainer<>(KEYCLOAK_IMAGE).withNetwork(NETWORK).withExposedPorts(8080)
                .withEnv("KC_DB", "postgres").withEnv("KC_DB_URL", "jdbc:postgresql://postgres:5432/provider")
                .withEnv("KC_DB_USERNAME", "keycloak").withEnv("KC_DB_PASSWORD", "provider-fixture-only")
                .withEnv("JAVA_OPTS_KC_HEAP", "-Xms128m -Xmx512m")
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "fixture-admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN_PASSWORD)
                .withCopyToContainer(MountableFile.forHostPath(realm, 0644), "/opt/keycloak/data/import/syla-realm.json")
                .withCommand("start-dev", "--import-realm", "--hostname-strict=false")
                .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forHttp("/realms/syla/.well-known/openid-configuration"))
                .withStartupTimeout(Duration.ofSeconds(120));
        provider.start();
        issuer = "http://localhost:" + provider.getMappedPort(8080) + "/realms/syla";
        var log = Path.of("target", "smoke.log");
        var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx384m", "-jar", "target/core-api.jar", "--server.port=" + port, "--server.address=127.0.0.1");
        builder.environment().put("DB_URL", POSTGRES.getJdbcUrl());
        builder.environment().put("DB_USER", POSTGRES.getUsername());
        builder.environment().put("DB_PASSWORD", POSTGRES.getPassword());
        builder.environment().put("OIDC_ISSUER_URI", issuer);
        builder.environment().put("KEYCLOAK_SERVICE_SECRET", SERVICE_SECRET);
        builder.environment().put("SYLA_IDENTITY_CLEANUP_DELAY_MS", "1000");
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        process = builder.start();
        var deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
        while (process.isAlive() && System.nanoTime() < deadline) {
            if (PORT.matcher(Files.readString(log)).find()) {
                base = URI.create("http://127.0.0.1:" + port);
                return;
            }
            Thread.sleep(100);
        }
        fail("Packaged app did not start. Log:%n%s", Files.readString(log));
    }

    @AfterAll
    static void stop() throws Exception {
        if (process != null) {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
        if (provider != null) provider.stop();
        POSTGRES.stop(); NETWORK.close();
        if (realm != null) Files.deleteIfExists(realm);
    }

    @Test
    @Timeout(30)
    void healthContractsAndAnonymousAccess() throws Exception {
        var anonymous = new Client();
        var health = anonymous.get("/api/v1/health");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(health.body())).isEqualTo(JSON.readTree("{\"status\":\"UP\"}"));
        assertThat(anonymous.get("/api/v1/workouts").statusCode()).isEqualTo(401);
        assertThat(anonymous.send("POST", "/api/v1/workouts", fixture("strength"), Map.of()).statusCode()).isEqualTo(401);
        assertThat(anonymous.send("POST", "/api/v1/health", "", Map.of()).statusCode()).isEqualTo(405);
        assertThat(anonymous.get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
        var api = anonymous.get("/v3/api-docs");
        assertThat(api.statusCode()).isEqualTo(200);
        var document = JSON.readTree(api.body());
        assertThat(document.path("paths").has("/api/v1/workouts")).isTrue();
        assertThat(document.path("components").path("schemas").has("WorkoutInput")).isTrue();
        Files.writeString(Path.of("target", "openapi.json"), api.body());
        for (var contract : List.of("health", "workouts", "identity", "privacy")) {
            assertThat(anonymous.get("/openapi/" + contract + ".yaml").body())
                    .isEqualTo(Files.readString(Path.of("src/main/openapi/" + contract + ".yaml")));
        }
        try (var connection = db(); var statement = connection.createStatement();
             var results = statement.executeQuery("select count(*) from information_schema.schemata where schema_name in ('identity','workouts','privacy')")) {
            assertThat(results.next()).isTrue(); assertThat(results.getInt(1)).isEqualTo(3);
        }
    }

    @Test
    @Timeout(60)
    void ownedCrudRevisionAndReplaySurviveRealPersistence() throws Exception {
        try (var owner = Client.login("crud"); var other = Client.login("other")) {
            owner.consent(true); other.consent(true);
            var input = fixture("strength"); var key = key(Instant.now());
            var created = owner.create(input, key); assertThat(created.statusCode()).isEqualTo(201);
            assertThat(created.headers().firstValue("cache-control").orElse("")).contains("no-store");
            var view = JSON.readTree(created.body()); var path = "/api/v1/workouts/" + view.path("id").asText();
            assertThat(view.at("/workout/section/actualSets/3/load/amount").decimalValue().toPlainString()).isEqualTo("62.5");
            assertThat(owner.get(path).body()).isEqualTo(created.body());
            assertThat(owner.create(input, key).body()).isEqualTo(created.body());
            assertThat(other.get(path).statusCode()).isEqualTo(404);
            assertThat(other.sendWithCsrf("PUT", path, input, Map.of("If-Match", "\"0\"")).statusCode()).isEqualTo(404);
            assertThat(owner.send("PUT", path, input, Map.of("If-Match", "\"0\"")).statusCode()).isEqualTo(403);
            assertThat(owner.sendWithCsrf("PUT", path, input, Map.of()).statusCode()).isEqualTo(428);
            var changed = (ObjectNode) JSON.readTree(input); changed.put("title", "Edited");
            assertThat(owner.create(changed.toString(), key).statusCode()).isEqualTo(409);
            var update = owner.sendWithCsrf("PUT", path, changed.toString(), Map.of("If-Match", "\"0\""));
            assertThat(update.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(update.body()).path("revision").asLong()).isEqualTo(1);
            assertThat(JSON.readTree(update.body()).path("sectionId")).isEqualTo(view.path("sectionId"));
            assertThat(owner.sendWithCsrf("PUT", path, input, Map.of("If-Match", "\"0\"")).statusCode()).isEqualTo(409);
            assertThat(owner.create(input, key).statusCode()).isEqualTo(409);
            assertThat(owner.sendWithCsrf("DELETE", path, null, Map.of("If-Match", "\"0\"")).statusCode()).isEqualTo(409);
            assertThat(owner.sendWithCsrf("DELETE", path, null, Map.of("If-Match", "\"1\"")).statusCode()).isEqualTo(204);
            assertThat(owner.get(path).statusCode()).isEqualTo(404);
            assertThat(owner.create(input, key).statusCode()).isEqualTo(409);
            assertThat(owner.sendWithCsrf("DELETE", path, null, Map.of("If-Match", "\"1\"")).statusCode()).isEqualTo(404);
        }
    }

    @Test
    @Timeout(60)
    void scoresAreTypedNormalizedAndStrict() throws Exception {
        try (var client = Client.login("validation")) {
            client.consent(true);
            var cindy = (ObjectNode) JSON.readTree(fixture("amrap"));
            assertThat(JSON.readTree(client.create(cindy.toString(), key(Instant.now())).body()).path("totalReps").asLong()).isEqualTo(368);
            ((ObjectNode)cindy.at("/section/result")).put("additionalReps", 38);
            var normalized = client.create(cindy.toString(), key(Instant.now()));
            assertThat(normalized.statusCode()).isEqualTo(201);
            assertThat(JSON.readTree(normalized.body()).at("/workout/section/result/rounds").asInt()).isEqualTo(13);
            ((ObjectNode)cindy.at("/section/result")).put("rounds", 12.8);
            assertThat(client.create(cindy.toString(), key(Instant.now())).statusCode()).isEqualTo(400);
            var fran = (ObjectNode) JSON.readTree(fixture("for-time"));
            assertThat(client.create(fran.toString(), key(Instant.now())).statusCode()).isEqualTo(201);
            ((ObjectNode)fran.path("section")).put("capSeconds", 600).set("result", JSON.readTree("{\"kind\":\"CAPPED\",\"completedReps\":81}"));
            assertThat(client.create(fran.toString(), key(Instant.now())).statusCode()).isEqualTo(201);
            ((ObjectNode)fran.at("/section/result")).put("completedReps", 90);
            assertThat(client.create(fran.toString(), key(Instant.now())).statusCode()).isEqualTo(400);
            var strength = (ObjectNode) JSON.readTree(fixture("strength"));
            strength.put("trainingDate", "2100-01-01");
            assertThat(client.create(strength.toString(), key(Instant.now())).statusCode()).isEqualTo(400);
            strength.put("trainingDate", "2026-01-01").put("ownerId", UUID.randomUUID().toString());
            assertThat(client.create(strength.toString(), key(Instant.now())).statusCode()).isEqualTo(400);
            assertThat(client.create(fixture("strength"), key(Instant.now().minus(Duration.ofDays(2)))).statusCode()).isEqualTo(400);
            assertThat(client.create(fixture("strength"), UUID.randomUUID()).statusCode()).isEqualTo(400);
        }
    }

    @Test
    @Timeout(60)
    void concurrentSameKeySavesOneWorkoutAndHistoryPagesStayScoped() throws Exception {
        try (var client = Client.login("concurrent")) {
            client.consent(true); var input = fixture("amrap"); var key = key(Instant.now());
            try (var threads = Executors.newFixedThreadPool(2)) {
                var first = threads.submit(() -> client.create(input, key));
                var second = threads.submit(() -> client.create(input, key));
                var one = first.get(20, TimeUnit.SECONDS); var two = second.get(20, TimeUnit.SECONDS);
                assertThat(one.statusCode()).isEqualTo(201); assertThat(two.statusCode()).isEqualTo(201);
                assertThat(one.body()).isEqualTo(two.body());
            }
            var list = JSON.readTree(client.get("/api/v1/workouts").body());
            assertThat(list.path("items").size()).isEqualTo(1);
            client.create(fixture("strength"), key(Instant.now()));
            var page = JSON.readTree(client.get("/api/v1/workouts?limit=1").body());
            var cursor = URLEncoder.encode(page.path("nextCursor").asText(), StandardCharsets.UTF_8);
            var next = JSON.readTree(client.get("/api/v1/workouts?limit=1&cursor=" + cursor).body());
            assertThat(next.path("items").size()).isEqualTo(1);
            assertThat(next.at("/items/0/id")).isNotEqualTo(page.at("/items/0/id"));
        }
    }

    @Test
    @Timeout(60)
    void consentWithdrawalBlocksTrainingButKeepsPrivateExportAvailable() throws Exception {
        try (var client = Client.login("consent")) {
            assertThat(client.create(fixture("strength"), key(Instant.now())).statusCode()).isEqualTo(403);
            client.consent(true); client.create(fixture("strength"), key(Instant.now()));
            client.consent(false);
            assertThat(client.get("/api/v1/workouts").statusCode()).isEqualTo(403);
            var export = client.get("/api/v1/privacy/export");
            assertThat(export.statusCode()).isEqualTo(200);
            var data = JSON.readTree(export.body());
            assertThat(data.path("workouts").size()).isEqualTo(1);
            assertThat(data.path("consents").size()).isEqualTo(2);
            assertThat(data.path("account").path("id")).isEqualTo(JSON.readTree(client.get("/api/v1/account").body()).path("id"));
        }
    }

    @Test
    @Timeout(60)
    void deletionAndSaveCannotLeaveTrainingDataBehind() throws Exception {
        try (var client = Client.login("racing")) {
            client.consent(true); client.create(fixture("strength"), key(Instant.now()));
            var actor = UUID.fromString(JSON.readTree(client.get("/api/v1/account").body()).path("id").asText());
            var start = new CountDownLatch(1);
            try (var barrier = db(); var lock = barrier.prepareStatement("select id from identity.accounts where id = ? for update");
                 var threads = Executors.newFixedThreadPool(2)) {
                barrier.setAutoCommit(false); lock.setObject(1, actor); lock.execute();
                var save = threads.submit(() -> { start.await(); return client.create(fixture("amrap"), key(Instant.now())); });
                var delete = threads.submit(() -> { start.await(); return client.sendWithCsrf("DELETE", "/api/v1/account", null, Map.of()); });
                start.countDown(); Thread.sleep(150); barrier.commit();
                assertThat(save.get(20, TimeUnit.SECONDS).statusCode()).isIn(201, 403);
                var removed = delete.get(20, TimeUnit.SECONDS);
                assertThat(removed.statusCode()).isEqualTo(200);
                assertThat(JSON.readTree(removed.body()).path("state").asText()).isEqualTo("COMPLETED");
            }
            try (var connection = db()) {
                for (var table : List.of("workouts.sessions", "workouts.create_requests", "privacy.consents")) {
                    try (var query = connection.prepareStatement("select count(*) from " + table + " where "
                            + (table.endsWith("sessions") ? "owner_id" : "actor_id") + " = ?")) {
                        query.setObject(1, actor);
                        try (var result = query.executeQuery()) { result.next(); assertThat(result.getInt(1)).isZero(); }
                    }
                }
            }
            assertThat(client.get("/api/v1/workouts").statusCode()).isEqualTo(401);
        }
    }

    private static java.sql.Connection db() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
    private static String fixture(String name) throws IOException { return Files.readString(Path.of("src/test/resources/" + name + ".json")); }
    private static UUID key(Instant instant) {
        var random = UUID.randomUUID();
        return new UUID((instant.toEpochMilli() << 16) | 0x7000L | (random.getMostSignificantBits() & 0xfffL), random.getLeastSignificantBits());
    }

    private static class Client implements AutoCloseable {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(3)).build();
        private String csrf;
        static Client login(String username) throws Exception {
            var client = new Client();
            var form = client.get("/oauth2/authorization/keycloak");
            assertThat(form.statusCode()).isEqualTo(200);
            var match = Pattern.compile("<form[^>]*action=\"([^\"]+)\"").matcher(form.body());
            assertThat(match.find()).as("Keycloak login form").isTrue();
            // Send RFC 6265 cookies rather than HttpCookie's legacy quoted Version=1 format.
            client.cookies.getCookieStore().getCookies().forEach(cookie -> {
                cookie.setVersion(0);
                // Browsers allow Secure cookies on trustworthy localhost HTTP; Java CookieManager does not.
                if (cookie.getDomain().equals("localhost.local")) cookie.setSecure(false);
            });
            var action = URI.create(match.group(1).replace("&amp;", "&"));
            var payload = "username=" + username + "&password=" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8);
            var loggedIn = client.http.send(HttpRequest.newBuilder(action).timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", action.getScheme() + "://" + action.getAuthority())
                    .header("Referer", form.uri().toString()).POST(HttpRequest.BodyPublishers.ofString(payload)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(loggedIn.statusCode()).as("Keycloak login callback").isEqualTo(200);
            assertThat(client.get("/api/v1/account").statusCode()).as("verified OIDC login for " + username).isEqualTo(200);
            client.csrf = JSON.readTree(client.get("/api/v1/csrf").body()).path("token").asText();
            return client;
        }
        void consent(boolean granted) throws Exception {
            assertThat(sendWithCsrf("PUT", "/api/v1/consents/training", "{\"version\":\"v1\",\"granted\":" + granted + "}", Map.of()).statusCode()).isEqualTo(200);
        }
        HttpResponse<String> get(String path) throws Exception { return send("GET", path, null, Map.of()); }
        HttpResponse<String> create(String body, UUID key) throws Exception { return sendWithCsrf("POST", "/api/v1/workouts", body, Map.of("Idempotency-Key", key.toString())); }
        HttpResponse<String> sendWithCsrf(String method, String path, String body, Map<String,String> headers) throws Exception {
            var all = new HashMap<>(headers); all.put("X-XSRF-TOKEN", csrf); return send(method, path, body, all);
        }
        HttpResponse<String> send(String method, String path, String body, Map<String,String> headers) throws Exception {
            var request = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(15));
            if (body != null) request.header("Content-Type", "application/json");
            headers.forEach(request::header);
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
        @Override public void close() { http.close(); }
    }
}
