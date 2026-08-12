package com.foliarace.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

public final class FoliaRaceAgent {
    private FoliaRaceAgent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        install(instrumentation);
    }

    private static void install(Instrumentation instrumentation) {
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
}
