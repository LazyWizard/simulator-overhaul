package org.lazywizard.newsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Level;

public class SimCombatPlugin extends BaseEveryFrameCombatPlugin
{
    // TODO: Make which Comparator this uses to sort opponents into a config file option
    private static final Comparator<FleetMemberAPI> comparator = new SortByHullSize();
    private Map<String, FleetMemberType> playerShips, enemyShips;
    private boolean needsRecheck, useSimList;

    // TODO: Only create ships that are missing from reserve list
    private static void createShipList(final FleetSide side,
            final Map<String, FleetMemberType> shipIds)
    {
        final List<FleetMemberAPI> newReserves = new ArrayList<>();
        Global.getLogger(SimCombatPlugin.class).log(Level.DEBUG,
                "Checking ships for side " + side.name());

        // Create replacement ship reserves
        FleetMemberAPI tmp;
        for (Iterator<Map.Entry<String, FleetMemberType>> iter
                = shipIds.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<String, FleetMemberType> entry = iter.next();
            String shipId = entry.getKey();
            FleetMemberType type = entry.getValue();
            try
            {
                tmp = Global.getFactory().createFleetMember(type, shipId);
            }
            catch (Exception ex)
            {
                Global.getLogger(SimCombatPlugin.class).log(Level.ERROR,
                        "Failed to create " + type + " " + shipId, ex);
                iter.remove();
                continue;
            }

            // Set the created ship's combat readiness and which side it fights for
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.getRepairTracker().setCR(SimSettings.STARTING_CR);
            tmp.getStats().getMaxCombatReadiness().modifyFlat("sim_startingcr",
                    (SimSettings.STARTING_CR - .6f));
            tmp.setOwner(side.ordinal());
            Global.getLogger(SimCombatPlugin.class).log(Level.DEBUG,
                    "Added " + type + " " + shipId + " to side " + side
                    + " at CR " + tmp.getRepairTracker().getCR());
            newReserves.add(tmp);
        }

        CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(side);
        Collections.sort(newReserves, comparator);

        // Clear existing simulation reserves
        for (FleetMemberAPI member : fm.getReservesCopy())
        {
            fm.removeFromReserves(member);
        }

        // Add all variants to simulation reserves
        for (FleetMemberAPI member : newReserves)
        {
            fm.addToReserves(member);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isSimulation())
        {
            // Only check for ship replacement after player uses picker dialog
            if (engine.isUIShowingDialog())
            {
                needsRecheck = true;
                return;
            }

            // check if either fleet needs reserves respawned
            if (needsRecheck)
            {
                Global.getLogger(SimCombatPlugin.class).log(Level.DEBUG,
                        "Rechecking sim reserves");
                needsRecheck = false;

                // Regenerate missing enemy reserves
                if (engine.getFleetManager(FleetSide.ENEMY)
                        .getReservesCopy().size() != enemyShips.size())
                {
                    Global.getLogger(SimCombatPlugin.class).log(Level.DEBUG,
                            "Regenerating side: " + FleetSide.ENEMY);
                    createShipList(FleetSide.ENEMY, enemyShips);
                }

                // Regenerate missing player reserves
                if (engine.isInCampaignSim() && engine.getFleetManager(FleetSide.PLAYER)
                        .getReservesCopy().size() != playerShips.size())
                {
                    Global.getLogger(SimCombatPlugin.class).log(Level.DEBUG,
                            "Regenerating side: " + FleetSide.PLAYER);
                    createShipList(FleetSide.PLAYER, playerShips);
                }
            }
        }
    }

    private static Map<String, FleetMemberType> getReservesMap(FleetSide side)
    {
        Map<String, FleetMemberType> tmp = new LinkedHashMap<>();

        // Remember all sim_opponents.csv opponents in missions
        for (FleetMemberAPI member : Global.getCombatEngine().getFleetManager(side).getReservesCopy())
        {
            tmp.put(member.getSpecId(), member.getType());
        }

        return tmp;
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        if (engine.isSimulation())
        {
            useSimList = (engine.isInCampaignSim() && !SimSettings.IS_SSP_ENABLED);
            Global.getLogger(SimCombatPlugin.class).log(Level.INFO,
                    "Using sim_opponents.csv: " + !useSimList);

            playerShips = getReservesMap(FleetSide.PLAYER);
            enemyShips = (useSimList ? SimMaster.getAllKnownShipsActual()
                    : getReservesMap(FleetSide.ENEMY));
            createShipList(FleetSide.ENEMY, enemyShips);
            needsRecheck = false;
        }
    }

    private static class SortByHullSize implements Comparator<FleetMemberAPI>
    {
        @Override
        public int compare(FleetMemberAPI o1, FleetMemberAPI o2)
        {
            // Sort ships of same hull size by their name
            if (o1.getHullSpec().getHullSize() == o2.getHullSpec().getHullSize())
            {
                return o1.getVariant().getFullDesignationWithHullName().compareTo(
                        o2.getVariant().getFullDesignationWithHullName());
            }

            // Sort ships by hull size
            return o2.getHullSpec().getHullSize().compareTo(
                    o1.getHullSpec().getHullSize());
        }
    }
}
