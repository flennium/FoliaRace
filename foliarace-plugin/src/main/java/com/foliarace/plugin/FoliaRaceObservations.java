package com.foliarace.plugin;

import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.ObservationOrigin;
import com.foliarace.core.observation.OperationCategory;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.List;

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
        return plugin == null
                ? ObservationReceipt.unavailable("FoliaRace is not enabled")
                : observe(plugin, source, location, category, plugin.runtimeAdapter().resolveLocationOwnership(location, Instant.now()));
    }

    public static ObservationReceipt observeEntityAccess(Plugin source, Entity entity, OperationCategory category) {
        if (entity == null) {
            return ObservationReceipt.unavailable("entity is null");
        }
        FoliaRacePlugin plugin = runtime;
        return plugin == null
                ? ObservationReceipt.unavailable("FoliaRace is not enabled")
                : observe(plugin, source, entity, category, plugin.runtimeAdapter().resolveEntityOwnership(entity, Instant.now()));
    }

    public static ObservationReceipt observeGlobalAccess(Plugin source, OperationCategory category) {
        FoliaRacePlugin plugin = runtime;
        return plugin == null
                ? ObservationReceipt.unavailable("FoliaRace is not enabled")
                : observe(plugin, source, new Object(), category, com.foliarace.core.evidence.OwnershipEvidence.unknown(Instant.now()));
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
            com.foliarace.core.evidence.OwnershipEvidence ownership
    ) {
        if (source == null) {
            return ObservationReceipt.unavailable("source plugin is null");
        }
        Instant now = Instant.now();

        String pluginName = source.getName();
        Observation observation = new Observation(
                java.util.UUID.randomUUID(),
                now,
                pluginName,
                pluginName,
                category == null ? OperationCategory.UNKNOWN : category,
                plugin.runtimeAdapter().classifyCurrentContext(now),
                ownership,
                new ObservationOrigin(pluginName, pluginName, "explicit-observation", "unknown", "", callSite()),
                callSite(),
                java.util.Map.of("targetClass", target.getClass().getName())
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

    private static CallSite callSite() {
        List<String> frames = StackWalker.getInstance().walk(stream -> stream
                .filter(frame -> !frame.getClassName().startsWith("com.foliarace.plugin."))
                .limit(8)
                .map(frame -> frame.getClassName() + "#" + frame.getMethodName())
                .toList());
        return new CallSite(frames.isEmpty() ? "unknown" : frames.getFirst(), frames);
    }
}
