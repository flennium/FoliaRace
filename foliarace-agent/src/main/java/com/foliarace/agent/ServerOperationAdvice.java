package com.foliarace.agent;

import net.bytebuddy.asm.Advice;

public final class ServerOperationAdvice {
    private ServerOperationAdvice() {
    }

    @Advice.OnMethodEnter
    public static void enter(
            @Advice.Origin("#t") String ownerType,
            @Advice.Origin("#m") String methodName,
            @Advice.This(optional = true) Object receiver,
            @Advice.AllArguments Object[] arguments
    ) {
        InstrumentationBridge.record(ownerType, methodName, receiver, arguments);
    }
}
