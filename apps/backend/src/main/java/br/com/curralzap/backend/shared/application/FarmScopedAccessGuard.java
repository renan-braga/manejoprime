package br.com.curralzap.backend.shared.application;

import br.com.curralzap.backend.shared.domain.FarmScopedEntityRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class FarmScopedAccessGuard {

    public <T> T requireInFarm(
            FarmScopedEntityRepository<T> repository,
            UUID entityId,
            UUID farmId,
            Supplier<? extends RuntimeException> notFoundException
    ) {
        return repository.findByIdAndFarmId(entityId, farmId)
                .orElseThrow(notFoundException);
    }
}
