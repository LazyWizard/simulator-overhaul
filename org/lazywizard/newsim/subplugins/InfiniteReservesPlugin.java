package org.lazywizard.newsim.subplugins;

import java.util.List;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;

/**
 *
 * @author LazyWizard
 */
public class InfiniteReservesPlugin extends BaseEveryFrameCombatPlugin
{
    private static final Logger Log = Logger.getLogger(InfiniteReservesPlugin.class);

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
    }
}
