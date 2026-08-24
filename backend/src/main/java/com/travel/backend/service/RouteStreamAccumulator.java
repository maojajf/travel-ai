package com.travel.backend.service;

import java.util.function.Consumer;

final class RouteStreamAccumulator {

    private final RouteMarkdownSanitizer sanitizer;
    private final long flushIntervalMillis;
    private final StringBuilder rawMarkdown = new StringBuilder();
    private final StringBuilder emittedMarkdown = new StringBuilder();

    private long lastFlushAt;
    private int receivedChars;

    RouteStreamAccumulator(RouteMarkdownSanitizer sanitizer, long flushIntervalMillis) {
        this.sanitizer = sanitizer;
        this.flushIntervalMillis = flushIntervalMillis;
        this.lastFlushAt = System.currentTimeMillis();
    }

    void append(String chunk, Consumer<String> deltaConsumer) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        rawMarkdown.append(chunk);
        receivedChars += chunk.length();

        long now = System.currentTimeMillis();
        if (shouldFlushNow(chunk, now)) {
            flushStable(deltaConsumer, now);
        }
    }

    void flush(Consumer<String> deltaConsumer) {
        flushStable(deltaConsumer, System.currentTimeMillis());
    }

    String complete(Consumer<String> deltaConsumer) {
        String sanitizedMarkdown = sanitizer.sanitize(rawMarkdown.toString());
        emitTarget(deltaConsumer, sanitizedMarkdown);
        lastFlushAt = System.currentTimeMillis();
        return sanitizedMarkdown;
    }

    int getReceivedChars() {
        return receivedChars;
    }

    private boolean shouldFlushNow(String chunk, long now) {
        return containsBoundary(chunk) || now - lastFlushAt >= flushIntervalMillis;
    }

    private boolean containsBoundary(String chunk) {
        return chunk.indexOf('\n') >= 0
            || chunk.contains("\\n")
            || chunk.indexOf('#') >= 0
            || chunk.indexOf('-') >= 0;
    }

    private void flushStable(Consumer<String> deltaConsumer, long now) {
        String normalizedMarkdown = sanitizer.normalizeForStreaming(rawMarkdown.toString());
        String stablePrefix = sanitizer.stablePrefix(normalizedMarkdown);
        emitTarget(deltaConsumer, stablePrefix);

        lastFlushAt = now;
    }

    private void emitTarget(Consumer<String> deltaConsumer, String targetMarkdown) {
        if (targetMarkdown == null || targetMarkdown.length() <= emittedMarkdown.length()) {
            return;
        }

        if (!targetMarkdown.startsWith(emittedMarkdown.toString())) {
            return;
        }

        String deltaChunk = targetMarkdown.substring(emittedMarkdown.length());
        emittedMarkdown.append(deltaChunk);
        deltaConsumer.accept(deltaChunk);
    }
}
