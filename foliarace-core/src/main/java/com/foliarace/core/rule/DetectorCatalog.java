package com.foliarace.core.rule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class DetectorCatalog {
    private static final Map<String, Supplier<DetectorRule>> FACTORIES = Map.of(
            CrossRegionOwnershipRule.ID, CrossRegionOwnershipRule::new,
            CrossEntityOwnershipRule.ID, CrossEntityOwnershipRule::new,
            AsyncServerStateRule.ID, AsyncServerStateRule::new,
            SchedulerMisuseRule.ID, SchedulerMisuseRule::new
    );

    private DetectorCatalog() {
    }

    public static Set<String> ids() {
        return FACTORIES.keySet();
    }

    public static List<DetectorRule> create(Set<String> enabledIds) {
        if (enabledIds == null) {
            throw new IllegalArgumentException("enabled detector set is required");
        }
        Set<String> unknown = enabledIds.stream().filter(id -> !FACTORIES.containsKey(id)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown detector IDs: " + unknown);
        }
        return enabledIds.stream().sorted().map(id -> FACTORIES.get(id).get()).toList();
    }
}
