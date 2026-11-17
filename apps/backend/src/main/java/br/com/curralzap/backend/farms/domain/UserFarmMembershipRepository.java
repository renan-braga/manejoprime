package br.com.curralzap.backend.farms.domain;

import java.util.List;
import java.util.UUID;

public interface UserFarmMembershipRepository {

    void save(UUID userId, UUID farmId, String role);

    int countByUserId(UUID userId);

    boolean existsByUserIdAndFarmId(UUID userId, UUID farmId);

    List<FarmMembershipDetails> findWithFarmNameByUserId(UUID userId);
}
