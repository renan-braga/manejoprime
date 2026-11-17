package br.com.curralzap.backend.occurrences.domain;

import br.com.curralzap.backend.shared.domain.FarmScopedEntityRepository;

import java.util.UUID;

public interface OccurrenceRepository extends FarmScopedEntityRepository<Occurrence> {

    Occurrence insert(Occurrence occurrence);

    Occurrence save(Occurrence occurrence);

    void delete(UUID occurrenceId);
}
