package org.lazywizard.newsim.comparators;

import java.util.Comparator;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class SortByHullSize implements Comparator<FleetMemberAPI>
    {
        @Override
        public int compare(FleetMemberAPI o1, FleetMemberAPI o2)
        {
            // Hulls go at the end of the list
            final boolean isO1Hull = o1.getVariant().isEmptyHullVariant(),
                    isO2Hull = o2.getVariant().isEmptyHullVariant();
            if (isO1Hull && !isO2Hull)
            {
                return 1;
            }
            else if (isO2Hull && !isO1Hull)
            {
                return -1;
            }

            // Sort ships of same hull size by their name
            if (o1.getHullSpec().getHullSize() == o2.getHullSpec().getHullSize())
            {
                return o1.getVariant().getFullDesignationWithHullName().compareTo(
                        o2.getVariant().getFullDesignationWithHullName());
            }

            // Sort ships by hull size
            return o2.getHullSpec().getHullSize().compareTo(
                    o1.getHullSpec().getHullSize());
        }
    }
