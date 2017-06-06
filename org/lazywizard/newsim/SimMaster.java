package org.lazywizard.newsim;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONArray;
import org.json.JSONException;

public class SimMaster
{
    private static final String STARTING_OPPONENTS_CSV
            = "data/config/simulator/starting_sim_opponents.csv";
    private static final String KNOWN_SHIPS_PDATA_ID = "lw_simlist_opponents";
    private static Set<String> DEFAULT_OPPONENTS = null;

    public static Set<String> getAllKnownShips()
    {
        return Collections.unmodifiableSet(getAllKnownShipsActual());
    }

    public static void addKnownShip(String variantId)
    {
        Log.debug("Adding " + variantId + " to known ships");
        getAllKnownShipsActual().add(variantId);
    }

    public static void removeKnownShip(String variantId)
    {
        Log.debug("Removing ship " + variantId + " from known ships");
        getAllKnownShipsActual().remove(variantId);
    }

    static boolean checkAddOpponent(ShipAPI opponent)
    {
        if (opponent == null)
        {
            return false;
        }

        // Only add stock variants, excluding empty hulls
        final String id = opponent.getVariant().getHullVariantId();
        if (!SimUtils.isValidVariant(opponent.getVariant()))
        {
            Log.debug("Ignoring invalid ship " + id);
            return false;
        }

        // Add ship to persistent data, return true if it wasn't already known
        if (getAllKnownShipsActual().add(id))
        {
            Log.debug("Added " + id + " to known ships");
            return true;
        }

        Log.debug("Ignoring known ship " + id);
        return false;
    }

    public static Set<String> getDefaultOpponents()
    {
        if (DEFAULT_OPPONENTS == null)
        {
            DEFAULT_OPPONENTS = new LinkedHashSet<>();

            final JSONArray csv;
            try
            {
                csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                        "variant id", STARTING_OPPONENTS_CSV, "lw_asirb");
            }
            catch (IOException | JSONException ex)
            {
                Log.error("Failed to read starting sim opponents. Using hardcoded defaults instead!", ex);
                DEFAULT_OPPONENTS.clear();
                DEFAULT_OPPONENTS.add("kite_Raider");
                DEFAULT_OPPONENTS.add("hound_d_pirates_Standard");
                return Collections.unmodifiableSet(DEFAULT_OPPONENTS);
            }

            for (int i = 0; i < csv.length(); i++)
            {
                try
                {
                    final String id = csv.getJSONObject(i).getString("variant id");
                    if (id.toLowerCase().endsWith("_wing"))
                    {
                        Log.warn("Fighter wings are no longer accepted as sim opponents. Please remove '"
                                + id + "' from " + STARTING_OPPONENTS_CSV + ".");
                        continue;
                    }

                    DEFAULT_OPPONENTS.add(id);
                }
                catch (JSONException ex)
                {
                    Log.error("Failed to parse CSV row!", ex);
                }
            }
        }

        return Collections.unmodifiableSet(DEFAULT_OPPONENTS);
    }

    public static void resetDefaultSimList()
    {
        Log.debug("Creating default ship list");
        final Set<String> known = getAllKnownShipsActual();
        known.clear();
        known.addAll(getDefaultOpponents());
    }

    @SuppressWarnings("unchecked")
    static Set<String> getAllKnownShipsActual()
    {
        // Sanity check, make sure we're actually in the campaign
        final SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return Collections.<String>emptySet();
        }

        // This should never trip, but just in case
        final Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return Collections.<String>emptySet();
        }

        // If there wasn't already simlist data, fill it with starting ships
        if (!persistentData.containsKey(KNOWN_SHIPS_PDATA_ID))
        {
            Log.debug("Creating default ship list");
            final Set<String> tmp = new LinkedHashSet<>();
            tmp.addAll(getDefaultOpponents());
            persistentData.put(KNOWN_SHIPS_PDATA_ID, tmp);
            return tmp;
        }

        return (Set<String>) persistentData.get(KNOWN_SHIPS_PDATA_ID);
    }

    private SimMaster()
    {
    }
}
