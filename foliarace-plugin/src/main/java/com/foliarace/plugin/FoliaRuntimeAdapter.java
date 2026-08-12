package com.foliarace.plugin;

import com.foliarace.core.runtime.RuntimeAdapter;
import com.foliarace.core.runtime.RuntimeDescriptor;
import org.bukkit.Bukkit;

/**
 * Initial adapter boundary. Ownership and scheduler queries will be added here
 * once the first real Folia integration fixtures exist.
 */
public final class FoliaRuntimeAdapter implements RuntimeAdapter {
    public static final String VERSION = "26.2.build.4-beta";

    @Override
    public RuntimeDescriptor describe() {
        return new RuntimeDescriptor(
                "Folia",
                Bukkit.getVersion(),
                System.getProperty("java.version", "unknown"),
                VERSION,
                "limited: runtime identity available; ownership hooks pending"
        );
    }
}
