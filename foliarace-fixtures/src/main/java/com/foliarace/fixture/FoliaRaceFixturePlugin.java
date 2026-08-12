package com.foliarace.fixture;

import com.foliarace.plugin.FoliaRaceObservations;
import com.foliarace.core.observation.OperationCategory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Test-only plugin. Never install this artifact on a production server. */
public final class FoliaRaceFixturePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        if (world == null) {
            getLogger().severe("No world available for fixture scenario");
            return;
        }

        String scenario = getConfig().getString("scenario", "cross-region-unsafe").toLowerCase(Locale.ROOT);
        Location origin = new Location(world, 0.0, 80.0, 0.0);
        Location distant = new Location(world, 1024.0, 80.0, 1024.0);
        switch (scenario) {
            case "same-region-safe" -> scheduleRegion(origin, origin, "safe");
            case "async-state-access" -> Bukkit.getAsyncScheduler().runDelayed(this, task -> observe(distant, "async"), 2, TimeUnit.SECONDS);
            case "cross-region-unsafe" -> scheduleRegion(origin, distant, "cross-region");
            default -> getLogger().warning("Unknown fixture scenario: " + scenario);
        }
    }

    private void scheduleRegion(Location executionLocation, Location target, String label) {
        Bukkit.getRegionScheduler().run(this, executionLocation, task -> observe(target, label));
    }

    private void observe(Location target, String label) {
        target.getWorld().getBlockAt(target);
        var receipt = FoliaRaceObservations.observeLocationAccess(this, target, OperationCategory.BLOCK_ACCESS);
        getLogger().info("fixture scenario=" + label + ", block access issued, explicitAccepted=" + receipt.accepted());
    }
}
