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

    @Override
    public void onGameLoad()
    {
        // Doesn't function properly with Starsector+ randomized variants
        if (SimSettings.IS_SSP_ENABLED)
        {
            return;
        }

        final SimCampaignEventListener listener = new SimCampaignEventListener();
        Global.getSector().addTransientListener(listener);
        Global.getSector().addTransientScript(listener);
        SimMaster.checkLegacy();
    }
}
