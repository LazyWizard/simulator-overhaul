package org.lazywizard.newsim;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;

// TODO: Ensure respawned allies have the same stats/skill effects as the original
public class SimCombatPlugin extends BaseEveryFrameCombatPlugin
{
    private static final Logger Log = Global.getLogger(SimCombatPlugin.class);
    // TODO: Change which Comparator is used to sort opponents into a config file option
    private static final Comparator<FleetMemberAPI> comparator = new SortByHullSize();
    private final Set<FleetMemberAPI> playerShips = new HashSet<>(), enemyShips = new HashSet<>();
    private boolean needsRecheck = false, addHulkCleaner = false,
            addHealPlugin = false, addInfiniteCR = false;

    // Awkward utility method since we don't have direct access to reserves list
    private static void sortReserves(FleetSide side)
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

    private static void clearReserves(FleetSide side)
    {
        Log.debug("Clearing reserves of side " + side.name());
        final CombatFleetManagerAPI fm = Global.getCombatEngine().getFleetManager(side);
        for (FleetMemberAPI member : fm.getReservesCopy())
        {
            fm.removeFromReserves(member);
        }
    }

    private static FleetMemberAPI createFleetMember(String id, FleetMemberType type, FleetSide side)
    {
        // Set the created ship's combat readiness and which side it fights for
        final FleetMemberAPI toAdd = Global.getFactory().createFleetMember(type, id);
        toAdd.setShipName("SIM " + toAdd.getVariant().getDesignation());
        toAdd.getCrewComposition().addRegular(toAdd.getNeededCrew());
        toAdd.getRepairTracker().setCR(SimSettings.STARTING_CR);
        toAdd.getStats().getMaxCombatReadiness().modifyFlat("sim_startingcr",
                (SimSettings.STARTING_CR - .6f));
        toAdd.setOwner(side.ordinal());
        return toAdd;
    }

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
            for (Iterator<Map.Entry<String, FleetMemberType>> iter
                    = SimMaster.getAllKnownShipsActual().entrySet().iterator(); iter.hasNext();)
            {
                final Map.Entry<String, FleetMemberType> entry = iter.next();
                try
                {
                    final FleetMemberAPI toAdd = createFleetMember(entry.getKey(),
                            entry.getValue(), FleetSide.ENEMY);
                    enemyShips.add(toAdd);
                }
                catch (Exception ex)
                {
                    Log.error("Failed to instantiate ship: " + entry.getKey()
                            + " (" + entry.getValue().name() + ")", ex);
                    iter.remove();
                    Log.info("Removed " + entry.getKey() + " from known opponents list");
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
            for (FleetMemberAPI member : fmEnemy.getReservesCopy())
            {
                if (!member.isFighterWing() && !member.getVariant().isEmptyHullVariant())
                {
                    hulls.add(member.getHullSpec().getHullId() + "_Hull");
                }
            }

            for (String hull : hulls)
            {
                final FleetMemberAPI toAdd = createFleetMember(hull,
                        FleetMemberType.SHIP, FleetSide.ENEMY);
                toAdd.setShipName("SIM Hull");
                enemyShips.add(toAdd);
            }
        }

        // Populate enemy reserves with replacement list
        clearReserves(FleetSide.ENEMY);
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

                // Fighter wings are simple
                if (oldShip.isFighterWing())
                {
                    final FleetMemberAPI newShip = createFleetMember(
                            oldShip.getSpecId(), oldShip.getType(), side);
                    reserves.remove(oldShip);
                    reserves.add(newShip);
                    fm.addToReserves(newShip);
                }
                // Ships are more hacky, have to work around unsaved variants
                else
                {
                    final FleetMemberAPI newShip = createFleetMember(
                            oldShip.getHullId() + "_Hull", oldShip.getType(), side);
                    newShip.setCaptain(oldShip.getCaptain());
                    newShip.setVariant(oldShip.getVariant(), false, true);
                    newShip.setShipName(oldShip.getShipName());
                    newShip.setStatUpdateNeeded(false);
                    reserves.remove(oldShip);
                    reserves.add(newShip);
                    fm.addToReserves(newShip);
                }
            }

            sortReserves(side);
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

            // TODO: TEMPORARY UNTIL 0.7a
            if (addHulkCleaner)
            {
                addHulkCleaner = false;
                engine.addPlugin(new HulkCleanerPlugin());
            }

            // TODO: TEMPORARY UNTIL 0.7a
            if (addInfiniteCR)
            {
                addInfiniteCR = false;
                engine.addPlugin(new InfiniteCRPlugin());
            }

            // TODO: TEMPORARY UNTIL 0.7a
            if (addHealPlugin)
            {
                addHealPlugin = false;
                engine.addPlugin(new HealOnVictoryPlugin());
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

            // TODO: Can move addPlugin() calls here after 0.7a is released
            if (SimSettings.CLEAN_UP_HULKS)
            {
                addHulkCleaner = true;
            }
            if (SimSettings.INFINITE_CR)
            {
                addInfiniteCR = true;
            }
            if (SimSettings.HEAL_ON_VICTORY)
            {
                addHealPlugin = true;
            }

            // Determine what ships will be replenished for each side
            generateReserveLists(useUnlockedList);
            needsRecheck = false;
        }
    }

    private static class SortByHullSize implements Comparator<FleetMemberAPI>
    {
        @Override
        public int compare(FleetMemberAPI o1, FleetMemberAPI o2)
        {
            // Hulls go at the end of the list
            final boolean isO1Hull = o1.getVariant().isEmptyHullVariant(),
                    isO2Hull = o2.getVariant().isEmptyHullVariant();
            if (isO1Hull && !isO2Hull)
            {
                return 1;
            }
            else if (isO2Hull && !isO1Hull)
            {
                return -1;
            }

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
