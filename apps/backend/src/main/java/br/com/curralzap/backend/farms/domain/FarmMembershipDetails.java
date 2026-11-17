package br.com.curralzap.backend.farms.domain;

import java.util.UUID;

public record FarmMembershipDetails(UUID farmId, String farmName, String role) {}
