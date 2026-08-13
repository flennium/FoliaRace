package com.foliarace.plugin;

import com.foliarace.core.runtime.AdapterCapability;
import com.foliarace.core.runtime.CompatibilityMatrix;
import com.foliarace.core.runtime.CompatibilityResolution;
import com.foliarace.core.runtime.RuntimeAdapter;
import com.foliarace.core.runtime.RuntimeDescriptor;
import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.OwnershipEvidence;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Initial adapter boundary. Ownership and scheduler queries will be added here
 * once the first real Folia integration fixtures exist.
 */
public final class FoliaRuntimeAdapter implements RuntimeAdapter<Location, Entity> {
    public static final String ADAPTER_VERSION = "0.2.0";

    @Override
    public RuntimeDescriptor describe() {
        String runtimeVersion = runtimeVersion();
        Set<AdapterCapability> capabilities = detectCapabilities();
        CompatibilityResolution compatibility = CompatibilityMatrix.resolve(runtimeVersion, capabilities);
        return new RuntimeDescriptor(
                "Folia",
                runtimeVersion,
                System.getProperty("java.version", "unknown"),
                ADAPTER_VERSION,
                compatibility.status().name().toLowerCase(java.util.Locale.ROOT) + ": " + compatibility.reason(),
                compatibility.status(),
                compatibility.profileId(),
                compatibility.reason(),
                compatibility.capabilities()
        );
    }

    @Override
    public ExecutionContext classifyCurrentContext(Instant observedAt) {
        String threadName = Thread.currentThread().getName();
        try {
            if (Boolean.TRUE.equals(invokeBoolean(Bukkit.class, "isGlobalTickThread"))) {
                return new ExecutionContext(ExecutionContextType.GLOBAL_REGION, "global", threadName, observedAt);
            }
        } catch (LinkageError ignored) {
            // An unknown context is safer than inferring ownership from a name.
        }
        String normalizedThreadName = threadName.toLowerCase(java.util.Locale.ROOT);
        if (normalizedThreadName.contains("async") || normalizedThreadName.contains("forkjoin")) {
            return new ExecutionContext(ExecutionContextType.ASYNC, "", threadName, observedAt);
        }
        if (normalizedThreadName.contains("folia") || normalizedThreadName.contains("plugin")) {
            return new ExecutionContext(ExecutionContextType.PLUGIN_THREAD, "", threadName, observedAt);
        }
        return ExecutionContext.unknown(observedAt, threadName);
    }

    @Override
    public ExecutionContext classifyLocationContext(Location location, Instant observedAt) {
        ExecutionContext current = classifyCurrentContext(observedAt);
        if (current.type() != ExecutionContextType.UNKNOWN) {
            return current;
        }
        if (Boolean.TRUE.equals(invokeOwnershipCheck(Location.class, location))) {
            return new ExecutionContext(ExecutionContextType.REGION, "", current.threadName(), observedAt);
        }
        return current;
    }

    @Override
    public ExecutionContext classifyEntityContext(Entity entity, Instant observedAt) {
        ExecutionContext current = classifyCurrentContext(observedAt);
        if (current.type() != ExecutionContextType.UNKNOWN) {
            return current;
        }
        if (Boolean.TRUE.equals(invokeOwnershipCheck(Entity.class, entity))) {
            return new ExecutionContext(ExecutionContextType.REGION, "", current.threadName(), observedAt);
        }
        return current;
    }

    @Override
    public OwnershipEvidence resolveLocationOwnership(Location location, Instant observedAt) {
        Boolean ownsTarget = invokeOwnershipCheck(Location.class, location);
        return ownsTarget == null
                ? OwnershipEvidence.unknown(observedAt)
                : OwnershipEvidence.authoritativeCurrentContextCheck(ownsTarget, observedAt);
    }

    @Override
    public OwnershipEvidence resolveEntityOwnership(Entity entity, Instant observedAt) {
        Boolean ownsTarget = invokeOwnershipCheck(Entity.class, entity);
        return ownsTarget == null
                ? OwnershipEvidence.unknown(observedAt)
                : OwnershipEvidence.authoritativeCurrentContextCheck(ownsTarget, observedAt);
    }

    private static String runtimeVersion() {
        String version = Bukkit.getBukkitVersion();
        if (version == null || version.isBlank()) {
            version = Bukkit.getVersion();
        }
        return version == null ? "unknown" : version;
    }

    private static Set<AdapterCapability> detectCapabilities() {
        EnumSet<AdapterCapability> capabilities = EnumSet.of(AdapterCapability.RUNTIME_IDENTITY);
        try {
            Class<?> serverType = Class.forName("org.bukkit.Server");
            if (hasMethod(serverType, "getGlobalRegionScheduler")) {
                capabilities.add(AdapterCapability.GLOBAL_SCHEDULER);
            }
            if (hasMethod(serverType, "getRegionScheduler")) {
                capabilities.add(AdapterCapability.REGION_SCHEDULER);
            }
            if (hasMethod(serverType, "getAsyncScheduler")) {
                capabilities.add(AdapterCapability.ASYNC_SCHEDULER);
            }
            if (hasMethod(Entity.class, "getScheduler")) {
                capabilities.add(AdapterCapability.ENTITY_SCHEDULER);
            }
            if (hasMethod(Bukkit.class, "isOwnedByCurrentRegion", Location.class)) {
                capabilities.add(AdapterCapability.REGION_OWNERSHIP_CHECK);
            }
            if (hasMethod(Bukkit.class, "isOwnedByCurrentRegion", Entity.class)) {
                capabilities.add(AdapterCapability.ENTITY_OWNERSHIP_CHECK);
            }
        } catch (LinkageError | ReflectiveOperationException ignored) {
            // An unavailable API surface is represented by missing capabilities.
        }
        return Set.copyOf(capabilities);
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method method = type.getMethod(name, parameterTypes);
            return method != null;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private static Boolean invokeOwnershipCheck(Class<?> targetType, Object target) {
        try {
            Method method = Bukkit.class.getMethod("isOwnedByCurrentRegion", targetType);
            return (Boolean) method.invoke(null, target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {
            return null;
        }
    }

    private static Boolean invokeBoolean(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            return (Boolean) method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {
            return null;
        }
    }
}
