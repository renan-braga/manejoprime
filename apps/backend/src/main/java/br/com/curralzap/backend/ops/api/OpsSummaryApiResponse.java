package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsPlatformSummaryView;

record OpsSummaryApiResponse(
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
) {
    static OpsSummaryApiResponse from(OpsPlatformSummaryView v) {
        return new OpsSummaryApiResponse(
                v.totalFarms(), v.totalUsers(), v.adminCount(), v.vetCount(), v.workerCount(),
                v.totalAnimals(), v.messagesLast24h(), v.messagesLast7d(), v.messagesLast30d(),
                v.audioCount(), v.transcriptionCount(), v.errorCount());
    }
}
