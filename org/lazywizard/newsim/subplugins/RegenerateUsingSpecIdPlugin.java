package org.lazywizard.newsim.subplugins;

import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;
import org.lazywizard.newsim.ReserveRegenerator;

public class RegenerateUsingSpecIdPlugin extends BaseEveryFrameCombatPlugin
        implements ReserveRegenerator
{
    private static final Logger Log = Logger.getLogger(RegenerateUsingSpecIdPlugin.class);
    private final FleetSide side;
    private final CombatFleetManagerAPI fm;
    private final Map<String, FleetMemberType> reserves;
    private int lastSize;
    private boolean needsRecheck = true;

    public RegenerateUsingSpecIdPlugin(FleetSide side,
            Map<String, FleetMemberType> reserves)
    {
        this.side = side;
        this.reserves = reserves;
        fm = Global.getCombatEngine().getFleetManager(side);
        lastSize = reserves.size();
        checkReserves();
    }

    @Override
    public void checkReserves()
    {
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // Only check for ship replacement after player uses picker dialog
        if (Global.getCombatEngine().isUIShowingDialog())
        {
            needsRecheck = true;
            return;
        }

        if (needsRecheck)
        {
            needsRecheck = false;
            if (fm.getReservesCopy().size() != lastSize)
            {
                Log.debug("Regenerating side: " + side.name());
                checkReserves();
            }
        }
    }
}
