package com.foliarace.core.runtime;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.evidence.OwnershipEvidence;

import java.time.Instant;

public interface RuntimeAdapter<L, E> {
    RuntimeDescriptor describe();

    ExecutionContext classifyCurrentContext(Instant observedAt);

    OwnershipEvidence resolveLocationOwnership(L location, Instant observedAt);

    OwnershipEvidence resolveEntityOwnership(E entity, Instant observedAt);
}
