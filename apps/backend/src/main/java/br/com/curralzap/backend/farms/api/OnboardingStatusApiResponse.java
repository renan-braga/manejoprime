package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.farms.application.OnboardingStatusView;

record OnboardingStatusApiResponse(
        boolean adminHasPhone,
        boolean hasLots,
        boolean hasAnimals,
        boolean hasVaccines,
        long animalCount,
        Integer animalPlanLimit
) {

    static OnboardingStatusApiResponse from(OnboardingStatusView view) {
        return new OnboardingStatusApiResponse(
                view.adminHasPhone(),
                view.hasLots(),
                view.hasAnimals(),
                view.hasVaccines(),
                view.animalCount(),
                view.animalPlanLimit()
        );
    }
}
