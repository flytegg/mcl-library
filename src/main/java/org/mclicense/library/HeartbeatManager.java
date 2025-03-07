package org.mclicense.library;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.mclicense.library.Constants.HEARTBEAT_URL;
import static org.mclicense.library.Constants.TIMEOUT_MS;

class HeartbeatManager {
    public static boolean running;

    private static String pluginId;
    private static String key;
    private static String sessionId;

    private static ScheduledTask foliaTask;
    private static BukkitTask bukkitTask;

    private static boolean listenerRegistered = false;

    protected static void startHeartbeat(JavaPlugin plugin, String pluginId, String key, String sessionId) {
        if (running) {
            killHeartbeat();
        }

        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(new ShutdownListener(plugin), plugin);
            listenerRegistered = true;
        }

        HeartbeatManager.pluginId = pluginId;
        HeartbeatManager.key = key;
        HeartbeatManager.sessionId = sessionId;

        if (Constants.IS_FOLIA) {
            foliaTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> sendHeartbeat(false), Constants.HEARTBEAT_INTERVAL_SECONDS, Constants.HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } else {
            bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> sendHeartbeat(false), Constants.HEARTBEAT_INTERVAL_SECONDS * 20, Constants.HEARTBEAT_INTERVAL_SECONDS * 20);
        }

        running = true;
    }

    protected static void sendHeartbeat(boolean isShutdown) {
        try {
            URL url = new URL(String.format(HEARTBEAT_URL, pluginId, key));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);

            // Create JSON payload
            JSONObject payload = new JSONObject();
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

    private static void killHeartbeat() {
        // Send shutdown heartbeat
        sendHeartbeat(true);

        // Safely cancel tasks
        if (Constants.IS_FOLIA && foliaTask != null) {
            foliaTask.cancel();
            foliaTask = null;
        } else if (!Constants.IS_FOLIA && bukkitTask != null) {
            bukkitTask.cancel();
            bukkitTask = null;
        }

        running = false;
    }
}