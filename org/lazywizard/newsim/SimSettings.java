package org.lazywizard.newsim;

import java.io.IOException;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.ModUtils;
import org.lazywizard.newsim.subplugins.HealOnVictoryPlugin;
import org.lazywizard.newsim.subplugins.HulkCleanerPlugin;
import org.lazywizard.newsim.subplugins.InfiniteCRPlugin;

class SimSettings
{
    private static final String SETTINGS_FILE = "data/config/simulator/sim_settings.json";
    private static final boolean IS_SSP_ENABLED;
    static boolean REQUIRE_PLAYER_VICTORY_TO_UNLOCK, CLEAN_UP_HULKS,
            INFINITE_CR, HEAL_ON_VICTORY, WIPE_SIM_DATA_ON_PLAYER_DEATH,
            SHOW_UNLOCKED_OPPONENTS, INCLUDE_HULLS, USE_VANILLA_SIM_LIST;
    static float STARTING_CR;

    static
    {
        IS_SSP_ENABLED = ModUtils.isClassPresent("data.scripts.SSPModPlugin");
    }

    static void reload() throws IOException, JSONException
    {
        // Load settings from the json
        final JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);
        STARTING_CR = (float) settings.getDouble("startingCR");
        INFINITE_CR = settings.getBoolean("infiniteCR");
        HEAL_ON_VICTORY = settings.getBoolean("healOnVictory");
        CLEAN_UP_HULKS = settings.getBoolean("cleanUpHulks");
        REQUIRE_PLAYER_VICTORY_TO_UNLOCK = settings.getBoolean("requirePlayerVictoryToUnlock");
        WIPE_SIM_DATA_ON_PLAYER_DEATH = settings.getBoolean("wipeSimDataOnPlayerDeath");
        SHOW_UNLOCKED_OPPONENTS = settings.getBoolean("showUnlockedOpponents");
        INCLUDE_HULLS = settings.getBoolean("includeHulls");
        USE_VANILLA_SIM_LIST = IS_SSP_ENABLED && settings.getBoolean("useVanillaSimListWithStarsector+");

        // Set log level for all classes
        final Level logLevel = Level.toLevel(settings.getString("logLevel"), Level.WARN);
        Global.getLogger(SimModPlugin.class).setLevel(logLevel);
        Global.getLogger(SimSettings.class).setLevel(logLevel);
        Global.getLogger(SimMaster.class).setLevel(logLevel);
        Global.getLogger(SimCombatPlugin.class).setLevel(logLevel);
        Global.getLogger(HulkCleanerPlugin.class).setLevel(logLevel);
        Global.getLogger(InfiniteCRPlugin.class).setLevel(logLevel);
        Global.getLogger(HealOnVictoryPlugin.class).setLevel(logLevel);
        Global.getLogger(SimCampaignEventListener.class).setLevel(logLevel);
    }

    private SimSettings()
    {
    }
}
