package br.com.curralzap.backend.animals.application;

public record AnimalImportRow(
        String brinco,
        String nome,
        String raca,
        String sexo,
        String dataNascimento,
        String pesoNascimentoKg,
        String nomeLote,
        String dataEntrada,
        String status
) {}
