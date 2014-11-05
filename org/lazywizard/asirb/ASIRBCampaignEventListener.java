package org.lazywizard.asirb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
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

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result)
    {
        // TODO: Add config option for requiring player victory for opponent unlock
        // TODO: Add config option for wiping simulator list on player fleet destruction
        if (!result.didPlayerWin())
        {
            return;
        }

        final List<DeployedFleetMemberAPI> seenShips
                = result.getLoserResult().getAllEverDeployedCopy();
        seenShips.addAll(result.getWinnerResult().getAllEverDeployedCopy());

        for (DeployedFleetMemberAPI dfm : seenShips)
        {
            ShipAPI ship = dfm.getShip();
            if (ASIRBMaster.checkAddOpponent(ship))
            {
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
        // TODO: Add 'show message' option to config, return immediately if false
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null || ui.isShowingDialog())
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
