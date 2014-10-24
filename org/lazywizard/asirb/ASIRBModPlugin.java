package org.lazywizard.asirb;

import java.util.List;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

public class ASIRBModPlugin extends BaseModPlugin
{
    private static final CampaignEventListener listener;

    static
    {
        listener = new BaseCampaignEventListener(false)
        {
            @Override
            public void reportPlayerEngagement(EngagementResultAPI result)
            {
                //System.out.println("Engagement found");
                final List<DeployedFleetMemberAPI> seenShips
                        = result.getLoserResult().getAllEverDeployedCopy();
                // TODO: Remove check after next hotfix
                if (seenShips != null)
                {
                    seenShips.addAll(result.getWinnerResult().getAllEverDeployedCopy());

                    for (DeployedFleetMemberAPI ship : seenShips)
                    {
                        ASIRBMaster.addOpponent(ship.getShip());
                    }
                }
            }
        };
    }

    @Override public void onGameLoad()
    {
        Global.getSector().addTransientListener(listener);
    }

    @Override
    public void onNewGameAfterTimePass()
    {

        Global.getSector().addTransientListener(listener);
    }
}
