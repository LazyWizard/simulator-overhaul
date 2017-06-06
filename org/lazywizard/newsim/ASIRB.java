package org.lazywizard.newsim;

import java.util.Set;
import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class ASIRB implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
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

        final Set<String> simOpponents = SimMaster.getAllKnownShipsActual();

        // Add all variants
        int totalVariants = 0;
        for (String id : Global.getSettings().getAllVariantIds())
        {
            if (SimUtils.isValidVariant(Global.getSettings().getVariant(id)) && simOpponents.add(id))
            {
                System.out.println("Added variant " + id);
                totalVariants++;
            }
        }

        Console.showMessage("Unlocked " + totalVariants + " variants; "
                + simOpponents.size() + " opponents are now known to the simulator.");
        return CommandResult.SUCCESS;
    }
}
