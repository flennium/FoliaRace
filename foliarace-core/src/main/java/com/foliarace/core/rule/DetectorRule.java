package com.foliarace.core.rule;

import com.foliarace.core.finding.FindingDraft;
import com.foliarace.core.observation.Observation;

import java.util.Optional;

public interface DetectorRule {
    String id();

    String version();

    Optional<FindingDraft> evaluate(Observation observation);
}
