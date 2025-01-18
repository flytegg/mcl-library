package org.mclicense.library;

// Supported marketplaces: Polymart, Spigot
public class MarketplaceProvider {
    private static String pmPlaceholder = "%%__POLYMART__%%";
    private static String pmLicense = "%%__LICENSE__%%";

    private static String spUser = "%%__USER__%";
    private static String spResource = "%%__RESOURCE__%";

    protected static String getHardcodedLicense() {
        if (pmPlaceholder.equals("1") && !pmLicense.startsWith("%%__")) {
            return "pm_" + pmLicense;
        } else if (!spUser.startsWith("%%__") && !spResource.startsWith("%%__")) {
            return "sp_" + spResource + "_" + spUser;
        }
        return null;
    }
}