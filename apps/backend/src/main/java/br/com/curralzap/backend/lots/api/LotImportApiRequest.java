package br.com.curralzap.backend.lots.api;

import br.com.curralzap.backend.lots.application.LotImportRow;

import java.util.List;

record LotImportApiRequest(List<LotImportRow> rows) {}
