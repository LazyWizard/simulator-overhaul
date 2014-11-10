package org.lazywizard.newsim;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import org.apache.log4j.Level;

public class SimMaster
{
    private static final String KNOWN_SHIPS_PDATA_ID = "lw_simlist_opponents";
    private static final String LEGACY_SHIP_PDATA_ID = "lw_ASIRB_knownships";
    private static final String LEGACY_WING_PDATA_ID = "lw_ASIRB_knownwings";

    public static Map<String, FleetMemberType> getAllKnownShips()
    {
        return new LinkedHashMap<>(getAllKnownShipsActual());
    }

    public static void addKnownShip(String wingOrVariantId, FleetMemberType type)
    {
        Global.getLogger(SimMaster.class).log(Level.DEBUG,
                "Adding " + type + " " + wingOrVariantId + " to known ships");
        getAllKnownShipsActual().put(wingOrVariantId, type);
    }

    public static void removeKnownShip(String wingOrVariantId)
    {
        Global.getLogger(SimMaster.class).log(Level.DEBUG,
                "Removing ship " + wingOrVariantId + " from known ships");
        getAllKnownShipsActual().remove(wingOrVariantId);
    }

    static boolean checkAddOpponent(ShipAPI opponent)
    {
        if (opponent == null || opponent.getVariant().getSource() != VariantSource.STOCK)
        {
            return false;
        }

        // Add ship to persistent data, return true if it wasn't already known
        String id = (opponent.isFighter() ? opponent.getWing().getWingId()
                : opponent.getVariant().getHullVariantId());
        FleetMemberType type = (opponent.isFighter() ? FleetMemberType.FIGHTER_WING
                : FleetMemberType.SHIP);
        Global.getLogger(SimMaster.class).log(Level.DEBUG,
                "Attempting to add " + type + " " + id + " to known ships");
        return (getAllKnownShipsActual().put(id, type) == null);
    }

    // TODO: Remove this after the next compatibility-breaking SS update
    static void checkLegacy()
    {
        // Sanity check, make sure we're actually in the campaign
        SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return;
        }

        // This should never trip, but just in case
        Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return;
        }

        Map<String, FleetMemberType> toTransfer = new LinkedHashMap<>();

        // Add old ships to simulator data
        if (persistentData.containsKey(LEGACY_SHIP_PDATA_ID))
        {
            for (String id : (Set<String>) persistentData.get(LEGACY_SHIP_PDATA_ID))
            {
                toTransfer.put(id, FleetMemberType.SHIP);
            }

            persistentData.remove(LEGACY_SHIP_PDATA_ID);
        }

        // Add old wings to simulator data
        if (persistentData.containsKey(LEGACY_WING_PDATA_ID))
        {
            for (String id : (Set<String>) persistentData.get(LEGACY_WING_PDATA_ID))
            {
                toTransfer.put(id, FleetMemberType.FIGHTER_WING);
            }

            persistentData.remove(LEGACY_WING_PDATA_ID);
        }

        // Register legacy simulator data with the new system
        if (!toTransfer.isEmpty())
        {
            Map<String, FleetMemberType> known = getAllKnownShipsActual();
            for (Map.Entry<String, FleetMemberType> entry : toTransfer.entrySet())
            {
                Global.getLogger(SimMaster.class).log(Level.INFO,
                        "Moving legacy ship " + entry.getKey() + " to new system");
                known.put(entry.getKey(), entry.getValue());
            }

            // Notify player of transferred data
            if (SimSettings.SHOW_UNLOCKED_OPPONENTS)
            {
                Global.getSector().getCampaignUI().addMessage(
                        "Combat simulator hardware upgrade complete");
            }
        }
    }

    static Map<String, FleetMemberType> getAllKnownShipsActual()
    {
        // Sanity check, make sure we're actually in the campaign
        SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return Collections.<String, FleetMemberType>emptyMap();
        }

        // This should never trip, but just in case
        Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return Collections.<String, FleetMemberType>emptyMap();
        }

        // If there wasn't already simlist data, fill it with starting ships
        // TODO: Make starting simulator opponents a config file or CSV
        if (!persistentData.containsKey(KNOWN_SHIPS_PDATA_ID))
        {
            Global.getLogger(SimMaster.class).log(Level.DEBUG,
                    "Creating default ship list");
            Map<String, FleetMemberType> tmp = new LinkedHashMap<>();
            tmp.put("hound_Standard", FleetMemberType.SHIP);
            tmp.put("talon_wing", FleetMemberType.FIGHTER_WING);
            persistentData.put(KNOWN_SHIPS_PDATA_ID, tmp);
            return tmp;
        }

        return (Map<String, FleetMemberType>) persistentData.get(KNOWN_SHIPS_PDATA_ID);
    }

    private SimMaster()
    {
    }
}
