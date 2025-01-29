package org.mclicense.library;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mclicense.library.Constants.HEARTBEAT_URL;
import static org.mclicense.library.Constants.TIMEOUT_MS;

class HeartbeatManager {
    private static String pluginId;
    private static String licenseKey;
    private static String sessionId;

    protected static void startHeartbeat(JavaPlugin plugin, String pluginId, String licenseKey, String sessionId) {
        plugin.getServer().getPluginManager().registerEvents(new ShutdownListener(plugin), plugin);

        HeartbeatManager.pluginId = pluginId;
        HeartbeatManager.licenseKey = licenseKey;
        HeartbeatManager.sessionId = sessionId;

        if (Constants.IS_FOLIA) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> sendHeartbeat(false), Constants.HEARTBEAT_INTERVAL_SECONDS, Constants.HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> sendHeartbeat(false), Constants.HEARTBEAT_INTERVAL_SECONDS * 20, Constants.HEARTBEAT_INTERVAL_SECONDS * 20);
        }
    }

    protected static void sendHeartbeat(boolean isShutdown) {
        try {
            URL url = new URL(String.format(HEARTBEAT_URL, pluginId, licenseKey));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);

            // Create JSON payload
            JSONObject payload = new JSONObject();
            // This really should be changed to sessionId, but will require a lot of backend changes
            payload.put("serverIp", sessionId);
            if (isShutdown) {
                payload.put("shutdown", true);
            }

            // Send the heartbeat
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (connection.getResponseCode() != 200) {
                // Ignore
            }
        } catch (Exception x) {

        }
    }
}