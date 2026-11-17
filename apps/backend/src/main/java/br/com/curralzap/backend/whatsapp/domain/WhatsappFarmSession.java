package br.com.curralzap.backend.whatsapp.domain;

import java.time.LocalDate;
import java.util.UUID;

public record WhatsappFarmSession(String phoneNumber, UUID farmId, String farmName, LocalDate selectedDate) {}
