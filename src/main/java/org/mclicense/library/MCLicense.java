package org.mclicense.library;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class MCLicense {
    /**
     * Internal function that handles the actual license validation with the MCLicense server.
     *
     * @param plugin The JavaPlugin instance requesting validation
     * @param pluginId The unique identifier assigned to your plugin by MCLicense
     * @param key The license key to validate
     * @return true if the license is valid and active, false otherwise
     */
    private static boolean validateLicenseWithServer(JavaPlugin plugin, String pluginId, String key, File licenseFile) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String nonce = UUID.randomUUID().toString();

            // Properly encode all URL components
            String encodedPluginId = URLEncoder.encode(pluginId, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            URL url = new URL(String.format(Constants.API_URL, encodedPluginId, encodedKey) +
                    "?serverIp=" + sessionId +
                    "&nonce=" + nonce);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(Constants.TIMEOUT_MS);
            connection.setReadTimeout(Constants.TIMEOUT_MS);

            // Read the response from the server
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

            // Reject if the response code is not 200
            if (connection.getResponseCode() != 200) {
                try {
                    Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (" + new JSONObject(response).getString("message") + ")");
                } catch (Exception e) {
                    Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (Server error)");
                }
                return false;
            }

            JSONObject responseJson = new JSONObject(response);

            // Verify nonce is what was sent
            if (!responseJson.getString("nonce").equals(nonce)) {
                Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (Nonce mismatch)");
                return false;
            }

            // Verify key and pluginId are what was sent
            if ((!key.startsWith("sptemp_") && !responseJson.getString("key").equals(key)) || !responseJson.getString("pluginId").equals(pluginId)) {
                Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (Key or pluginId mismatch)");
                return false;
            }

            // Verify the response signature
            String signature = responseJson.getString("signature");

            JSONObject dataToVerify = new JSONObject();
            dataToVerify.put("key", responseJson.getString("key"));
            dataToVerify.put("pluginId", responseJson.getString("pluginId"));
            dataToVerify.put("status", responseJson.getString("status"));
            dataToVerify.put("message", responseJson.getString("message"));
            dataToVerify.put("nonce", responseJson.getString("nonce"));

            String data = dataToVerify.toString();

            String publicKeyPEM = Constants.PUBLIC_KEY
                    .replace("-----BEGIN PUBLIC KEY-----\n", "")
                    .replace("\n-----END PUBLIC KEY-----", "")
                    .replaceAll("\n", "");

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));

            boolean isValid = sig.verify(Base64.getDecoder().decode(signature));
            if (!isValid) {
                Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (Signature mismatch)");
                return false;
            }

            HeartbeatManager.startHeartbeat(plugin, pluginId, key, sessionId);

            // Handle temporary Spigot license
            if (key.startsWith("sptemp_")) {
                String returnedKey = responseJson.getString("key");
                if (!returnedKey.equals(key)) {
                    Files.write(licenseFile.toPath(), returnedKey.getBytes(StandardCharsets.UTF_8));
                    Constants.LOGGER.info("Your license for " + plugin.getName() + " was validated through PayPal, and your permanent key has been set in the mclicense.txt! Keep it safe, as you will have to contact the author if you lose it.");
                    return true;
                }

                Long deadline = Long.valueOf(responseJson.getString("message"));
                TempLicenseManager.startPolling(plugin, pluginId, deadline, key);

                Constants.LOGGER.info("License validation succeeded for " + plugin.getName() + "! Your license is temporary while we verify PayPal logs, the permanent one will soon be placed in your mclicense.txt automatically.");
                return true;
            }

            Constants.LOGGER.info("License validation succeeded for " + plugin.getName() + "!");
            return true;
        } catch (Exception e) {
            Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (System error)");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validates a license key with the MCLicense validation server.
     * <p>
     * This function performs several checks:
     * <ul>
     *     <li>Verifies the existence and content of the license file</li>
     *     <li>Validates the license key with the remote server</li>
     *     <li>Verifies the response signature for security</li>
     *     <li>Ensures the response matches the requested plugin and key</li>
     *     <li>Checks if the license is valid and not expired, reached max IPs, etc</li>
     * </ul>
     *
     * The license key should be placed in a file named 'mclicense.txt' in the plugin's data folder by the user, or be hardcoded by a marketplace.
     *
     * @param plugin The JavaPlugin instance requesting validation
     * @param pluginId The unique identifier assigned to your plugin by MCLicense
     * @return true if the license is valid and active, false otherwise
     */
    public static boolean validateKey(JavaPlugin plugin, String pluginId) {
        try {
            // Check if license file exists or create it
            File licenseFile = new File(plugin.getDataFolder(), "mclicense.txt");
            String fileContent = "";
            if (!licenseFile.exists()) {
                plugin.getDataFolder().mkdirs();
                licenseFile.createNewFile();
            } else {
                fileContent = new String(Files.readAllBytes(Paths.get(licenseFile.getPath())), StandardCharsets.UTF_8).trim();
            }

            // Read the license key from the file
            String key = fileContent;
            if (key.isEmpty()) {
                // Assuming first run, use hardcoded if exists, else prompt
                String hardcodedKey = MarketplaceProvider.getHardcodedLicense();
                if (hardcodedKey != null) {
                    key = hardcodedKey;
                } else {
                    Constants.LOGGER.info("License key is empty for " + plugin.getName() + "! Place your key in the 'mclicense.txt' file in the plugin folder and restart the server.");
                    return false;
                }
            }

            boolean isValid = validateLicenseWithServer(plugin, pluginId, key, licenseFile);

            // First run, key in file not equal to key in jar
            if (isValid && !key.equals(fileContent)) {
                Files.write(licenseFile.toPath(), key.getBytes(StandardCharsets.UTF_8));
            }

            return isValid;
        } catch (Exception e) {
            Constants.LOGGER.info("License validation failed for " + plugin.getName() + " (System error)");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Writes a license key to the license file and validates it immediately.
     *
     * @param plugin The JavaPlugin instance requesting validation
     * @param pluginId The unique identifier assigned to your plugin by MCLicense
     * @return true if the license is valid and active, false otherwise
     */
    public static boolean writeAndValidate(JavaPlugin plugin, String pluginId, String key) {
        try {
            // Check if license file exists or create it
            File licenseFile = new File(plugin.getDataFolder(), "mclicense.txt");
            if (!licenseFile.exists()) {
                plugin.getDataFolder().mkdirs();
                licenseFile.createNewFile();
            }

            // Write key
            Files.write(licenseFile.toPath(), key.getBytes(StandardCharsets.UTF_8));

            // Validate the license
            return validateLicenseWithServer(plugin, pluginId, key, licenseFile);
        } catch (Exception e) {
            Constants.LOGGER.info("License write and validation failed for " + plugin.getName() + " (System error)");
            e.printStackTrace();
            return false;
        }
    }
}