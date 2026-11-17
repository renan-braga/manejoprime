package br.com.curralzap.backend.users.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {

    Optional<Invitation> findPendingByEmail(String email);

    List<Invitation> findByFarmId(UUID farmId);

    Optional<Invitation> findByIdAndFarmId(UUID id, UUID farmId);

    Invitation save(Invitation invitation);
}
