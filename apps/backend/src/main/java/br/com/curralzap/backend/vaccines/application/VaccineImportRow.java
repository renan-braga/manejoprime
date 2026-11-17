package br.com.curralzap.backend.vaccines.application;

public record VaccineImportRow(
        String brincoAnimal,
        String tipoVacina,
        String loteVacina,
        String dose,
        String dataAplicacao,
        String proximaDose,
        String volumeMl
) {}
