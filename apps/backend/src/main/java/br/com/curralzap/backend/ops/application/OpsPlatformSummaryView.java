package br.com.curralzap.backend.ops.application;

public record OpsPlatformSummaryView(
        long totalFarms,
        long totalUsers,
        long adminCount,
        long vetCount,
        long workerCount,
        long totalAnimals,
        long messagesLast24h,
        long messagesLast7d,
        long messagesLast30d,
        long audioCount,
        long transcriptionCount,
        long errorCount
) {}
