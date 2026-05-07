package com.romi.mogumogu.scripts;

/** Profile 相關共用判斷工具 */
public final class ScriptProfileUtil {

    private ScriptProfileUtil() {
    }

    /** 支援逗號分隔 profile（例如 dev,mysql） */
    public static boolean containsProfile(String activeProfiles, String expectedProfile) {
        for (String profile : activeProfiles.split(",")) {
            if (expectedProfile.equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }
}
