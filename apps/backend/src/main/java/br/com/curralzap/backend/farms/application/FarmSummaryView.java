package br.com.curralzap.backend.farms.application;

import java.util.UUID;

public record FarmSummaryView(UUID farmId, String farmName, String role, boolean isCurrent) {}
