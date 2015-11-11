package org.lazywizard.newsim;

import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;

public interface ReserveRegenerator extends EveryFrameCombatPlugin
{
    public void checkReserves();
}
