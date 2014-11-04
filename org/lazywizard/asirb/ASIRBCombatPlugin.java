package org.lazywizard.asirb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

// TODO: respawn allied ships in campaign simulator list when killed
// TODO: provide infinite sim opponents in mission simulator
public class ASIRBCombatPlugin extends BaseEveryFrameCombatPlugin
{
    private static final float TIME_BETWEEN_CHECKS = .25f;
    // TODO: Make which Comparator this uses a config file option
    private static final Comparator<FleetMemberAPI> comparator = new SortByHullSize();
    private Set<String> playerShips, enemyShips, playerWings, enemyWings;
    private float nextCheck = 0f;
    private int reservesSize = -1;

    private void createShipList(FleetSide side,Set<String> shipIds, Set<String> wingIds)
    {
        Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                "Checking ships for side " + side.name());

        List<FleetMemberAPI> newReserves = new ArrayList<>();
        FleetMemberAPI tmp;
        // Add all known ship variants to sim list
        for (String variantId : shipIds)
        {
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, variantId);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBCombatPlugin.class).log(Level.ERROR,
                        "Failed to create ship " + variantId, ex);
                ASIRBMaster.removeKnownShip(variantId);
                continue;
            }

            // Set as enemy with regular crew
            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(side.ordinal());
            Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                    "Added ship " + tmp.getHullId() + " to side " + side
                    + " at CR " + tmp.getRepairTracker().getCR());
            newReserves.add(tmp);
        }
        // Add all known fighter wings to sim list
        for (String wingId : wingIds)
        {
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, wingId);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBCombatPlugin.class).log(Level.ERROR,
                        "Failed to create wing " + wingId, ex);
                ASIRBMaster.removeKnownWing(wingId);
                continue;
            }

            // Set as enemy with regular crew
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

        // Keep track of size of simulation list (know when to recalc)
        reservesSize = newReserves.size();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isSimulation())
        {
            nextCheck -= amount;
            if (nextCheck <= 0f)
            {
                nextCheck = TIME_BETWEEN_CHECKS;

                if (engine.getFleetManager(FleetSide.ENEMY)
                        .getReservesCopy().size() != reservesSize)
                {
                    Global.getLogger(ASIRBCombatPlugin.class).log(Level.DEBUG,
                            "Needs re-check!");
                    createShipList(FleetSide.ENEMY, enemyShips, enemyWings);
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
                enemyShips = ASIRBMaster.getAllKnownShips();
                enemyWings = ASIRBMaster.getAllKnownWings();
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
