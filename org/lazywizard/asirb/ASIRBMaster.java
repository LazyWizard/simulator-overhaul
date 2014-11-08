package org.lazywizard.asirb;

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

class ASIRBMaster
{
    private static final String KNOWN_PDATA_ID = "lw_simlist_ships";
    private static final String LEGACY_SHIP_PDATA_ID = "lw_ASIRB_knownships";
    private static final String LEGACY_WING_PDATA_ID = "lw_ASIRB_knownwings";

    static FleetMemberType getType(ShipAPI ship)
    {
        return (ship.isFighter() ? FleetMemberType.FIGHTER_WING : FleetMemberType.SHIP);
    }

    static boolean checkAddOpponent(ShipAPI opponent)
    {
        if (opponent == null || opponent.getVariant().getSource() != VariantSource.STOCK)
        {
            return false;
        }

        Map<String, FleetMemberType> known = getAllKnownShips();
        String id = (opponent.isFighter() ? opponent.getWing().getWingId()
                : opponent.getVariant().getHullVariantId());
        Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                "Attempting to add " + id + " to known ships");
        return (known.put(id, getType(opponent)) != null);
    }

    public static void addKnownShip(String wingOrVariantId, FleetMemberType type)
    {
        getAllKnownShips().put(wingOrVariantId, type);
    }

    public static void removeKnownShip(String wingOrVariantId)
    {
        Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                "Removing ship " + wingOrVariantId + " from known ships");
        getAllKnownShips().remove(wingOrVariantId);
    }

    static void checkLegacy()
    {
        SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return;
        }

        Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return;
        }

        Map<String, FleetMemberType> newData = new LinkedHashMap<>();

        if (persistentData.containsKey(LEGACY_SHIP_PDATA_ID))
        {
            for (String id : (Set<String>) persistentData.get(LEGACY_SHIP_PDATA_ID))
            {
                newData.put(id, FleetMemberType.SHIP);
            }

            persistentData.remove(LEGACY_SHIP_PDATA_ID);
        }

        if (persistentData.containsKey(LEGACY_WING_PDATA_ID))
        {
            for (String id : (Set<String>) persistentData.get(LEGACY_WING_PDATA_ID))
            {
                newData.put(id, FleetMemberType.FIGHTER_WING);
            }

            persistentData.remove(LEGACY_WING_PDATA_ID);
        }

        for (Map.Entry<String, FleetMemberType> entry : newData.entrySet())
        {
            Global.getLogger(ASIRBMaster.class).log(Level.INFO,
                    "Moving legacy ship " + entry.getKey() + " to new system");
            addKnownShip(entry.getKey(), entry.getValue());
        }
    }

    public static Map<String, FleetMemberType> getAllKnownShips()
    {
        SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return Collections.<String, FleetMemberType>emptyMap();
        }

        Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return Collections.<String, FleetMemberType>emptyMap();
        }

        if (!persistentData.containsKey(KNOWN_PDATA_ID))
        {
            Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                    "Creating default ship list");
            Map<String, FleetMemberType> tmp = new LinkedHashMap<>();
            tmp.put("hound_Standard", FleetMemberType.SHIP);
            tmp.put("talon_wing", FleetMemberType.FIGHTER_WING);
            persistentData.put(KNOWN_PDATA_ID, tmp);
            return tmp;
        }

        return (Map<String, FleetMemberType>) persistentData.get(KNOWN_PDATA_ID);
    }

    private ASIRBMaster()
    {
    }
}
