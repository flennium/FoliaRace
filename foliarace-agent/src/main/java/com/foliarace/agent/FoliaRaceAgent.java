package com.foliarace.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

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
        appendBridgeToBootstrap(instrumentation);
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .ignore(nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("java."))
                        .or(nameStartsWith("jdk."))
                        .or(nameStartsWith("sun."))
                        .or(nameStartsWith("com.foliarace.agent.")))
                .type(ElementMatchers.nameMatches("org\\.bukkit\\.craftbukkit\\..*(CraftWorld|CraftEntity)$"))
                .transform((builder, type, classLoader, module, domain) -> builder.visit(
                        Advice.to(ServerOperationAdvice.class).on(
                                isMethod().and(
                                        ElementMatchers.named("getBlockAt")
                                                .or(ElementMatchers.named("getBlockType"))
                                                .or(ElementMatchers.named("getBlockData"))
                                                .or(ElementMatchers.named("getChunkAt"))
                                                .or(ElementMatchers.named("getLocation"))
                                                .or(ElementMatchers.named("getNearbyEntities"))
                                )
                        )
                ))
                .installOn(instrumentation);
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
        } catch (java.io.IOException | UnsupportedOperationException ignored) {
            // Explicit observation remains available if the bridge cannot be made bootstrap-visible.
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
