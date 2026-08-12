package com.foliarace.plugin;

import com.foliarace.core.config.ConfigManager;
import com.foliarace.core.config.FoliaRaceConfig;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.finding.Baseline;
import com.foliarace.core.finding.BaselineComparator;
import com.foliarace.core.finding.BaselineComparison;
import com.foliarace.core.finding.FindingGroupSnapshot;
import com.foliarace.core.finding.Suppression;
import com.foliarace.core.finding.SuppressionMatcher;
import com.foliarace.core.ci.CiEvaluation;
import com.foliarace.core.ci.CiEvaluator;
import com.foliarace.core.pipeline.ObservationPipeline;
import com.foliarace.core.report.JsonReportWriter;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FoliaRacePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private FindingAggregator findingAggregator;
    private ObservationPipeline observationPipeline;
    private SessionManager sessionManager;
    private FoliaRuntimeAdapter runtimeAdapter;
    private DiagnosticSession lastSession;
    private AgentBridgeInstaller agentBridge;
    private boolean instrumentationAvailable;
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
        instrumentationAvailable = agentBridge.install(this);
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
        if (agentBridge != null) {
            agentBridge.uninstall();
        }
        FoliaRaceObservations.uninstall(this);
        if (observationPipeline != null) {
            observationPipeline.close();
        }
        if (lastSession != null) {
            flushReport();
        }
    }

    boolean recordObservation(com.foliarace.core.observation.Observation observation) {
        return observationPipeline != null && observationPipeline.submit(observation);
    }

    FoliaRuntimeAdapter runtimeAdapter() {
        return runtimeAdapter;
    }

    void recordInstrumentedOperation(String ownerType, String methodName, Object receiver, Object[] arguments) {
        Plugin source = findOriginPlugin();
        if (source == null) {
            return;
        }
        if (receiver instanceof Entity entity && ownerType.endsWith("CraftEntity")) {
            FoliaRaceObservations.observeEntityAccess(source, entity, com.foliarace.core.observation.OperationCategory.ENTITY_ACCESS);
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
        lastSession = sessionManager.start(label, configManager.current());
        return "Started FoliaRace session " + lastSession.id();
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
            Path reportPath = getDataFolder().toPath().resolve("reports").resolve(lastSession.id() + ".json");
            new JsonReportWriter().write(reportPath, new ReportDocument(
                    "1",
                    lastSession.id(),
                    lastSession.label(),
                    Instant.now(),
                    lastSession.state().name().toLowerCase(java.util.Locale.ROOT),
                    runtimeAdapter.describe(),
                    reportFindings(),
                    Map.of(
                            "droppedObservations", observationPipeline.droppedObservations(),
                            "ruleFailures", observationPipeline.ruleFailures(),
                            "pendingObservations", observationPipeline.pendingObservations(),
                            "ciStatus", ciEvaluation(reportFindings()).status().name(),
                            "ciExitCode", ciEvaluation(reportFindings()).exitCode()
                    )
            ));
            return "Wrote report to " + reportPath;
        } catch (Exception error) {
            getLogger().warning("Could not write FoliaRace report: " + error.getMessage());
            return "Report write failed: " + error.getMessage();
        }
    }

    private List<FindingGroupSnapshot> reportFindings() {
        return SuppressionMatcher.apply(findingAggregator.snapshot(), suppressions, Instant.now());
    }

    private CiEvaluation ciEvaluation(List<FindingGroupSnapshot> findings) {
        BaselineComparison comparison = baseline == null
                ? null
                : BaselineComparator.compare(baseline, currentBaseline(findings));
        boolean incomplete = runtimeAdapter.describe().compatibilityStatus() != com.foliarace.core.runtime.CompatibilityStatus.SUPPORTED;
        return CiEvaluator.evaluate(findings, comparison, incomplete, !instrumentationAvailable);
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
