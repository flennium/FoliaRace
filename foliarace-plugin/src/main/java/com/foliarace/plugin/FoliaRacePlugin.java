package com.foliarace.plugin;

import com.foliarace.core.config.ConfigManager;
import com.foliarace.core.config.FoliaRaceConfig;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.finding.Baseline;
import com.foliarace.core.finding.BaselineComparator;
import com.foliarace.core.finding.BaselineComparison;
import com.foliarace.core.finding.FindingGroupSnapshot;
import com.foliarace.core.finding.FindingFilters;
import com.foliarace.core.finding.Suppression;
import com.foliarace.core.finding.SuppressionMatcher;
import com.foliarace.core.config.OutputFormat;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.ci.CiEvaluation;
import com.foliarace.core.ci.CiEvaluator;
import com.foliarace.core.pipeline.ObservationPipeline;
import com.foliarace.core.report.JsonReportWriter;
import com.foliarace.core.report.InstrumentationHealth;
import com.foliarace.core.report.ReportDocument;
import com.foliarace.core.session.DiagnosticSession;
import com.foliarace.core.session.SessionManager;
import com.foliarace.core.rule.CrossRegionOwnershipRule;
import com.foliarace.core.rule.DetectorCatalog;
import com.foliarace.core.rule.DetectorRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.block.Block;
import org.bukkit.Chunk;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class FoliaRacePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private FindingAggregator findingAggregator;
    private ObservationPipeline observationPipeline;
    private SessionManager sessionManager;
    private FoliaRuntimeAdapter runtimeAdapter;
    private DiagnosticSession lastSession;
    private AgentBridgeInstaller agentBridge;
    private List<Suppression> suppressions = List.of();
    private Baseline baseline;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FoliaRaceConfig config;
        try {
            config = PluginConfigLoader.load(this);
        } catch (IllegalArgumentException error) {
            getLogger().severe("Invalid configuration: " + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        configManager = new ConfigManager(config);
        try {
            suppressions = PolicyLoader.suppressions(getDataFolder().toPath().resolve(config.suppressionFile()).toFile());
            baseline = PolicyLoader.baseline(getDataFolder().toPath().resolve(config.baselineFile()).toFile());
        } catch (IllegalArgumentException error) {
            getLogger().severe("Invalid policy file: " + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        findingAggregator = new FindingAggregator();
        sessionManager = new SessionManager();
        runtimeAdapter = new FoliaRuntimeAdapter();

        List<DetectorRule> rules;
        try {
            rules = DetectorCatalog.create(config.enabledDetectors());
        } catch (IllegalArgumentException error) {
            getLogger().severe("Invalid detector configuration: " + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        observationPipeline = new ObservationPipeline(config.observationQueueCapacity(), rules, findingAggregator);
        observationPipeline.start();
        FoliaRaceObservations.install(this);
        agentBridge = new AgentBridgeInstaller();
        agentBridge.install(this);
        startSession("startup");

        PluginCommand command = getCommand("foliarace");
        if (command != null) {
            FoliaRaceCommand executor = new FoliaRaceCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("FoliaRace enabled: " + statusLine());
        if (runtimeAdapter.describe().compatibilityStatus().name().equals("UNSUPPORTED")) {
            getLogger().warning("FoliaRace entered limited mode: " + runtimeAdapter.describe().compatibilityReason());
        }
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) {
            stopSession();
        }
        FoliaRaceObservations.uninstall(this);
        if (observationPipeline != null) {
            observationPipeline.close();
        }
        if (lastSession != null) {
            flushReport();
        }
        if (agentBridge != null) {
            agentBridge.uninstall();
        }
    }

    boolean recordObservation(com.foliarace.core.observation.Observation observation) {
        return observationPipeline != null && observationPipeline.submit(observation);
    }

    boolean acceptSample() {
        return ThreadLocalRandom.current().nextDouble() < configManager.current().samplingRate();
    }

    CallSite captureCallSite() {
        if (configManager.current().overheadMode() == com.foliarace.core.config.OverheadMode.MINIMAL) {
            return CallSite.unknown();
        }
        int depth = configManager.current().overheadMode() == com.foliarace.core.config.OverheadMode.EXHAUSTIVE ? 24 : 8;
        List<String> frames = StackWalker.getInstance().walk(stream -> stream
                .filter(frame -> !frame.getClassName().startsWith("com.foliarace.plugin."))
                .limit(depth)
                .map(frame -> frame.getClassName() + "#" + frame.getMethodName())
                .toList());
        return new CallSite(frames.isEmpty() ? "unknown" : frames.getFirst(), frames);
    }

    FoliaRuntimeAdapter runtimeAdapter() {
        return runtimeAdapter;
    }

    void recordInstrumentedOperation(String ownerType, String methodName, Object receiver, Object[] arguments) {
        Plugin source = findOriginPlugin();
        if (source == null) {
            return;
        }
        if (receiver instanceof Entity entity && (ownerType.endsWith("CraftEntity") || ownerType.endsWith("CraftPlayer"))) {
            FoliaRaceObservations.observeEntityAccess(source, entity, com.foliarace.core.observation.OperationCategory.ENTITY_ACCESS);
            return;
        }
        if (receiver instanceof Inventory inventory) {
            FoliaRaceObservations.observeInventoryAccess(source, inventory);
            return;
        }
        if (receiver instanceof Block block) {
            FoliaRaceObservations.observeLocationAccess(source, block.getLocation(), com.foliarace.core.observation.OperationCategory.BLOCK_ACCESS);
            return;
        }
        if (receiver instanceof Chunk chunk) {
            Location center = new Location(chunk.getWorld(), (chunk.getX() << 4) + 8, 0, (chunk.getZ() << 4) + 8);
            FoliaRaceObservations.observeLocationAccess(source, center, com.foliarace.core.observation.OperationCategory.CHUNK_ACCESS);
            return;
        }
        if (ownerType.endsWith("CraftServer")) {
            FoliaRaceObservations.observeGlobalAccess(source, com.foliarace.core.observation.OperationCategory.SERVER_GLOBAL_ACCESS);
            return;
        }
        if (!(receiver instanceof World world)) {
            return;
        }

        Location location = locationArgument(arguments);
        if (location == null) {
            location = numericLocation(world, arguments);
        }
        if (location == null) {
            return;
        }
        var category = methodName.startsWith("getChunk")
                ? com.foliarace.core.observation.OperationCategory.CHUNK_ACCESS
                : com.foliarace.core.observation.OperationCategory.BLOCK_ACCESS;
        FoliaRaceObservations.observeLocationAccess(source, location, category);
    }

    private Plugin findOriginPlugin() {
        return StackWalker.getInstance().walk(frames -> frames
                .map(frame -> pluginForClass(frame.getClassName()))
                .filter(plugin -> plugin != null && plugin != this)
                .findFirst()
                .orElse(null));
    }

    private Plugin pluginForClass(String className) {
        for (Plugin plugin : getServer().getPluginManager().getPlugins()) {
            if (plugin == this) {
                continue;
            }
            try {
                Class<?> type = Class.forName(className, false, plugin.getClass().getClassLoader());
                if (type.getClassLoader() == plugin.getClass().getClassLoader()) {
                    return plugin;
                }
            } catch (ClassNotFoundException | LinkageError ignored) {
                // The frame belongs to another loader or an unloaded plugin.
            }
        }
        return null;
    }

    private static Location locationArgument(Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            if (argument instanceof Location location) {
                return location.clone();
            }
        }
        return null;
    }

    private static Location numericLocation(World world, Object[] arguments) {
        if (arguments == null || arguments.length < 3
                || !(arguments[0] instanceof Number x)
                || !(arguments[1] instanceof Number y)
                || !(arguments[2] instanceof Number z)) {
            return null;
        }
        return new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    String statusLine() {
        if (observationPipeline == null) {
            return "not initialized";
        }
        String session = lastSession == null ? "none" : lastSession.state().name().toLowerCase(java.util.Locale.ROOT);
        return "session=" + session
                + ", detectors=" + configManager.current().enabledDetectors()
                + ", findings=" + findingAggregator.groupCount()
                + ", dropped=" + observationPipeline.droppedObservations()
                + ", compatibility=" + runtimeAdapter.describe().compatibilityStatus().name().toLowerCase(java.util.Locale.ROOT)
                + ", profile=" + runtimeAdapter.describe().compatibilityProfile()
                + ", coverage=" + runtimeAdapter.describe().coverageStatus();
    }

    String startSession(String label) {
        try {
            lastSession = sessionManager.start(label, configManager.current());
            scheduleSessionExpiry(lastSession);
            return "Started FoliaRace session " + lastSession.id();
        } catch (IllegalStateException error) {
            return "Could not start session: " + error.getMessage();
        }
    }

    String stopSession() {
        return sessionManager.stop()
                .map(session -> {
                    lastSession = session;
                    return "Stopped FoliaRace session " + session.id();
                })
                .orElse("No active FoliaRace session");
    }

    String flushReport() {
        if (lastSession == null || findingAggregator == null) {
            return "No report available";
        }
        try {
            List<FindingGroupSnapshot> findings = reportFindings();
            CiEvaluation evaluation = ciEvaluation(findings);
            InstrumentationHealth instrumentation = instrumentationHealth();
            Map<String, Object> health = new HashMap<>();
            health.put("droppedObservations", observationPipeline.droppedObservations());
            health.put("ruleFailures", observationPipeline.ruleFailures());
            health.put("pendingObservations", observationPipeline.pendingObservations());
            health.put("ciMode", configManager.current().ciMode());
            health.put("ciStatus", evaluation.status().name());
            health.put("ciExitCode", configManager.current().ciMode() ? evaluation.exitCode() : 0);
            health.put("instrumentationRequired", configManager.current().requireInstrumentation());
            health.putAll(instrumentation.asReportFields());
            ReportDocument report = new ReportDocument(
                    "1",
                    lastSession.id(),
                    lastSession.label(),
                    Instant.now(),
                    lastSession.state().name().toLowerCase(java.util.Locale.ROOT),
                    runtimeAdapter.describe(),
                    findings,
                    health
            );
            Path reportDirectory = getDataFolder().toPath().resolve("reports");
            List<Path> written = new java.util.ArrayList<>();
            for (OutputFormat format : configManager.current().outputFormats()) {
                Path destination = reportDirectory.resolve(lastSession.id() + (format == OutputFormat.JSON ? ".json" : ".md"));
                if (format == OutputFormat.JSON) {
                    new JsonReportWriter().write(destination, report);
                } else {
                    new com.foliarace.core.report.MarkdownReportWriter().write(destination, report);
                }
                written.add(destination);
            }
            return "Wrote reports to " + written;
        } catch (Exception error) {
            getLogger().warning("Could not write FoliaRace report: " + error.getMessage());
            return "Report write failed: " + error.getMessage();
        }
    }

    private List<FindingGroupSnapshot> reportFindings() {
        return FindingFilters.apply(
                SuppressionMatcher.apply(findingAggregator.snapshot(), suppressions, Instant.now()),
                configManager.current().minimumSeverity(),
                configManager.current().minimumConfidence()
        );
    }

    private void scheduleSessionExpiry(DiagnosticSession session) {
        long delayTicks = Math.multiplyExact(configManager.current().maxSessionDurationSeconds(), 20L);
        getServer().getGlobalRegionScheduler().runDelayed(this, task -> {
            if (lastSession == session && session.state() == com.foliarace.core.session.SessionState.ACTIVE) {
                stopSession();
                flushReport();
                getLogger().info("Session expired after " + configManager.current().maxSessionDurationSeconds() + " seconds");
            }
        }, delayTicks);
    }

    private CiEvaluation ciEvaluation(List<FindingGroupSnapshot> findings) {
        BaselineComparison comparison = baseline == null
                ? null
                : BaselineComparator.compare(baseline, currentBaseline(findings));
        boolean incomplete = runtimeAdapter.describe().compatibilityStatus() != com.foliarace.core.runtime.CompatibilityStatus.SUPPORTED;
        boolean instrumentationFailure = configManager.current().requireInstrumentation()
                && !instrumentationHealth().installed();
        return CiEvaluator.evaluate(findings, comparison, incomplete, instrumentationFailure);
    }

    private InstrumentationHealth instrumentationHealth() {
        return agentBridge == null ? InstrumentationHealth.unavailable("agent bridge not initialized") : agentBridge.health();
    }

    private static Baseline currentBaseline(List<FindingGroupSnapshot> findings) {
        Map<String, String> detectorVersions = findings.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                group -> group.representative().detectorId(),
                group -> group.representative().detectorVersion(),
                (left, right) -> left
        ));
        Set<String> fingerprints = findings.stream().map(group -> group.representative().fingerprint().value()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new Baseline("1", detectorVersions, fingerprints);
    }
}
