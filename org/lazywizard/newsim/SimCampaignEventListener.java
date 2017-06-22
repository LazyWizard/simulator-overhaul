package org.lazywizard.newsim;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

class SimCampaignEventListener extends BaseCampaignEventListener implements EveryFrameScript
{
    private static final float TIME_BETWEEN_REPORT_CHECKS = .25f;
    private final Set<String> newShips;
    private String lastFleetId = null;
    private boolean listCleared = false;
    private float timeUntilReportCheck = TIME_BETWEEN_REPORT_CHECKS;

    SimCampaignEventListener()
    {
        super(false);
        newShips = new HashSet<>();
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result)
    {
        // TODO: Test multi-battle support
        if (!result.didPlayerWin())
        {
            // Don't remember opponents on a loss if that option is enabled
            if (SimSettings.REQUIRE_PLAYER_VICTORY_TO_UNLOCK)
            {
                return;
            }
        }

        // Don't include battles fought by second in command
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
                newShips.add(ship.getVariant().getFullDesignationWithHullName());
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
        final CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null || ui.isShowingDialog() || Global.getSector().isInNewGameAdvance())
        {
            return;
        }

        // Wipe sim list on fleet death if that option is enabled
        if (SimSettings.WIPE_SIM_DATA_ON_PLAYER_DEATH)
        {
            final CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player == null)
            {
                return;
            }

            final String currentId = player.getId();
            if (lastFleetId == null)
            {
                lastFleetId = currentId;
            }

            if (!currentId.equals(lastFleetId))
            {
                newShips.clear();
                SimMaster.resetDefaultSimList();
                listCleared = true;
                lastFleetId = currentId;
                Log.debug("Cleared sim list due to player death");
            }
        }

        // Minor optimization: only check for new ships a few times per second
        timeUntilReportCheck -= amount;
        if (timeUntilReportCheck <= 0f)
        {
            timeUntilReportCheck = TIME_BETWEEN_REPORT_CHECKS;

            // Report simlist cleared
            if (listCleared)
            {
                ui.addMessage("All recorded combat simulator data was lost in the"
                        + " destruction of your fleet.", Color.RED);
                listCleared = false;
                return;
            }

            // Report new sim opponents
            if (SimSettings.SHOW_UNLOCKED_OPPONENTS)
            {
                if (!newShips.isEmpty())
                {
                    final List<String> sortedNewShips = new ArrayList<>(newShips);
                    Collections.sort(sortedNewShips);
                    MessageUtils.showMessage("New vessels added to computer simulation banks:",
                            CollectionUtils.implode(sortedNewShips), true);
                    newShips.clear();
                }
            }
        }
    }
}
