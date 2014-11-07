package org.lazywizard.asirb;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;

public class ASIRBModPlugin extends BaseModPlugin
{
    // TODO: Make log level a config file option
    private static final Level LOG_LEVEL = Level.ALL;

    @Override
    public void onApplicationLoad() throws Exception
    {
        // TODO: Add config file, load settings here
        Global.getLogger(ASIRBModPlugin.class).setLevel(LOG_LEVEL);
        Global.getLogger(ASIRBMaster.class).setLevel(LOG_LEVEL);
        Global.getLogger(ASIRBCombatPlugin.class).setLevel(LOG_LEVEL);
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
