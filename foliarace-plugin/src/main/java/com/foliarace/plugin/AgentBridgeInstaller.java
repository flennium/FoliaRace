package com.foliarace.plugin;

import com.foliarace.core.report.InstrumentationHealth;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class AgentBridgeInstaller {
    private Object sink;
    private Class<?> bridgeType;
    private Method uninstall;
    private String failureReason = "agent unavailable";

    boolean install(FoliaRacePlugin plugin) {
        try {
            ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            Class<?> sinkType = Class.forName("com.foliarace.agent.InstrumentationSink", true, systemLoader);
            bridgeType = Class.forName("com.foliarace.agent.InstrumentationBridge", true, systemLoader);
            sink = Proxy.newProxyInstance(systemLoader, new Class<?>[]{sinkType}, (proxy, method, arguments) -> {
                if (method.getName().equals("accept") && arguments != null && arguments.length == 4) {
                    plugin.recordInstrumentedOperation(
                            (String) arguments[0],
                            (String) arguments[1],
                            arguments[2],
                            (Object[]) arguments[3]
                    );
                }
                return null;
            });
            bridgeType.getMethod("install", sinkType).invoke(null, sink);
            uninstall = bridgeType.getMethod("uninstall", sinkType);
            failureReason = "available";
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError error) {
            plugin.getLogger().info("Automatic instrumentation agent is not installed: " + error.getMessage());
            sink = null;
            bridgeType = null;
            uninstall = null;
            failureReason = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            return false;
        }
    }

    InstrumentationHealth health() {
        if (bridgeType == null || sink == null) {
            return InstrumentationHealth.unavailable(failureReason);
        }
        try {
            return new InstrumentationHealth(
                    invokeBoolean("installed"),
                    invokeLong("transformedTargets"),
                    invokeLong("emittedEvents"),
                    invokeLong("droppedEvents"),
                    invokeLong("bridgeFailures"),
                    invokeLong("transformationFailures"),
                    invokeString("failureReason")
            );
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError error) {
            return InstrumentationHealth.unavailable("agent health unavailable: " + error.getClass().getSimpleName());
        }
    }

    void uninstall() {
        if (sink == null || uninstall == null) {
            return;
        }
        try {
            uninstall.invoke(null, sink);
        } catch (ReflectiveOperationException ignored) {
            // Agent shutdown must never affect the server.
        } finally {
            sink = null;
            bridgeType = null;
            uninstall = null;
        }
    }

    private long invokeLong(String methodName) throws ReflectiveOperationException {
        return ((Number) bridgeType.getMethod(methodName).invoke(null)).longValue();
    }

    private boolean invokeBoolean(String methodName) throws ReflectiveOperationException {
        return (Boolean) bridgeType.getMethod(methodName).invoke(null);
    }

    private String invokeString(String methodName) throws ReflectiveOperationException {
        return (String) bridgeType.getMethod(methodName).invoke(null);
    }
}
