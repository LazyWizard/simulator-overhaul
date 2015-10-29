package org.lazywizard.newsim.plugins;

import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;

public class HealOnVictoryPlugin extends BaseEveryFrameCombatPlugin
{
    private static final Logger Log = Logger.getLogger(HealOnVictoryPlugin.class);
    private static final float TIME_BETWEEN_CHECKS = 5f;
    private float timeUntilNextCheck = 0f;
    private boolean alreadyHealed = true;

    private static void restoreShip(ShipAPI ship)
    {
        // Restore hull, armor, and combat readiness
        ship.setHitpoints(ship.getMaxHitpoints() * ship.getHullLevelAtDeployment());
        ship.setCurrentCR(ship.getCRAtDeployment());
        final ArmorGridAPI grid = ship.getArmorGrid();
        final float maxArmor = grid.getMaxArmorInCell();
        final float sizeX = grid.getGrid().length,
                sizeY = grid.getGrid()[0].length;
        for (int x = 0; x < sizeX; x++)
        {
            for (int y = 0; y < sizeY; y++)
            {
                grid.setArmorValue(x, y, maxArmor);
            }
        }

        // Restore system
        ShipSystemAPI system = ship.getSystem();
        if (system != null)
        {
            system.setAmmo(system.getMaxAmmo());
        }

        // Restore drones
        List<ShipAPI> drones = ship.getDeployedDrones();
        if (drones != null && !drones.isEmpty())
        {
            for (ShipAPI drone : drones)
            {
                restoreShip(drone);
            }
        }

        // Restore weapons
        for (WeaponAPI weapon : ship.getAllWeapons())
        {
            if (weapon.usesAmmo())
            {
                weapon.resetAmmo();
            }

            if (weapon.isDisabled())
            {
                weapon.repair();
            }
        }

        // TODO: Engines are not restorable in current API, recheck after 0.7a
    }

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
            final ShipAPI player = engine.getPlayerShip();
            final boolean isVictor = (player != null && !player.isHulk()
                    && engine.isEntityInPlay(player)
                    && engine.getFleetManager(FleetSide.ENEMY)
                            .getDeployedCopy().isEmpty());

            // Only heal once per victory
            if (isVictor && !alreadyHealed)
            {
                Log.debug("Healing player");
                restoreShip(player);
                alreadyHealed = true;
            }
            else if (!isVictor)
            {
                alreadyHealed = false;
            }
        }
    }
}
