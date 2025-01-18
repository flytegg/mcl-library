package org.mclicense.library;

// Supported marketplaces: Polymart, Spigot
public class MarketplaceProvider {
    private static String pmPlaceholder = "%%__POLYMART__%%";
    private static String pmLicense = "%%__LICENSE__%%";

    private static String spResource = "%%__RESOURCE__%";
    private static String spUser = "%%__USER__%";

    protected static String getHardcodedLicense() {
        if (pmPlaceholder.equals("1") && !pmLicense.startsWith("%%__")) {
            return "pm_" + pmLicense;
        } else if (!spResource.startsWith("%%__") && !spUser.startsWith("%%__")) {
            return "spnew_" + spResource + "_" + spUser;
        }
        return null;
    }
}