package org.lazywizard.newsim;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.newsim.comparators.SortByHullSize;

public class SimUtils
{
    // TODO: Change which Comparator is used to sort opponents into a config file option
    private static final Comparator<FleetMemberAPI> comparator = new SortByHullSize();

    public static void sortReserves(FleetSide side)
    {
        Log.debug("Sorting reserves of side " + side.name());
        final CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(side);
        final List<FleetMemberAPI> toSort = fm.getReservesCopy();
        Collections.sort(toSort, comparator);

        // Clear existing simulation reserves
        for (FleetMemberAPI member : fm.getReservesCopy())
        {
            fm.removeFromReserves(member);
        }

        // Add all variants to simulation reserves
        for (FleetMemberAPI member : toSort)
        {
            fm.addToReserves(member);
        }
    }

    public static void clearReserves(FleetSide side)
    {
        Log.debug("Clearing reserves of side " + side.name());
        final CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(side);
        for (FleetMemberAPI member : fm.getReservesCopy())
        {
            fm.removeFromReserves(member);
        }
    }

    public static FleetMemberAPI createFleetMember(String id, FleetMemberType type, FleetSide side)
    {
        // Set the created ship's combat readiness and which side it fights for
        final FleetMemberAPI toAdd = Global.getFactory().createFleetMember(type, id);
        toAdd.setShipName("SIM " + toAdd.getVariant().getDesignation());
        toAdd.getCrewComposition().addCrew(toAdd.getNeededCrew());
        toAdd.getRepairTracker().setCR(SimSettings.STARTING_CR);
        toAdd.getStats().getMaxCombatReadiness().modifyFlat("sim_startingcr",
                (SimSettings.STARTING_CR - toAdd.getRepairTracker().getMaxCR()));
        toAdd.setOwner(side.ordinal());
        return toAdd;
    }

    public static boolean isValidVariant(ShipVariantAPI variant)
    {
        final Set<ShipTypeHints> hints = variant.getHullSpec().getHints();
        return !(variant.isEmptyHullVariant() || !variant.isStockVariant()
                || variant.getHullSize() == HullSize.FIGHTER || variant.isFighter()
                || (!SimSettings.INCLUDE_HIDDEN && hints.contains(ShipTypeHints.HIDE_IN_CODEX))
                || (!SimSettings.INCLUDE_STATIONS && hints.contains(ShipTypeHints.STATION)));
    }

    private SimUtils()
    {
    }
}
