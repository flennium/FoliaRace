package com.foliarace.plugin;

import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.ObservationOrigin;
import com.foliarace.core.observation.OperationCategory;
import com.foliarace.core.context.ExecutionContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Map;

/**
 * Explicit observation entry points for fixture plugins and targeted integrations.
 * These methods never modify or reschedule the observed operation.
 */
public final class FoliaRaceObservations {
    private static volatile FoliaRacePlugin runtime;

    private FoliaRaceObservations() {
    }

    static void install(FoliaRacePlugin plugin) {
        runtime = plugin;
    }

    static void uninstall(FoliaRacePlugin plugin) {
        runtime = runtime == plugin ? null : runtime;
    }

    public static ObservationReceipt observeLocationAccess(Plugin source, Location location, OperationCategory category) {
        if (location == null) {
            return ObservationReceipt.unavailable("location is null");
        }
        FoliaRacePlugin plugin = runtime;
        if (plugin == null) {
            return ObservationReceipt.unavailable("FoliaRace is not enabled");
        }
        if (!plugin.acceptSample()) {
            return ObservationReceipt.unavailable("observation sampled out by configuration");
        }
        Instant observedAt = Instant.now();
        return observe(plugin, source, location, category,
                plugin.runtimeAdapter().classifyLocationContext(location, observedAt),
                plugin.runtimeAdapter().resolveLocationOwnership(location, observedAt));
    }

    public static ObservationReceipt observeEntityAccess(Plugin source, Entity entity, OperationCategory category) {
        if (entity == null) {
            return ObservationReceipt.unavailable("entity is null");
        }
        FoliaRacePlugin plugin = runtime;
        if (plugin == null) {
            return ObservationReceipt.unavailable("FoliaRace is not enabled");
        }
        if (!plugin.acceptSample()) {
            return ObservationReceipt.unavailable("observation sampled out by configuration");
        }
        Instant observedAt = Instant.now();
        return observe(plugin, source, entity, category,
                plugin.runtimeAdapter().classifyEntityContext(entity, observedAt),
                plugin.runtimeAdapter().resolveEntityOwnership(entity, observedAt));
    }

    public static ObservationReceipt observeGlobalAccess(Plugin source, OperationCategory category) {
        FoliaRacePlugin plugin = runtime;
        if (plugin == null) {
            return ObservationReceipt.unavailable("FoliaRace is not enabled");
        }
        if (!plugin.acceptSample()) {
            return ObservationReceipt.unavailable("observation sampled out by configuration");
        }
        Instant observedAt = Instant.now();
        return observe(plugin, source, new Object(), category,
                plugin.runtimeAdapter().classifyCurrentContext(observedAt),
                com.foliarace.core.evidence.OwnershipEvidence.unknown(observedAt));
    }

    public static ObservationReceipt observeSchedulerSubmission(
            Plugin source,
            String scheduler,
            String targetKind
    ) {
        FoliaRacePlugin plugin = runtime;
        if (plugin == null) {
            return ObservationReceipt.unavailable("FoliaRace is not enabled");
        }
        if (!plugin.acceptSample()) {
            return ObservationReceipt.unavailable("observation sampled out by configuration");
        }
        Instant observedAt = Instant.now();
        return observe(plugin, source, new Object(), OperationCategory.SCHEDULER_SUBMISSION,
                plugin.runtimeAdapter().classifyCurrentContext(observedAt),
                com.foliarace.core.evidence.OwnershipEvidence.unknown(observedAt),
                Map.of(
                        "scheduler", normalize(scheduler),
                        "targetKind", normalize(targetKind)
                ));
    }

    public static ObservationReceipt observeInventoryAccess(Plugin source, Inventory inventory) {
        if (inventory == null) {
            return ObservationReceipt.unavailable("inventory is null");
        }
        if (inventory.getHolder() instanceof Entity entity) {
            return observeEntityAccess(source, entity, OperationCategory.INVENTORY_ACCESS);
        }
        return observeGlobalAccess(source, OperationCategory.INVENTORY_ACCESS);
    }

    private static ObservationReceipt observe(
            FoliaRacePlugin plugin,
            Plugin source,
            Object target,
            OperationCategory category,
            ExecutionContext executionContext,
            com.foliarace.core.evidence.OwnershipEvidence ownership
    ) {
        return observe(plugin, source, target, category, executionContext, ownership,
                Map.of("targetClass", target.getClass().getName()));
    }

    private static ObservationReceipt observe(
            FoliaRacePlugin plugin,
            Plugin source,
            Object target,
            OperationCategory category,
            ExecutionContext executionContext,
            com.foliarace.core.evidence.OwnershipEvidence ownership,
            Map<String, String> metadata
    ) {
        if (source == null) {
            return ObservationReceipt.unavailable("source plugin is null");
        }
        Instant now = Instant.now();

        String pluginName = source.getName();
        CallSite callSite = plugin.captureCallSite();
        Observation observation = new Observation(
                java.util.UUID.randomUUID(),
                now,
                pluginName,
                pluginName,
                category == null ? OperationCategory.UNKNOWN : category,
                executionContext,
                ownership,
                new ObservationOrigin(pluginName, pluginName, "explicit-observation", "unknown", "", callSite),
                callSite,
                metadata
        );
        boolean accepted = plugin.recordObservation(observation);
        boolean available = ownership.source() == com.foliarace.core.evidence.ResolutionSource.AUTHORITATIVE_API;
        return new ObservationReceipt(
                accepted,
                available,
                ownership.currentContextOwnsTarget(),
                accepted ? (available ? "submitted" : "submitted with unknown ownership") : "pipeline rejected observation"
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

}
