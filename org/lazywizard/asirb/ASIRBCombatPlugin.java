package org.lazywizard.asirb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Level;

public class ASIRBCombatPlugin extends BaseEveryFrameCombatPlugin
{
    // TODO: Make which Comparator this uses to sort opponents into a config file option
    private static final Comparator<FleetMemberAPI> comparator = new SortByHullSize();
    private Set<String> playerShips, enemyShips, playerWings, enemyWings;
    private boolean needsRecheck;

    private void createShipList(FleetSide side, Set<String> shipIds, Set<String> wingIds)
    {
        Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                "Checking ships for side " + side.name());

        List<FleetMemberAPI> newReserves = new ArrayList<>();
        FleetMemberAPI tmp;
        for (Iterator<String> iter = shipIds.iterator(); iter.hasNext();)
        {
            String variantId = iter.next();
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, variantId);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBCombatPlugin.class).log(Level.ERROR,
                        "Failed to create ship " + variantId, ex);
                iter.remove();
                continue;
            }
            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(side.ordinal());
            Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                    "Added ship " + tmp.getHullId() + " to side " + side
                    + " at CR " + tmp.getRepairTracker().getCR());
            newReserves.add(tmp);
        }
        for (Iterator<String> iter = wingIds.iterator(); iter.hasNext();)
        {
            String wingId = iter.next();
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, wingId);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBCombatPlugin.class).log(Level.ERROR,
                        "Failed to create wing " + wingId, ex);
                iter.remove();
                continue;
            }
            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(side.ordinal());
            Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                    "Added wing " + tmp.getHullId() + " to side " + side
                    + " at CR " + tmp.getRepairTracker().getCR());
            newReserves.add(tmp);
        }

        CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(side);
        Collections.sort(newReserves, comparator);

        // Clear existing simulation list
        for (FleetMemberAPI member : fm.getReservesCopy())
        {
            fm.removeFromReserves(member);
        }

        // Add all variants to simulation list
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
                needsRecheck = false;

                // Regenerate missing enemy reserves
                if (engine.getFleetManager(FleetSide.ENEMY).getReservesCopy().size()
                        != (enemyShips.size() + enemyWings.size()))
                {
                    Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                            "Needs re-check!");
                    createShipList(FleetSide.ENEMY, enemyShips, enemyWings);
                }

                // Regenerate missing player reserves
                if (engine.isInCampaignSim()
                        && engine.getFleetManager(FleetSide.PLAYER).getReservesCopy().size()
                        != (playerShips.size() + playerWings.size()))
                {
                    createShipList(FleetSide.PLAYER, playerShips, playerWings);
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        if (engine.isSimulation())
        {
            // Remember previously fought opponents in campaign for auto-respawn
            if (engine.isInCampaignSim())
            {
                playerShips = new LinkedHashSet<>();
                playerWings = new LinkedHashSet<>();
                enemyShips = ASIRBMaster.getAllKnownShips();
                enemyWings = ASIRBMaster.getAllKnownWings();

                // Auto-respawn player ships in campaign
                for (FleetMemberAPI member : engine.getFleetManager(FleetSide.PLAYER).getReservesCopy())
                {
                    if (!member.isFighterWing())
                    {
                        playerShips.add(member.getSpecId());
                    }
                    else
                    {
                        playerWings.add(member.getSpecId());
                    }
                }
            }
            else
            {
                enemyShips = new LinkedHashSet<>();
                enemyWings = new LinkedHashSet<>();

                // Remember all sim_opponents.csv opponents in missions for auto-respawn
                for (FleetMemberAPI member : engine.getFleetManager(FleetSide.ENEMY).getReservesCopy())
                {
                    if (!member.isFighterWing())
                    {
                        enemyShips.add(member.getSpecId());
                    }
                    else
                    {
                        enemyWings.add(member.getSpecId());
                    }
                }
            }

            createShipList(FleetSide.ENEMY, enemyShips, enemyWings);
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
