package org.lazywizard.newsim;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class SimModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        SimSettings.reload();
    }

    //@Override
    public void onGameLoad(boolean newGame)
    {
        // Doesn't function properly with Starsector+ randomized variants
        if (SimSettings.USE_VANILLA_SIM_LIST)
        {
            return;
        }

        final SimCampaignEventListener listener = new SimCampaignEventListener();
        Global.getSector().addTransientListener(listener);
        Global.getSector().addTransientScript(listener);
    }

    // TODO: Remove after 0.7a
    public void onGameLoad()
    {
        onGameLoad(false);
    }
}
