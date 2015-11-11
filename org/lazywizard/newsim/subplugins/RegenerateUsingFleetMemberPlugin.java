package org.lazywizard.newsim.subplugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;
import org.lazywizard.newsim.ReserveRegenerator;
import org.lazywizard.newsim.SimUtils;

// TODO: Ensure respawned allies have the same stats/skill effects as the original
public class RegenerateUsingFleetMemberPlugin extends BaseEveryFrameCombatPlugin
        implements ReserveRegenerator
{
    private static final Logger Log = Logger.getLogger(RegenerateUsingFleetMemberPlugin.class);
    private final FleetSide side;
    private final CombatFleetManagerAPI fm;
    private final Set<FleetMemberAPI> reserves;
    private boolean needsRecheck = false;

    public RegenerateUsingFleetMemberPlugin(FleetSide side,
            Collection<FleetMemberAPI> reserves)
    {
        this.side = side;
        fm = Global.getCombatEngine().getFleetManager(side);
        this.reserves = new HashSet<>(reserves);
    }

    @Override
    public void checkReserves()
    {
        // Check which fleet members are no longer in a side's reserves
        final Set<FleetMemberAPI> missing = new HashSet<>(reserves);
        missing.removeAll(fm.getReservesCopy());

        // Re-add any members who have been deployed
        if (!missing.isEmpty())
        {
            for (FleetMemberAPI oldShip : missing)
            {
                Log.debug("Adding " + oldShip.getSpecId() + " ("
                        + oldShip.getType().name() + ", "
                        + oldShip.getVariant().getSource()
                        + ") to sim reserves of side " + side.name());

                // Fighter wings are simple
                if (oldShip.isFighterWing())
                {
                    try
                    {
                        final FleetMemberAPI newShip = SimUtils.createFleetMember(
                                oldShip.getSpecId(), oldShip.getType(), side);
                        reserves.remove(oldShip);
                        reserves.add(newShip);
                        fm.addToReserves(newShip);
                    }
                    catch (Exception ex)
                    {
                        Log.error("Failed to recreate wing \"" + oldShip.getSpecId()
                                + "\", wing not found!");
                        reserves.remove(oldShip);
                    }
                }
                // Ships are more hacky, have to work around unsaved variants
                else
                {
                    try
                    {
                        final FleetMemberAPI newShip = SimUtils.createFleetMember(
                                oldShip.getHullId() + "_Hull", oldShip.getType(), side);
                        newShip.setCaptain(oldShip.getCaptain());
                        newShip.setVariant(oldShip.getVariant(), false, true);
                        newShip.setShipName(oldShip.getShipName());
                        newShip.setStatUpdateNeeded(false);
                        reserves.remove(oldShip);
                        reserves.add(newShip);
                        fm.addToReserves(newShip);
                    }
                    catch (Exception ex)
                    {
                        Log.error("Failed to recreate ship \"" + oldShip.getHullId()
                                + "_Hull\", variant not found!");
                        reserves.remove(oldShip);
                    }
                }
            }

            SimUtils.sortReserves(side);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // Only check for ship replacement after player uses picker dialog
        if (Global.getCombatEngine().isUIShowingDialog())
        {
            needsRecheck = true;
            return;
        }

        if (needsRecheck)
        {
            needsRecheck = false;
            if (fm.getReservesCopy().size() != reserves.size())
            {
                Log.debug("Regenerating side: " + side.name());
                checkReserves();
            }
        }
    }
}
