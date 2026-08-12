package com.foliarace.plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class AgentBridgeInstaller {
    private Object sink;
    private Method uninstall;

    boolean install(FoliaRacePlugin plugin) {
        try {
            ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            Class<?> sinkType = Class.forName("com.foliarace.agent.InstrumentationSink", true, systemLoader);
            Class<?> bridgeType = Class.forName("com.foliarace.agent.InstrumentationBridge", true, systemLoader);
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
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException error) {
            plugin.getLogger().info("Automatic instrumentation agent is not installed: " + error.getMessage());
            sink = null;
            uninstall = null;
            return false;
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
            uninstall = null;
        }
    }
}
