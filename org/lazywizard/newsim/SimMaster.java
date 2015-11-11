package org.lazywizard.newsim;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class SimMaster
{
    private static final Logger Log = Global.getLogger(SimMaster.class);
    private static final String STARTING_OPPONENTS_CSV
            = "data/config/simulator/starting_sim_opponents.csv";
    private static final String KNOWN_SHIPS_PDATA_ID = "lw_simlist_opponents";
    private static Map<String, FleetMemberType> DEFAULT_OPPONENTS = null;

    public static Map<String, FleetMemberType> getAllKnownShips()
    {
        return Collections.unmodifiableMap(getAllKnownShipsActual());
    }

    public static void addKnownShip(String wingOrVariantId, FleetMemberType type)
    {
        Log.debug("Adding " + type + " " + wingOrVariantId + " to known ships");
        getAllKnownShipsActual().put(wingOrVariantId, type);
    }

    public static void removeKnownShip(String wingOrVariantId)
    {
        Log.debug("Removing ship " + wingOrVariantId + " from known ships");
        getAllKnownShipsActual().remove(wingOrVariantId);
    }

    static boolean checkAddOpponent(ShipAPI opponent)
    {
        // Only add stock variants, excluding empty hulls
        if (opponent == null || opponent.getVariant().isEmptyHullVariant()
                || opponent.getVariant().getSource() != VariantSource.STOCK)
        {
            return false;
        }

        // Add ship to persistent data, return true if it wasn't already known
        final String id = (opponent.isFighter() ? opponent.getWing().getWingId()
                : opponent.getVariant().getHullVariantId());
        final FleetMemberType type = (opponent.isFighter()
                ? FleetMemberType.FIGHTER_WING : FleetMemberType.SHIP);
        Log.debug("Attempting to add " + type + " " + id + " to known ships");
        return (getAllKnownShipsActual().put(id, type) == null);
    }

    public static Map<String, FleetMemberType> getDefaultOpponents()
    {
        if (DEFAULT_OPPONENTS == null)
        {
            DEFAULT_OPPONENTS = new LinkedHashMap<>();

            try
            {
                final JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                        "id", STARTING_OPPONENTS_CSV, "lw_asirb");
                for (int i = 0; i < csv.length(); i++)
                {
                    final JSONObject row = csv.getJSONObject(i);
                    DEFAULT_OPPONENTS.put(row.getString("id"), Enum.valueOf(
                            FleetMemberType.class, row.getString("type")));
                }
            }
            catch (Exception ex)
            {
                Log.error("Failed to read starting sim opponents, using hardcoded defaults!", ex);
                DEFAULT_OPPONENTS.clear();
                DEFAULT_OPPONENTS.put("hound_Standard", FleetMemberType.SHIP);
                DEFAULT_OPPONENTS.put("talon_wing", FleetMemberType.FIGHTER_WING);
            }
        }

        return Collections.unmodifiableMap(DEFAULT_OPPONENTS);
    }

    public static void resetDefaultSimList()
    {
        Log.debug("Creating default ship list");
        final Map<String, FleetMemberType> known = getAllKnownShipsActual();
        known.clear();
        known.putAll(getDefaultOpponents());
    }

    static Map<String, FleetMemberType> getAllKnownShipsActual()
    {
        // Sanity check, make sure we're actually in the campaign
        final SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return Collections.<String, FleetMemberType>emptyMap();
        }

        // This should never trip, but just in case
        final Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return Collections.<String, FleetMemberType>emptyMap();
        }

        // If there wasn't already simlist data, fill it with starting ships
        if (!persistentData.containsKey(KNOWN_SHIPS_PDATA_ID))
        {
            Log.debug("Creating default ship list");
            final Map<String, FleetMemberType> tmp = new LinkedHashMap<>();
            tmp.putAll(getDefaultOpponents());
            persistentData.put(KNOWN_SHIPS_PDATA_ID, tmp);
            return tmp;
        }

        return (Map<String, FleetMemberType>) persistentData.get(KNOWN_SHIPS_PDATA_ID);
    }

    private SimMaster()
    {
    }
}
