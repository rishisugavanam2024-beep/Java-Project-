package com.resolvebid.config;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public record AppConfig(String environment, String databaseUrl, String databaseUser,
        String databasePassword, int port, boolean secureCookies,
        String adminUsername, String adminPassword) {

    public static AppConfig load() {
        Map<String, String> env = System.getenv();
        String mode = env.getOrDefault("APP_ENV", "development").toLowerCase(Locale.ROOT);
        boolean production = mode.equals("production");
        String localUrl = "jdbc:h2:file:" + Path.of("data", "resolvebid").toAbsolutePath()
                .toString().replace('\\', '/')
                + ";AUTO_SERVER=TRUE;MODE=MySQL";
        String adminPassword = env.get("ADMIN_PASSWORD");
        if (production && (adminPassword == null || adminPassword.length() < 12)) {
            throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters in production.");
        }
        if (adminPassword == null || adminPassword.isBlank()) adminPassword = "ChangeMe123!";
        return new AppConfig(
                mode,
                env.getOrDefault("DB_URL", localUrl),
                env.getOrDefault("DB_USER", "sa"),
                env.getOrDefault("DB_PASSWORD", ""),
                integer(env.get("PORT"), 8090),
                Boolean.parseBoolean(env.getOrDefault("SECURE_COOKIES", production ? "true" : "false")),
                env.getOrDefault("ADMIN_USERNAME", "admin"),
                adminPassword);
    }

    public boolean production() { return environment.equals("production"); }

    private static int integer(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value); }
        catch (NumberFormatException ex) { return fallback; }
    }
}
