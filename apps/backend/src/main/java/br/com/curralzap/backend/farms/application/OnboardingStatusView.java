package br.com.curralzap.backend.farms.application;

public record OnboardingStatusView(
        boolean adminHasPhone,
        boolean hasLots,
        boolean hasAnimals,
        boolean hasVaccines,
        long animalCount,
        Integer animalPlanLimit
) {}
