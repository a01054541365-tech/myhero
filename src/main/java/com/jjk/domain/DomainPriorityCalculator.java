package com.jjk.domain;

import com.jjk.data.PlayerData;

public class DomainPriorityCalculator {

    public int calculate(DomainInstance domain, PlayerData ownerData) {
        // TODO: priority formula based on CE, grade, domain type
        return 0;
    }

    public DomainInstance resolveConflict(DomainInstance a, PlayerData dataA,
                                          DomainInstance b, PlayerData dataB) {
        int priorityA = calculate(a, dataA);
        int priorityB = calculate(b, dataB);
        return priorityA >= priorityB ? a : b;
    }
}
