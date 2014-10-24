package org.lazywizard.asirb;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.VariantSource;

public class ASIRBMaster
{
    private static final String KNOWN_SHIPS_PDATA_ID = "lw_ASIRB_knownships";
    private static final String KNOWN_WINGS_PDATA_ID = "lw_ASIRB_knownwings";

    public static boolean addOpponent(ShipAPI opponent)
    {
        if (opponent.getVariant().getSource() != VariantSource.STOCK)
        {
            return false;
        }

        Set<String> known = (opponent.isFighter() ? getAllKnownWings() : getAllKnownShips());
        String id = (opponent.isFighter() ? opponent.getWing().getWingId()
                : opponent.getVariant().getHullVariantId());
        //System.out.println(" ASIRB: adding " + id);
        return known.add(id);
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
            Set<String> tmp = new LinkedHashSet<>();
            tmp.add("talon_wing");
            persistentData.put(KNOWN_WINGS_PDATA_ID, tmp);
            return tmp;
        }

        return (Set<String>) persistentData.get(KNOWN_WINGS_PDATA_ID);
    }
}
