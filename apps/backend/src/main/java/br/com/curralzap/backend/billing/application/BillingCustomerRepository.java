package br.com.curralzap.backend.billing.application;

import br.com.curralzap.backend.billing.application.BillingCustomerData;

import java.util.Optional;
import java.util.UUID;

public interface BillingCustomerRepository {

    Optional<BillingCustomerData> findByFarmId(UUID farmId);

    void upsert(BillingCustomerData data);
}
