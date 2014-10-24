package org.lazywizard.asirb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Level;

public class ASIRBPlugin extends BaseEveryFrameCombatPlugin
{
    private static final float TIME_BETWEEN_CHECKS = 1f;
    private static final SortByHullSize sortByHullSize = new SortByHullSize();
    private float nextCheck = 0f;
    private int reservesSize = -1;

    private void checkAvailableShips()
    {
        Global.getLogger(ASIRBPlugin.class).log(Level.DEBUG, "Checking ships");

        List<FleetMemberAPI> newReserves = new ArrayList<>();
        FleetMemberAPI tmp;
        // Add all known ship variants to sim list
        for (String variantId : ASIRBMaster.getAllKnownShips())
        {
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, variantId);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBPlugin.class).log(Level.ERROR,
                        "Failed to create ship " + variantId, ex);
                ASIRBMaster.removeKnownShip(variantId);
                continue;
            }

            // Set as enemy with regular crew
            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(1);
            Global.getLogger(ASIRBPlugin.class).log(Level.DEBUG, "Added ship "
                    + tmp.getHullId() + " at CR " + tmp.getRepairTracker().getCR()
                    + ", crew " + tmp.getCrewFraction());
            newReserves.add(tmp);
        }
        // Add all known fighter wings to sim list
        for (String wingId : ASIRBMaster.getAllKnownWings())
        {
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, wingId);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBPlugin.class).log(Level.ERROR,
                        "Failed to create wing " + wingId, ex);
                ASIRBMaster.removeKnownWing(wingId);
                continue;
            }

            // Set as enemy with regular crew
            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(1);
            Global.getLogger(ASIRBPlugin.class).log(Level.DEBUG, "Added wing "
                    + tmp.getHullId() + " at CR " + tmp.getRepairTracker().getCR()
                    + ", crew " + tmp.getCrewFraction());
            newReserves.add(tmp);
        }

        CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY);
        Collections.sort(newReserves, sortByHullSize);

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
        // TODO: replace after next hotfix
        if (engine.isSimulation()) //InCampaignSim())
        {
            nextCheck -= amount;
            if (nextCheck <= 0f)
            {
                nextCheck = TIME_BETWEEN_CHECKS;

                if (engine.getFleetManager(FleetSide.ENEMY)
                        .getReservesCopy().size() != reservesSize)
                {
                    Global.getLogger(ASIRBPlugin.class).log(Level.DEBUG,
                            "Needs re-check!");
                    checkAvailableShips();
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        // TODO: Uncomment after next hotfix
        //if (engine.isInCampaignSim())
        {
            checkAvailableShips();
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
