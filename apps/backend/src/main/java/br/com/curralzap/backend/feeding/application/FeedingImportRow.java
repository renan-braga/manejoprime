package br.com.curralzap.backend.feeding.application;

public record FeedingImportRow(
        String brincoAnimal,
        String nomeLote,
        String tipoAlimento,
        String descricao,
        String quantidadeKg,
        String custoPorKg,
        String dataInicio,
        String dataFim
) {}
