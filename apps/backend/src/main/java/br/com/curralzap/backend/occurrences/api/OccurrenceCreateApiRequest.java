package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.occurrences.application.OccurrenceCreateCommand;

record OccurrenceCreateApiRequest(
        String animalId,
        String type,
        String severity,
        String description
) {
    OccurrenceCreateCommand toCommand() {
        return OccurrenceCreateCommand.fromApi(animalId, type, severity, description);
    }
}
