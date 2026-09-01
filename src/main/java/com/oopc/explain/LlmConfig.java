package com.oopc.explain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * LLM configuration — bring your own key.
 *
 * Resolution order (first hit wins):
 *   1. path given via --config
 *   2. ./oop-checkup.json
 *   3. ~/.oop-checkup.json
 *   4. environment variables OOPC_API_KEY / OOPC_BASE_URL / OOPC_MODEL
 *
 * If nothing is found, {@link #enabled} is false and the tool runs entirely on
 * templates — offline, free, deterministic, and functionally complete.
 */
public final class LlmConfig {

    public final boolean enabled;
    public final String baseUrl;
    public final String apiKey;
    public final String model;
    public final int timeoutMs;
    /** Where the configuration came from, for diagnostics. */
    public final String origin;

    private LlmConfig(boolean enabled, String baseUrl, String apiKey,
                      String model, int timeoutMs, String origin) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.origin = origin;
    }

    public static LlmConfig disabled() {
        return new LlmConfig(false, null, null, null, 0, "not configured");
    }

    public static LlmConfig load(String explicitPath) {
        if (explicitPath != null) {
            LlmConfig c = fromFile(Paths.get(explicitPath));
            if (c != null) return c;
            return disabled();
        }
        LlmConfig c = fromFile(Paths.get("oop-checkup.json"));
        if (c != null) return c;

        String home = System.getProperty("user.home");
        if (home != null) {
            c = fromFile(Paths.get(home, ".oop-checkup.json"));
            if (c != null) return c;
        }
        return fromEnv();
    }

    private static LlmConfig fromFile(Path p) {
        if (p == null || !Files.isRegularFile(p)) return null;
        String json;
        try {
            json = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
        String key   = Json.stringField(json, "apiKey");
        String base  = Json.stringField(json, "baseUrl");
        String model = Json.stringField(json, "model");
        boolean on   = Json.boolField(json, "enabled", true);
        int timeout  = Json.intField(json, "timeoutMs", 15000);

        // Allow the key to live in an env var even when other settings are in the file.
        if (isBlank(key)) key = System.getenv("OOPC_API_KEY");

        if (!on || isBlank(key) || isBlank(base) || isBlank(model)) {
            return new LlmConfig(false, base, key, model, timeout,
                    p + " (incomplete or disabled)");
        }
        return new LlmConfig(true, trimSlash(base), key, model, timeout, p.toString());
    }

    private static LlmConfig fromEnv() {
        String key   = System.getenv("OOPC_API_KEY");
        String base  = System.getenv("OOPC_BASE_URL");
        String model = System.getenv("OOPC_MODEL");
        if (isBlank(key) || isBlank(base) || isBlank(model)) {
            return disabled();
        }
        return new LlmConfig(true, trimSlash(base), key, model, 15000, "environment variables");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
