package org.mclicense.library;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static org.mclicense.library.Constants.*;

public class TempLicenseManager {
    private static ScheduledTask foliaTask;
    private static BukkitTask bukkitTask;

    protected static void startPolling(JavaPlugin plugin, Long deadline, String tempLicense) {
        if (Constants.IS_FOLIA) {
            foliaTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> poll(plugin, deadline, tempLicense), 60, 60, TimeUnit.SECONDS);
        } else {
            bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> poll(plugin, deadline, tempLicense), 60 * 20, 60 * 20);
        }
    }

    private static boolean poll(JavaPlugin plugin, Long deadline, String tempLicense) {
        try {
            URL url = new URL(String.format(TEMP_LICENSE_URL, tempLicense));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            String response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getResponseCode() >= 400 ?
                    connection.getErrorStream() : connection.getInputStream()))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                response = responseBuilder.toString();
            }

            // Permanent key granted
            if (connection.getResponseCode() == 200) {
                stopPolling();

                String permanentKey = new JSONObject(response).getString("permanentKey");

                Files.write(new File(plugin.getDataFolder(), "mclicense.txt").toPath(), permanentKey.getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Your license for " + plugin.getName() + " was validated through PayPal, and your permanent key has been set in the mclicense.txt! Keep it safe, as you will have to contact the author if you lose it.");
                return true;
            }
        } catch (Exception x) {

        }

        // 3 hour temporary deadline surpassed, no permanent key was granted
        if (System.currentTimeMillis() > deadline) {
            stopPolling();
            Bukkit.getPluginManager().disablePlugin(plugin);

            LOGGER.info("Your temporary license for " + plugin.getName() + " has expired as no valid purchase was found on PayPal. If this is a mistake, contact the author.");
        }

        return false;
    }

    private static void stopPolling() {
        if (Constants.IS_FOLIA) {
            foliaTask.cancel();
        } else {
            bukkitTask.cancel();
        }
    }
}
