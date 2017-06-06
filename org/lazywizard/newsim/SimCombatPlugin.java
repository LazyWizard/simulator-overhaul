package org.lazywizard.newsim;

import java.util.HashSet;
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
import org.lazywizard.newsim.subplugins.HealOnVictoryPlugin;
import org.lazywizard.newsim.subplugins.HulkCleanerPlugin;
import org.lazywizard.newsim.subplugins.InfiniteCRPlugin;

public class SimCombatPlugin extends BaseEveryFrameCombatPlugin
{
    private final Set<FleetMemberAPI> playerShips = new HashSet<>(), enemyShips = new HashSet<>();
    private boolean needsRecheck = false;

    private void generateReserveLists(boolean useUnlockedOpponentList)
    {
        playerShips.clear();
        enemyShips.clear();
        final CombatEngineAPI engine = Global.getCombatEngine();
        final CombatFleetManagerAPI fmPlayer = engine.getFleetManager(FleetSide.PLAYER);
        final CombatFleetManagerAPI fmEnemy = engine.getFleetManager(FleetSide.ENEMY);

        // Register player ships for respawn
        for (FleetMemberAPI member : fmPlayer.getReservesCopy())
        {
            playerShips.add(member);
        }

        // Register enemy known ship list and repopulate sim opponents with said list
        if (useUnlockedOpponentList)
        {
            for (Iterator<String> iter = SimMaster.getAllKnownShipsActual().iterator(); iter.hasNext();)
            {
                final String id = iter.next();
                try
                {
                    final FleetMemberAPI toAdd = SimUtils.createFleetMember(
                            id, FleetMemberType.SHIP, FleetSide.ENEMY);
                    enemyShips.add(toAdd);
                }
                catch (Exception ex)
                {
                    Log.error("Failed to instantiate ship: " + id, ex);
                    iter.remove();
                    Log.info("Removed " + id + " from known opponents list");
                }
            }
        }
        // Fallback: using sim_opponents.csv instead (missions/Starsector+)
        else
        {
            for (FleetMemberAPI member : fmEnemy.getReservesCopy())
            {
                enemyShips.add(member);
            }
        }

        // Auto-add hulls for known variants if relevant setting is enabled
        if (SimSettings.INCLUDE_HULLS)
        {
            final Set<String> hulls = new LinkedHashSet<>();
            for (FleetMemberAPI member : enemyShips)
            {
                if (!member.isFighterWing() && !member.getVariant().isEmptyHullVariant())
                {
                    hulls.add(member.getHullSpec().getHullId() + "_Hull");
                }
            }

            for (String hull : hulls)
            {
                try
                {
                    final FleetMemberAPI toAdd = SimUtils.createFleetMember(hull,
                            FleetMemberType.SHIP, FleetSide.ENEMY);
                    toAdd.setShipName("SIM Hull");
                    enemyShips.add(toAdd);
                }
                catch (Exception ex)
                {
                    Log.error("No hull variant found for \"" + hull + "\"!");
                }
            }
        }

        // Populate enemy reserves with replacement list
        SimUtils.clearReserves(FleetSide.ENEMY);
        checkReserves(FleetSide.ENEMY, enemyShips);
    }

    private static void checkReserves(FleetSide side, Set<FleetMemberAPI> reserves)
    {
        // Check which fleet members are no longer in a side's reserves
        final CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(side);
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

                // Copy the ship, preserving custom variants
                try
                {
                    final FleetMemberAPI newShip = SimUtils.createFleetMember(
                            oldShip.getSpecId(), oldShip.getType(), side);
                    if (!newShip.isFighterWing())
                    {
                        newShip.setVariant(oldShip.getVariant(), false, true);
                        newShip.setCaptain(oldShip.getCaptain());
                        newShip.setShipName(oldShip.getShipName());
                        newShip.setStatUpdateNeeded(false);
                    }

                    reserves.remove(oldShip);
                    reserves.add(newShip);
                    fm.addToReserves(newShip);
                }
                catch (Exception ex)
                {
                    if (oldShip.isFighterWing())
                    {
                        Log.error("Failed to recreate wing \"" + oldShip.getSpecId()
                                + "\", wing not found!");
                    }
                    else
                    {
                        Log.error("Failed to recreate ship \"" + oldShip.getHullId()
                                + "\", variant not found!");
                    }

                    reserves.remove(oldShip);
                }
            }

            SimUtils.sortReserves(side);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isSimulation())
        {
            // Only check for ship replacement after player uses picker dialog
            if (engine.isUIShowingDialog())
            {
                needsRecheck = true;
                return;
            }

            // Check if either fleet needs reserves respawned
            if (needsRecheck)
            {
                Log.debug("Rechecking sim reserves");
                needsRecheck = false;

                // Regenerate missing enemy reserves
                if (engine.getFleetManager(FleetSide.ENEMY)
                        .getReservesCopy().size() != enemyShips.size())
                {
                    Log.debug("Regenerating side: " + FleetSide.ENEMY);
                    checkReserves(FleetSide.ENEMY, enemyShips);
                }

                // Regenerate missing player reserves
                if (engine.isInCampaignSim() && engine.getFleetManager(FleetSide.PLAYER)
                        .getReservesCopy().size() != playerShips.size())
                {
                    Log.debug("Regenerating side: " + FleetSide.PLAYER);
                    checkReserves(FleetSide.PLAYER, playerShips);
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        if (engine.isSimulation())
        {
            // Determine whether to use the custom simlist or vanilla behavior
            final boolean useUnlockedList = (engine.isInCampaignSim()
                    && !SimSettings.USE_VANILLA_SIM_LIST);
            Log.info("Using sim_opponents.csv: " + !useUnlockedList);

            // Add sub-plugins if their options are enabled
            if (SimSettings.CLEAN_UP_HULKS)
            {
                engine.addPlugin(new HulkCleanerPlugin());
            }
            if (SimSettings.INFINITE_CR)
            {
                engine.addPlugin(new InfiniteCRPlugin());
            }
            if (SimSettings.HEAL_ON_VICTORY)
            {
                engine.addPlugin(new HealOnVictoryPlugin());
            }

            // Determine what ships will be replenished for each side
            generateReserveLists(useUnlockedList);
            needsRecheck = false;
        }
    }
}
