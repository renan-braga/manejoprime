import { useState } from "react";

export function usePageTour(key: string) {
  const storageKey = `curralzap.admin.tour.${key}`;
  const [run, setRun] = useState(() => !localStorage.getItem(storageKey));

  function finish() {
    localStorage.setItem(storageKey, "1");
    setRun(false);
  }

  return { run, finish };
}
