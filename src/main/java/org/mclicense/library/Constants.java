package org.mclicense.library;

import java.util.logging.Logger;

class Constants {
    // Logger
    static final Logger LOGGER = Logger.getLogger("MCLicense");

    // API Endpoints
    static final String API_BASE_URL = "https://api.mclicense.org";
    static final String API_URL = API_BASE_URL + "/validate/%s/%s";  // For license validation (pluginId, key)
    static final String HEARTBEAT_URL = API_BASE_URL + "/heartbeat/%s/%s";  // For heartbeat (pluginId, key)

    // Connection Settings
    static final int TIMEOUT_MS = 5000;  // 5 second timeout for HTTP requests

    // Heartbeat Settings
    static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    // RSA Public Key for signature verification
    static final String PUBLIC_KEY =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArhw7oQaOrgCzUxDi5D+N\n" +
            "TH9te0JYB1EvW7CEI40+n2drmHJ4+g0CXXYjJc5LyuePskSUPnnHf3UkRvi1GTUd\n" +
            "6Bqi2Jpeu+qbBfm3hg6rcyLUWo8d5MrBQDbVcIvKmQNegTaJGxFRpEFR9XOeHI1g\n" +
            "4dfF+hOfy+1rbEF4p4fgiz0irtKv8l3uSPOKVoEjTL9xnZx4MU5rIsn6W3jee04q\n" +
            "ESPJpCg8nmmZSuJ+9EzzoLnLnUc2/sBuqJ/jexpNfMrXIR11+L8DFqei7J2M7aKi\n" +
            "0KZvQNIqzqTPBCR9VLZPBjFu6cYT/E/WUjjFROuRhi+7Xsa6tKLqoiO4VwJSrn5L\n" +
            "zwIDAQAB\n" +
            "-----END PUBLIC KEY-----";

    // Folia Check
    static final boolean IS_FOLIA = isRunningFolia();

    private static boolean isRunningFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}