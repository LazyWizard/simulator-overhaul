package org.lazywizard.asirb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

public class ASIRBPlugin extends BaseEveryFrameCombatPlugin
{
    // TODO: Switch to 1f after next hotfix
    private static final float TIME_BETWEEN_CHECKS = 0f; // 1f;
    private static final SortByHullSize sortByHullSize = new SortByHullSize();
    private float nextCheck = TIME_BETWEEN_CHECKS;
    private int reservesSize = -1;

    private void checkAvailableShips()
    {
        //System.out.println("Checking ships");

        Set<String> toAdd = ASIRBMaster.getAllKnownShips();
        List<FleetMemberAPI> newReserves = new ArrayList<>();
        FleetMemberAPI tmp;
        for (String id : toAdd)
        {
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, id);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBPlugin.class).log(Level.ERROR,
                        "Failed to create ship " + id, ex);
                continue;
            }

            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(1);
            //System.out.println("Added " + tmp.getHullId() + " at CR "
            //        + tmp.getRepairTracker().getCR() + ", crew " + tmp.getCrewFraction());
            newReserves.add(tmp);
        }
        for (String id : ASIRBMaster.getAllKnownWings())
        {
            try
            {
                tmp = Global.getFactory().createFleetMember(
                        FleetMemberType.FIGHTER_WING, id);
            }
            catch (Exception ex)
            {
                Global.getLogger(ASIRBPlugin.class).log(Level.ERROR,
                        "Failed to create wing " + id, ex);
                continue;
            }

            tmp.getRepairTracker().setCR(.6f);
            tmp.getCrewComposition().addRegular(tmp.getNeededCrew());
            tmp.setOwner(1);
            //System.out.println("Added " + tmp.getHullId() + " at CR "
            //        + tmp.getRepairTracker().getCR() + ", crew " + tmp.getCrewFraction());
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
        if (engine.isInCampaignSim())
        {
            nextCheck -= amount;
            if (nextCheck <= 0f)
            {
                nextCheck = TIME_BETWEEN_CHECKS;

                if (engine.getFleetManager(FleetSide.ENEMY)
                        .getReservesCopy().size() != reservesSize)
                {
                    //System.out.println("Needs re-check!");
                    checkAvailableShips();
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        if (engine.isInCampaignSim())
        {
            checkAvailableShips();
        }
    }

    private static class SortByHullSize implements Comparator<FleetMemberAPI>
    {
        @Override
        public int compare(FleetMemberAPI o1, FleetMemberAPI o2)
        {
            if (o1.getHullSpec().getHullSize() == o2.getHullSpec().getHullSize())
            {
                return o1.getVariant().getFullDesignationWithHullName().compareTo(
                        o2.getVariant().getFullDesignationWithHullName());
            }

            return o2.getHullSpec().getHullSize().compareTo(
                    o1.getHullSpec().getHullSize());
        }
    }
}
