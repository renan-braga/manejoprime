package br.com.curralzap.backend.lots.application;

import java.util.Optional;
import java.util.UUID;

public interface LotReadViewRepository {

    LotListView listFor(UUID farmId, LotListQuery query);

    Optional<LotDetailView> findDetailById(UUID farmId, UUID lotId);
}
