package com.foliarace.plugin;

import com.foliarace.core.runtime.RuntimeAdapter;
import com.foliarace.core.runtime.AdapterCapability;
import com.foliarace.core.runtime.CompatibilityMatrix;
import com.foliarace.core.runtime.CompatibilityResolution;
import com.foliarace.core.runtime.RuntimeDescriptor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;

/**
 * Initial adapter boundary. Ownership and scheduler queries will be added here
 * once the first real Folia integration fixtures exist.
 */
public final class FoliaRuntimeAdapter implements RuntimeAdapter {
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
}
