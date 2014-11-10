package org.lazywizard.newsim;

import java.io.IOException;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;

class SimSettings
{
    private static final String SETTINGS_FILE = "sim_settings.json";
    static float STARTING_CR;
    static boolean REQUIRE_PLAYER_VICTORY_TO_UNLOCK, WIPE_SIM_DATA_ON_PLAYER_DEATH,
            SHOW_UNLOCKED_OPPONENTS;

    static void reload() throws IOException, JSONException
    {
        // Load settings from the json
        final JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);
        STARTING_CR = (float) settings.optDouble("startingCR", 0.6);
        REQUIRE_PLAYER_VICTORY_TO_UNLOCK = settings.optBoolean(
                "requirePlayerVictoryToUnlock", true);
        WIPE_SIM_DATA_ON_PLAYER_DEATH = settings.optBoolean(
                "wipeSimDataOnPlayerDeath", false);
        SHOW_UNLOCKED_OPPONENTS = settings.optBoolean(
                "showUnlockedOpponents", true);

        // Set log level for all classes
        final Level logLevel = Level.toLevel(settings.optString("logLevel", "WARN"));
        Global.getLogger(SimModPlugin.class).setLevel(logLevel);
        Global.getLogger(SimSettings.class).setLevel(logLevel);
        Global.getLogger(SimMaster.class).setLevel(logLevel);
        Global.getLogger(SimCombatPlugin.class).setLevel(logLevel);
        Global.getLogger(SimCampaignEventListener.class).setLevel(logLevel);
    }

    private SimSettings()
    {
    }
}
