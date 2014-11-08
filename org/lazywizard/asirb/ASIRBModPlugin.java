package org.lazywizard.asirb;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class ASIRBModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        ASIRBSettings.reload();
    }

    @Override
    public void onGameLoad()
    {
        final ASIRBCampaignEventListener listener = new ASIRBCampaignEventListener();
        Global.getSector().addTransientListener(listener);
        Global.getSector().addTransientScript(listener);
        ASIRBMaster.checkLegacy();
    }
}
