package br.com.curralzap.backend.occurrences.application;

import java.util.UUID;

public interface OccurrenceReadViewRepository {

    OccurrenceListView listFor(UUID farmId, OccurrenceListQuery query);
}
