package br.com.curralzap.backend.medications.api;

import br.com.curralzap.backend.medications.application.MedicationCreateCommand;

record MedicationCreateApiRequest(
        String animalId,
        String occurrenceId,
        String medicationName,
        String activeIngredient,
        String reason,
        String dosage,
        String withdrawalPeriodDays,
        String applicationDate,
        String cost
) {
    MedicationCreateCommand toCommand() {
        return MedicationCreateCommand.fromApi(
                animalId,
                occurrenceId,
                medicationName,
                activeIngredient,
                reason,
                dosage,
                withdrawalPeriodDays,
                applicationDate,
                cost
        );
    }
}
