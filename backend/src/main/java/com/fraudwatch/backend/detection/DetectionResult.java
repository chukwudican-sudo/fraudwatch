package com.fraudwatch.backend.detection;

/** The outcome of running a transaction through the detection rules. */
public record DetectionResult(boolean flagged, String reason) {

    /** Shorthand for "no rule had a problem with this transaction." */
    public static DetectionResult clear() {
        return new DetectionResult(false, "");
    }
}
