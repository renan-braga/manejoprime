package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsMessageStatsView;

record OpsMessageStatsApiResponse(
        String period,
        long totalMessages,
        long audioMessages,
        long textMessages,
        long transcribed,
        long applied,
        long failed
) {
    static OpsMessageStatsApiResponse from(OpsMessageStatsView v) {
        return new OpsMessageStatsApiResponse(
                v.period(), v.totalMessages(), v.audioMessages(), v.textMessages(),
                v.transcribed(), v.applied(), v.failed());
    }
}
