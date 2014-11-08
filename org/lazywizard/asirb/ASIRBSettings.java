package org.lazywizard.asirb;

import java.io.IOException;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;

class ASIRBSettings
{
    private static final String SETTINGS_FILE = "asirb_settings.json";
    static boolean REQUIRE_PLAYER_VICTORY_TO_UNLOCK;
    static boolean WIPE_SIM_DATA_ON_PLAYER_DEATH;
    static boolean SHOW_UNLOCKED_OPPONENTS;

    static void reload() throws IOException, JSONException
    {
        // Load ASIRB settings from the json
        final JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);
        REQUIRE_PLAYER_VICTORY_TO_UNLOCK = settings.optBoolean(
                "requirePlayerVictoryToUnlock", true);
        WIPE_SIM_DATA_ON_PLAYER_DEATH = settings.optBoolean(
                "wipeSimDataOnPlayerDeath", false);
        SHOW_UNLOCKED_OPPONENTS = settings.optBoolean(
                "showUnlockedOpponents", true);

        // Set log level for all ASIRB classes
        final Level logLevel = Level.toLevel(settings.optString("logLevel", "WARN"));
        Global.getLogger(ASIRBModPlugin.class).setLevel(logLevel);
        Global.getLogger(ASIRBSettings.class).setLevel(logLevel);
        Global.getLogger(ASIRBMaster.class).setLevel(logLevel);
        Global.getLogger(ASIRBCombatPlugin.class).setLevel(logLevel);
        Global.getLogger(ASIRBCampaignEventListener.class).setLevel(logLevel);
    }

    private ASIRBSettings()
    {
    }
}
