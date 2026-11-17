package br.com.curralzap.backend.offers.api;

import br.com.curralzap.backend.offers.application.OfferMutationInput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record OfferMutationApiRequest(
        String title,
        String notes,
        BigDecimal askingPrice,
        String buyerContact,
        LocalDate expiresAt,
        List<UUID> animalIds
) {
    OfferMutationInput toInput() {
        return OfferMutationInput.fromApi(title, notes, askingPrice, buyerContact, expiresAt, animalIds);
    }
}
