import { Joyride, type EventData, STATUS, type Step } from "react-joyride";

type AppTourProps = {
  steps: Step[];
  run: boolean;
  onFinish: () => void;
};

export function AppTour({ steps, run, onFinish }: AppTourProps) {
  function handleEvent(data: EventData) {
    if (data.status === STATUS.FINISHED || data.status === STATUS.SKIPPED) {
      onFinish();
    }
  }

  return (
    <Joyride
      steps={steps}
      run={run}
      continuous
      onEvent={handleEvent}
      locale={{
        back: "Voltar",
        close: "Fechar",
        last: "Concluir",
        next: "Próximo",
        skip: "Pular tour",
      }}
      options={{
        primaryColor: "#16a34a",
        overlayColor: "rgba(0, 0, 0, 0.45)",
        zIndex: 9999,
        showProgress: true,
        skipBeacon: true,
        buttons: ["back", "primary", "skip"],
      }}
      styles={{
        tooltip: { borderRadius: "8px" },
        tooltipTitle: { fontSize: "0.9rem", fontWeight: 600, marginBottom: "4px" },
        tooltipContent: { padding: "4px 0 0", fontSize: "0.875rem" },
        buttonPrimary: { borderRadius: "6px", fontSize: "0.875rem", padding: "6px 16px" },
        buttonBack: { color: "#6b7280", fontSize: "0.875rem", marginRight: "8px" },
        buttonSkip: { fontSize: "0.8rem", color: "#9ca3af" },
      }}
    />
  );
}
