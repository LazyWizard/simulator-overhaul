package org.lazywizard.newsim;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

// Cleans up hulks after they've been on the battle map for a while.
class HulkCleanerPlugin extends BaseEveryFrameCombatPlugin
{
    private static final Logger Log = Logger.getLogger(HulkCleanerPlugin.class);
    private static final float TIME_BETWEEN_CHECKS = 1f;
    private static final float TIME_TO_CLEANUP = 30f;
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

            // Register any new hulks for later cleanup
            for (ShipAPI ship : engine.getShips())
            {
                if (ship.isHulk() && !toCleanup.containsKey(ship))
                {
                    toCleanup.put(ship, curTime + TIME_TO_CLEANUP);
                }
            }

            // Check if any ships have existed long enough for cleanup
            // If so, damage them enough to cause an explosion
            for (Iterator<Map.Entry<ShipAPI, Float>> iter = toCleanup.entrySet().iterator(); iter.hasNext();)
            {
                final Map.Entry<ShipAPI, Float> entry = iter.next();
                if (curTime > entry.getValue())
                {
                    engine.applyDamage(entry.getKey(), entry.getKey().getLocation(),
                            9_999_999f, DamageType.OTHER, 0f, true, false, null);
                    iter.remove();
                }
            }
        }
    }
}
