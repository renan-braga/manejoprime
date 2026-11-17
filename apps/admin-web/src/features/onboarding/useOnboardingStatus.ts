import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";

export type OnboardingStatus = {
  adminHasPhone: boolean;
  hasLots: boolean;
  hasAnimals: boolean;
  hasVaccines: boolean;
  animalCount: number;
  animalPlanLimit: number | null;
};

const DISMISSED_KEY = "onboarding_wizard_dismissed";

export function useOnboardingStatus() {
  const { fetchProtectedJson } = useAuth();
  const [status, setStatus] = useState<OnboardingStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [dismissed, setDismissed] = useState(() => localStorage.getItem(DISMISSED_KEY) === "true");

  useEffect(() => {
    let cancelled = false;
    fetchProtectedJson<OnboardingStatus>("/api/v1/onboarding/status")
      .then((data) => {
        if (!cancelled) setStatus(data);
      })
      .catch(() => {
        // Non-critical — wizard just won't show
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [fetchProtectedJson]);

  function dismiss() {
    localStorage.setItem(DISMISSED_KEY, "true");
    setDismissed(true);
  }

  const shouldShow =
    !dismissed &&
    status != null &&
    (!status.hasLots || !status.hasAnimals);

  return { status, loading, shouldShow, dismiss, refetch: () => setStatus(null) };
}
