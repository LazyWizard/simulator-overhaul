package org.lazywizard.newsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Level;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

class SimCampaignEventListener extends BaseCampaignEventListener implements EveryFrameScript
{
    private static final float TIME_BETWEEN_REPORT_CHECKS = .25f;
    private final List<String> newShips, newWings;
    private float timeUntilReportCheck = TIME_BETWEEN_REPORT_CHECKS;

    SimCampaignEventListener()
    {
        super(false);
        newShips = new ArrayList<>();
        newWings = new ArrayList<>();
    }

    private static boolean wasFullyDestroyed(EngagementResultForFleetAPI fleet)
    {
        Global.getLogger(SimCampaignEventListener.class).log(Level.DEBUG,
                "Reserves size: " + fleet.getReserves().size()
                + "\nDeployed size: " + fleet.getDeployed().size()
                + "\nRetreated size: " + fleet.getRetreated().size()
                + "\nIs valid: " + fleet.getFleet().isValidPlayerFleet());
        return (fleet.getReserves().isEmpty() && fleet.getDeployed().isEmpty()
                && fleet.getRetreated().isEmpty());
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result)
    {
        if (!result.didPlayerWin())
        {
            // Wipe sim list on fleet death if that option is enabled
            if (SimSettings.WIPE_SIM_DATA_ON_PLAYER_DEATH
                    && wasFullyDestroyed(result.getLoserResult()))
            {
                SimMaster.getAllKnownShipsActual().clear();
            }

            // Don't remember opponents on a loss if that option is enabled
            if (SimSettings.REQUIRE_PLAYER_VICTORY_TO_UNLOCK)
            {
                return;
            }
        }
        // Don't include battles fough by second in command
        if (result.getWinnerResult().getAllEverDeployedCopy() == null
                || result.getLoserResult().getAllEverDeployedCopy() == null)
        {
            return;
        }

        // Check the deployed ships for opponents we've never fought before
        final List<DeployedFleetMemberAPI> seenShips
                = result.getLoserResult().getAllEverDeployedCopy();
        seenShips.addAll(result.getWinnerResult().getAllEverDeployedCopy());
        for (DeployedFleetMemberAPI dfm : seenShips)
        {
            ShipAPI ship = dfm.getShip();
            if (SimMaster.checkAddOpponent(ship))
            {
                // If we aren't announcing new unlocks don't populate the announcement list
                if (!SimSettings.SHOW_UNLOCKED_OPPONENTS)
                {
                    continue;
                }

                // Register the newly unlocked variant to be announced on the campaign map
                if (ship.isFighter())
                {
                    newWings.add(ship.getVariant().getFullDesignationWithHullName());
                }
                else
                {
                    newShips.add(ship.getVariant().getFullDesignationWithHullName());
                }
            }
        }
    }

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return true;
    }

    @Override
    public void advance(float amount)
    {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!SimSettings.SHOW_UNLOCKED_OPPONENTS || ui == null || ui.isShowingDialog())
        {
            return;
        }

        // Minor optimization: only check for new ships a few times per second
        timeUntilReportCheck -= amount;
        if (timeUntilReportCheck <= 0f)
        {
            timeUntilReportCheck = TIME_BETWEEN_REPORT_CHECKS;

            // Report new sim opponents
            if (!newShips.isEmpty())
            {
                Collections.sort(newShips);
                MessageUtils.showMessage("New ships added to computer simulation banks:",
                        CollectionUtils.implode(newShips), true);
                newShips.clear();
            }
            if (!newWings.isEmpty())
            {
                Collections.sort(newWings);
                MessageUtils.showMessage("New squadrons added to computer simulation banks:",
                        CollectionUtils.implode(newWings), true);
                newWings.clear();
            }
        }
    }
}
