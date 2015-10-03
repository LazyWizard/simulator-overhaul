package org.lazywizard.newsim;

import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

class InfiniteCRPlugin extends BaseEveryFrameCombatPlugin
{
    private static final Logger Log = Logger.getLogger(InfiniteCRPlugin.class);
    private static final float TIME_BETWEEN_CHECKS = 3f;
    private float timeUntilNextCheck = 0f;

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused())
        {
            return;
        }

        timeUntilNextCheck -= amount;
        if (timeUntilNextCheck <= 0f)
        {
            timeUntilNextCheck = TIME_BETWEEN_CHECKS;
            for (ShipAPI ship : engine.getShips())
            {
                if (ship.isHulk() || ship.isShuttlePod()
                        || !ship.losesCRDuringCombat())
                {
                    continue;
                }

                ship.setCurrentCR(Math.max(ship.getCurrentCR(), ship.getCRAtDeployment()));
                ship.getMutableStats().getPeakCRDuration().modifyFlat(
                        "lw_asirb", ship.getTimeDeployedForCRReduction());
            }
        }
    }
}
