package com.foliarace.plugin;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.ObservationOrigin;
import com.foliarace.core.observation.OperationCategory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        return observe(source, location, category, Location.class, "isOwnedByCurrentRegion");
    }

    public static ObservationReceipt observeEntityAccess(Plugin source, Entity entity, OperationCategory category) {
        if (entity == null) {
            return ObservationReceipt.unavailable("entity is null");
        }
        return observe(source, entity, category, Entity.class, "isOwnedByCurrentRegion");
    }

    private static ObservationReceipt observe(
            Plugin source,
            Object target,
            OperationCategory category,
            Class<?> targetType,
            String methodName
    ) {
        FoliaRacePlugin plugin = runtime;
        if (plugin == null) {
            return ObservationReceipt.unavailable("FoliaRace is not enabled");
        }
        if (source == null) {
            return ObservationReceipt.unavailable("source plugin is null");
        }
        Instant now = Instant.now();
        Boolean ownsTarget = invokeOwnershipCheck(targetType, methodName, target);
        if (ownsTarget == null) {
            return ObservationReceipt.unavailable("authoritative ownership API is unavailable");
        }

        String pluginName = source.getName();
        Observation observation = new Observation(
                java.util.UUID.randomUUID(),
                now,
                pluginName,
                pluginName,
                category == null ? OperationCategory.UNKNOWN : category,
                new ExecutionContext(
                        ExecutionContextType.REGION,
                        ownsTarget ? "current-region" : "unknown-region",
                        Thread.currentThread().getName(),
                        now
                ),
                OwnershipEvidence.authoritativeCurrentContextCheck(ownsTarget, now),
                new ObservationOrigin(pluginName, pluginName, "explicit-observation", "unknown", "", callSite()),
                callSite(),
                java.util.Map.of("targetClass", target.getClass().getName())
        );
        boolean accepted = plugin.recordObservation(observation);
        return new ObservationReceipt(accepted, true, ownsTarget, accepted ? "submitted" : "pipeline rejected observation");
    }

    private static Boolean invokeOwnershipCheck(Class<?> targetType, String methodName, Object target) {
        try {
            Method method = Bukkit.class.getMethod(methodName, targetType);
            return (Boolean) method.invoke(null, target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {
            return null;
        }
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
