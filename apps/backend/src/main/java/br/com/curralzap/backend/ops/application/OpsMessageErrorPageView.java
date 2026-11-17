package br.com.curralzap.backend.ops.application;

import java.util.List;

public record OpsMessageErrorPageView(
        List<OpsRecentErrorView> errors,
        long total,
        int limit,
        int offset
) {}
