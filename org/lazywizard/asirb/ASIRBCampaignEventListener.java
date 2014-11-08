package org.lazywizard.asirb;

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
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

class ASIRBCampaignEventListener extends BaseCampaignEventListener implements EveryFrameScript
{
    private static final float TIME_BETWEEN_REPORT_CHECKS = .25f;
    private final List<String> newShips, newWings;
    private float timeUntilReportCheck = TIME_BETWEEN_REPORT_CHECKS;

    ASIRBCampaignEventListener()
    {
        super(false);
        newShips = new ArrayList<>();
        newWings = new ArrayList<>();
    }

    private static boolean wasFullyDestroyed(EngagementResultForFleetAPI fleet)
    {
        /*System.out.println("Reserves size: " + fleet.getReserves().size());
        System.out.println("Deployed size: " + fleet.getDeployed().size());
        System.out.println("Retreated size: " + fleet.getRetreated().size());
        System.out.println("Is valid: " + fleet.getFleet().isValidPlayerFleet());*/
        return (fleet.getReserves().isEmpty() && fleet.getDeployed().isEmpty()
                && fleet.getRetreated().isEmpty());
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result)
    {
        if (!result.didPlayerWin())
        {
            // Wipe sim list on fleet death if that option is enabled
            if (ASIRBSettings.WIPE_SIM_DATA_ON_PLAYER_DEATH
                    && wasFullyDestroyed(result.getLoserResult()))
            {
                ASIRBMaster.getAllKnownShips().clear();
            }

            // Don't remember opponents on a loss if that option is enabled
            if (ASIRBSettings.REQUIRE_PLAYER_VICTORY_TO_UNLOCK)
            {
                return;
            }
        }

        // Check the deployed ships for opponents we've never fought before
        final List<DeployedFleetMemberAPI> seenShips
                = result.getLoserResult().getAllEverDeployedCopy();
        seenShips.addAll(result.getWinnerResult().getAllEverDeployedCopy());
        for (DeployedFleetMemberAPI dfm : seenShips)
        {
            ShipAPI ship = dfm.getShip();
            if (ASIRBMaster.checkAddOpponent(ship))
            {
                // If we aren't announcing new opponents, don't track them
                if (!ASIRBSettings.SHOW_UNLOCKED_OPPONENTS)
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
        if (ui == null || ui.isShowingDialog() || !ASIRBSettings.SHOW_UNLOCKED_OPPONENTS)
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
