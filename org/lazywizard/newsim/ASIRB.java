package org.lazywizard.newsim;

import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class ASIRB implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign() && context != CommandContext.COMBAT_CAMPAIGN)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (SimSettings.USE_VANILLA_SIM_LIST)
        {
            Console.showMessage("Simulator unlocking is disabled, so this"
                    + " command wouldn't do much, would it?");
            return CommandResult.ERROR;
        }

        final Map<String, FleetMemberType> simOpponents = SimMaster.getAllKnownShipsActual();

        // Add all variants
        int totalVariants = 0;
        final List<String> wingIds = Global.getSector().getAllFighterWingIds();
        for (String variant : Global.getSettings().getAllVariantIds())
        {
            // Workaround for getAllVariantIds() containing fighters for some reason
            if (variant.endsWith("_Hull") || wingIds.contains(variant+"_wing"))
            {
                continue;
            }

            if (simOpponents.put(variant, FleetMemberType.SHIP) == null)
            {
                System.out.println("Added variant " + variant);
                totalVariants++;
            }
        }

        // Add all wings
        int totalWings = 0;
        for (String wing : wingIds)
        {
            if (simOpponents.put(wing, FleetMemberType.FIGHTER_WING) == null)
            {
                System.out.println("Added wing " + wing);
                totalWings++;
            }
        }

        Console.showMessage("Unlocked " + totalVariants + " variants and "
                + totalWings + " wings; " + simOpponents.size()
                + " opponents are now known to the simulator ("
                + (totalVariants + totalWings) + " new).");
        return CommandResult.SUCCESS;
    }
}
