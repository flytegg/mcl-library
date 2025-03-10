package org.mclicense.library;

// Supported marketplaces: Polymart, Spigot, BuiltByBit
public class MarketplaceProvider {
    private static String pmPlaceholder = "%%__POLYMART__%%";
    private static String pmLicense = "%%__LICENSE__%%";

    private static String spResource = "%%__RESOURCE__%%";
    private static String spUser = "%%__USER__%%";

    private static String bbbLicense = "%%__BBB_LICENSE__%%";

    protected static String getHardcodedLicense() {
        if (!bbbLicense.startsWith("%%__")) {
            return bbbLicense;
        } else if (pmPlaceholder.equals("1") && !pmLicense.startsWith("%%__")) {
            return "pm_" + pmLicense;
        } else if (!spResource.startsWith("%%__") && !spUser.startsWith("%%__")) {
            return "sptemp_" + spResource + "_" + spUser;
        }
        return null;
    }
}