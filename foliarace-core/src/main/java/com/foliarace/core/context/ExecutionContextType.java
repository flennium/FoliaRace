package com.foliarace.core.context;

public enum ExecutionContextType {
    GLOBAL_REGION,
    REGION,
    ENTITY,
    ASYNC,
    SERVER_BOOTSTRAP,
    PLUGIN_THREAD,
    UNKNOWN
}
