package br.com.curralzap.backend.whatsapp.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappFarmSessionRepository {
    Optional<WhatsappFarmSession> findByPhoneAndDate(String phoneNumber, LocalDate date);
    void save(String phoneNumber, UUID farmId, String farmName, LocalDate date);
}
