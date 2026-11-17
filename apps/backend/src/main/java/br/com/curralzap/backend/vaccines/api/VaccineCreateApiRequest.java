package br.com.curralzap.backend.vaccines.api;

import br.com.curralzap.backend.vaccines.application.VaccineCreateCommand;

record VaccineCreateApiRequest(
        String animalId,
        String lotId,
        String vaccineType,
        String vaccineBatch,
        String dose,
        String applicationDate,
        String nextDoseDate,
        String volumeMl
) {
    VaccineCreateCommand toCommand() {
        return VaccineCreateCommand.fromApi(
                animalId,
                lotId,
                vaccineType,
                vaccineBatch,
                dose,
                applicationDate,
                nextDoseDate,
                volumeMl
        );
    }
}
