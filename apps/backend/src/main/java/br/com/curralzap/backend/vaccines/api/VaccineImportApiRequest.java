package br.com.curralzap.backend.vaccines.api;

import br.com.curralzap.backend.vaccines.application.VaccineImportRow;

import java.util.List;

record VaccineImportApiRequest(List<VaccineImportRow> rows) {}
