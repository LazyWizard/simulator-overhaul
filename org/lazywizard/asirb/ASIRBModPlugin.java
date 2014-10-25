package org.lazywizard.asirb;

import java.util.List;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.apache.log4j.Level;

// TODO: Remove 'replace' tag in mod_info.json after next hotfix
public class ASIRBModPlugin extends BaseModPlugin
{
    private static final Level LOG_LEVEL = Level.WARN;
    private static final CampaignEventListener listener;

    static
    {
        listener = new BaseCampaignEventListener(false)
        {
            @Override
            public void reportPlayerEngagement(EngagementResultAPI result)
            {
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

    @Override
    public void onApplicationLoad() throws Exception
    {
        Global.getLogger(ASIRBModPlugin.class).setLevel(LOG_LEVEL);
        Global.getLogger(ASIRBMaster.class).setLevel(LOG_LEVEL);
        Global.getLogger(ASIRBPlugin.class).setLevel(LOG_LEVEL);
    }

    @Override
    public void onGameLoad()
    {
        Global.getSector().addTransientListener(listener);
    }

    @Override
    public void onNewGameAfterTimePass()
    {

        Global.getSector().addTransientListener(listener);
    }
}
