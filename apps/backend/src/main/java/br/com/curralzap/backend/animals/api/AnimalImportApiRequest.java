package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.animals.application.AnimalImportRow;

import java.util.List;

record AnimalImportApiRequest(List<AnimalImportRow> rows) {}
