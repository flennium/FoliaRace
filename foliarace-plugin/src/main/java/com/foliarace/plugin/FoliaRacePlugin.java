package com.foliarace.plugin;

import com.foliarace.core.config.ConfigManager;
import com.foliarace.core.config.FoliaRaceConfig;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.pipeline.ObservationPipeline;
import com.foliarace.core.report.JsonReportWriter;
import com.foliarace.core.report.ReportDocument;
import com.foliarace.core.session.DiagnosticSession;
import com.foliarace.core.session.SessionManager;
import com.foliarace.core.rule.CrossRegionOwnershipRule;
import com.foliarace.core.rule.DetectorRule;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FoliaRacePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private FindingAggregator findingAggregator;
    private ObservationPipeline observationPipeline;
    private SessionManager sessionManager;
    private FoliaRuntimeAdapter runtimeAdapter;
    private DiagnosticSession lastSession;

    @Override
    public void onEnable() {
        FoliaRaceConfig config = FoliaRaceConfig.defaults();
        configManager = new ConfigManager(config);
        findingAggregator = new FindingAggregator();
        sessionManager = new SessionManager();
        runtimeAdapter = new FoliaRuntimeAdapter();

        List<DetectorRule> rules = config.enabledDetectors().contains(CrossRegionOwnershipRule.ID)
                ? List.of(new CrossRegionOwnershipRule())
                : List.of();
        observationPipeline = new ObservationPipeline(config.observationQueueCapacity(), rules, findingAggregator);
        observationPipeline.start();
        startSession("startup");

        PluginCommand command = getCommand("foliarace");
        if (command != null) {
            FoliaRaceCommand executor = new FoliaRaceCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("FoliaRace enabled: " + statusLine());
    }

    @Override
    public void onDisable() {
        stopSession();
        if (observationPipeline != null) {
            observationPipeline.close();
        }
        flushReport();
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
                    findingAggregator.snapshot(),
                    Map.of(
                            "droppedObservations", observationPipeline.droppedObservations(),
                            "ruleFailures", observationPipeline.ruleFailures(),
                            "pendingObservations", observationPipeline.pendingObservations()
                    )
            ));
            return "Wrote report to " + reportPath;
        } catch (Exception error) {
            getLogger().warning("Could not write FoliaRace report: " + error.getMessage());
            return "Report write failed: " + error.getMessage();
        }
    }
}
