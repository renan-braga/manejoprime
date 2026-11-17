package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.feeding.application.FeedingImportRow;

import java.util.List;

record FeedingImportApiRequest(List<FeedingImportRow> rows) {}
