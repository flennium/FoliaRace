package com.foliarace.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

public final class FoliaRaceAgent {
    private static JarFile bootstrapBridge;

    private FoliaRaceAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    private static void install(Instrumentation instrumentation) {
        try {
            appendBridgeToBootstrap(instrumentation);
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .ignore(nameStartsWith("net.bytebuddy.")
                            .or(nameStartsWith("java."))
                            .or(nameStartsWith("jdk."))
                            .or(nameStartsWith("sun."))
                            .or(nameStartsWith("com.foliarace.agent.")))
                    .with(new AgentBuilder.Listener.Adapter() {
                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                                                      JavaModule module, boolean loaded, DynamicType dynamicType) {
                            InstrumentationBridge.recordTransformation();
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module,
                                            boolean loaded, Throwable throwable) {
                            InstrumentationBridge.recordTransformationFailure(throwable == null
                                    ? "instrumentation transformation failed"
                                    : throwable.getClass().getSimpleName());
                        }
                    })
                    .type(ElementMatchers.nameMatches(
                            "org\\.bukkit\\.craftbukkit\\..*(CraftWorld|CraftEntity|CraftPlayer|CraftChunk|CraftBlock|CraftInventory|CraftServer)$"))
                    .transform((builder, type, classLoader, module, domain) -> builder.visit(
                            Advice.to(ServerOperationAdvice.class).on(
                                    isMethod().and(
                                            ElementMatchers.named("getBlockAt")
                                                    .or(ElementMatchers.named("getBlockType"))
                                                    .or(ElementMatchers.named("getBlockData"))
                                                    .or(ElementMatchers.named("getChunkAt"))
                                                    .or(ElementMatchers.named("getHighestBlockAt"))
                                                    .or(ElementMatchers.named("getLocation"))
                                                    .or(ElementMatchers.named("getNearbyEntities"))
                                                    .or(ElementMatchers.named("getEntitiesByClass"))
                                                    .or(ElementMatchers.named("getWorld"))
                                                    .or(ElementMatchers.named("getBlock"))
                                                    .or(ElementMatchers.named("getChunk"))
                                                    .or(ElementMatchers.named("getType"))
                                                    .or(ElementMatchers.named("getBlockData"))
                                                    .or(ElementMatchers.named("getInventory"))
                                                    .or(ElementMatchers.named("getEnderChest"))
                                                    .or(ElementMatchers.named("getOnlinePlayers"))
                                                    .or(ElementMatchers.named("getWorlds"))
                                                    .or(ElementMatchers.named("getPluginManager"))
                                    )
                            )
                    ))
                    .installOn(instrumentation);
        } catch (RuntimeException error) {
            InstrumentationBridge.recordBridgeFailure("agent installation failed: " + error.getClass().getSimpleName());
        }
    }

    private static void appendBridgeToBootstrap(Instrumentation instrumentation) {
        try {
            Path bridgeJar = Files.createTempFile("foliarace-bridge-", ".jar");
            bridgeJar.toFile().deleteOnExit();
            try (OutputStream file = Files.newOutputStream(bridgeJar);
                 JarOutputStream output = new JarOutputStream(file)) {
                copyClass(output, "com/foliarace/agent/InstrumentationBridge.class");
                copyClass(output, "com/foliarace/agent/InstrumentationSink.class");
            }
            bootstrapBridge = new JarFile(bridgeJar.toFile());
            instrumentation.appendToBootstrapClassLoaderSearch(bootstrapBridge);
        } catch (java.io.IOException | UnsupportedOperationException error) {
            InstrumentationBridge.recordBridgeFailure("bootstrap bridge unavailable: " + error.getClass().getSimpleName());
        }
    }

    private static void copyClass(JarOutputStream output, String resource) throws java.io.IOException {
        output.putNextEntry(new JarEntry(resource));
        try (InputStream input = FoliaRaceAgent.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new java.io.FileNotFoundException(resource);
            }
            input.transferTo(output);
        }
        output.closeEntry();
    }
}
