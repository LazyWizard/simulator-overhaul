package org.lazywizard.asirb;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.VariantSource;
import org.apache.log4j.Level;

public class ASIRBMaster
{
    private static final String KNOWN_SHIPS_PDATA_ID = "lw_ASIRB_knownships";
    private static final String KNOWN_WINGS_PDATA_ID = "lw_ASIRB_knownwings";

    static boolean checkAddOpponent(ShipAPI opponent)
    {
        if (opponent == null || opponent.getVariant().getSource() != VariantSource.STOCK)
        {
            return false;
        }

        Set<String> known = (opponent.isFighter() ? getAllKnownWings() : getAllKnownShips());
        String id = (opponent.isFighter() ? opponent.getWing().getWingId()
                : opponent.getVariant().getHullVariantId());
        Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                "Attempting to add " + id + " to known ships");
        return known.add(id);
    }

    public static void removeKnownShip(String variantId)
    {
        Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                "Removing ship " + variantId + " from known ships");
        getAllKnownShips().remove(variantId);
    }

    public static void removeKnownWing(String wingId)
    {
        Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                "Removing wing " + wingId + " from known wings");
        getAllKnownWings().remove(wingId);
    }

    public static Set<String> getAllKnownShips()
    {
        SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return Collections.<String>emptySet();
        }

        Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return Collections.<String>emptySet();
        }

        if (!persistentData.containsKey(KNOWN_SHIPS_PDATA_ID))
        {
            Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                    "Creating default ship list");
            Set<String> tmp = new LinkedHashSet<>();
            tmp.add("hound_Standard");
            persistentData.put(KNOWN_SHIPS_PDATA_ID, tmp);
            return tmp;
        }

        return (Set<String>) persistentData.get(KNOWN_SHIPS_PDATA_ID);
    }

    public static Set<String> getAllKnownWings()
    {
        SectorAPI sector = Global.getSector();
        if (sector == null)
        {
            return Collections.<String>emptySet();
        }

        Map<String, Object> persistentData = sector.getPersistentData();
        if (persistentData == null)
        {
            return Collections.<String>emptySet();
        }

        if (!persistentData.containsKey(KNOWN_WINGS_PDATA_ID))
        {
            Global.getLogger(ASIRBMaster.class).log(Level.DEBUG,
                    "Creating default wing list");
            Set<String> tmp = new LinkedHashSet<>();
            tmp.add("talon_wing");
            persistentData.put(KNOWN_WINGS_PDATA_ID, tmp);
            return tmp;
        }

        return (Set<String>) persistentData.get(KNOWN_WINGS_PDATA_ID);
    }

    private ASIRBMaster()
    {
    }
}
