package br.com.curralzap.backend.ops.application;

public record OpsMessageStatsView(
        String period,
        long totalMessages,
        long audioMessages,
        long textMessages,
        long transcribed,
        long applied,
        long failed
) {}
