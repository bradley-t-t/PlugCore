package io.plugcore.plugCore.config;

public class DatabaseConfig {
    private static final String BASE_URL = "https://db.plugcore.io/functions/v1";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd1amd0anFxdXJpbGRxdXJwZmZoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM5MTkxOTAsImV4cCI6MjA3OTQ5NTE5MH0.9jd6izem9wvp9RgYvlzgLhjSAiRxfsCfTxuIQHOunZc";

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getAnonKey() {
        return ANON_KEY;
    }
}

