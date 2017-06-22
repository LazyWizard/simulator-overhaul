package org.lazywizard.newsim.subplugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Cleans up hulks after they've been on the battle map for a while.
public class HulkCleanerPlugin extends BaseEveryFrameCombatPlugin
{
    private static final Logger Log = Logger.getLogger(HulkCleanerPlugin.class);
    private static final float TIME_BETWEEN_CHECKS = 1f;
    private static final float TIME_TO_CLEANUP_HULKS = 30f;
    private final Map<ShipAPI, Float> toCleanup = new LinkedHashMap<>();
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
            float curTime = engine.getTotalElapsedTime(false);

            // Register any new hulks and ship pieces for later cleanup
            for (ShipAPI ship : engine.getShips())
            {
                if ((ship.isHulk() || ship.isPiece()) && !toCleanup.containsKey(ship))
                {
                    toCleanup.put(ship, curTime + TIME_TO_CLEANUP_HULKS);
                }
            }

            // Check if any ships have existed long enough for cleanup
            // If so, damage them enough to cause an explosion
            for (Iterator<Map.Entry<ShipAPI, Float>> iter = toCleanup.entrySet().iterator(); iter.hasNext(); )
            {
                final Map.Entry<ShipAPI, Float> entry = iter.next();
                if (curTime > entry.getValue())
                {
                    engine.removeEntity(entry.getKey());
                    iter.remove();
                }
            }
        }
    }
}
