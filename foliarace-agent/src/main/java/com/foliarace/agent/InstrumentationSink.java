package com.foliarace.agent;

@FunctionalInterface
public interface InstrumentationSink {
    void accept(String ownerType, String methodName, Object receiver, Object[] arguments);
}
