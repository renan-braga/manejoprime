import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const ANIMALS_RESPONSE = {
  items: [
    { id: "animal-1", identifier: "347", name: "Trovao", sex: "male", breed: "Nelore", status: "active", lotId: "lot-1", lotName: "Engorda 1" }
  ],
  total: 1,
  limit: 100,
  offset: 0,
};

const MEDICATIONS_RESPONSE = {
  items: [
    {
      id: "medication-1",
      animalId: "animal-1",
      occurrenceId: "occurrence-1",
      medicationName: "Ivermectin",
      activeIngredient: "Ivermectina",
      reason: "Ticks",
      dosage: "10 ml",
      withdrawalPeriodDays: 35,
      applicationDate: "2026-04-24",
      releaseDate: "2026-05-29",
      inWithdrawalPeriod: true,
      cost: 45.9,
      source: "app",
      recordedBy: "admin-user",
    },
    {
      id: "medication-2",
      animalId: "animal-1",
      occurrenceId: null,
      medicationName: "Oxytetracycline",
      activeIngredient: "Oxitetraciclina",
      reason: "Respiratory protocol",
      dosage: "20 ml",
      withdrawalPeriodDays: 5,
      applicationDate: "2026-04-20",
      releaseDate: "2026-04-25",
      inWithdrawalPeriod: false,
      cost: 88.4,
      source: "app",
      recordedBy: "admin-user",
    },
  ],
  total: 2,
  limit: 20,
  offset: 0,
};

function setupMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";

    if (url.includes("/api/v1/animals?")) return json(ANIMALS_RESPONSE);
    if (url.includes("/api/v1/medications?")) return json(MEDICATIONS_RESPONSE);
    if (url.endsWith("/api/v1/medications") && method === "POST") {
      return json({
        id: "medication-3",
        animalId: "animal-1",
        occurrenceId: "occurrence-2",
        medicationName: "Flunixin",
        activeIngredient: "Flunixina",
        reason: "Pain control",
        dosage: "5 ml",
        withdrawalPeriodDays: 7,
        applicationDate: "2026-05-05",
        releaseDate: "2026-05-12",
        inWithdrawalPeriod: true,
        cost: 19.9,
        source: "app",
        recordedBy: "admin-user",
      });
    }
    throw new Error(`Unexpected request: ${method} ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("medications page migration", () => {
  it("renders /health/medications with medication rows and carencia column", async () => {
    vi.setSystemTime(new Date("2026-05-05T12:00:00Z"));
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/medications"], session: admin.session });

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1, name: "Medicamentos" })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: "Nome" })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: "Dosagem e Motivo" })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: "Carência" })).toBeInTheDocument();
    });
    await waitFor(() => expect(screen.getByText("Ivermectin")).toBeInTheDocument());
    expect(screen.getByText("24 dias restantes")).toBeInTheDocument();
  });

  it("shows an expired carencia badge when days remaining is zero or negative", async () => {
    vi.setSystemTime(new Date("2026-05-05T12:00:00Z"));
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/medications"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Oxytetracycline")).toBeInTheDocument());
    expect(screen.getByText("Venceu há 10 dias")).toBeInTheDocument();
  });

  it("lets veterinarians access the standalone route", async () => {
    const veterinarian = adminWebRoleScenarios.find((s) => s.name === "veterinarian")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/medications"], session: veterinarian.session });

    await waitFor(() => expect(screen.getByText("Ivermectin")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Salvar medicamento" })).toBeInTheDocument();
  });

  it("keeps the medication create form posting to the existing API", async () => {
    vi.setSystemTime(new Date("2026-05-05T12:00:00Z"));
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    const fetchMock = setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/medications"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Ivermectin")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Occurrence ID"), { target: { value: "occurrence-2" } });
    fireEvent.change(screen.getByLabelText("Medication name"), { target: { value: "Flunixin" } });
    fireEvent.change(screen.getByLabelText("Active ingredient"), { target: { value: "Flunixina" } });
    fireEvent.change(screen.getByLabelText("Reason"), { target: { value: "Pain control" } });
    fireEvent.change(screen.getByLabelText("Dosage"), { target: { value: "5 ml" } });
    fireEvent.change(screen.getByLabelText("Withdrawal period (days)"), { target: { value: "7" } });
    fireEvent.change(screen.getByLabelText("Data de aplicação"), { target: { value: "2026-05-05" } });
    fireEvent.change(screen.getByLabelText("Cost"), { target: { value: "19.90" } });

    fireEvent.click(screen.getByRole("button", { name: "Salvar medicamento" }));

    await waitFor(() => expect(screen.getByText("Flunixin")).toBeInTheDocument());
    expect(screen.getByText("7 dias restantes")).toBeInTheDocument();
    const createCall = fetchMock.mock.calls.find((call) => {
      const url = typeof call[0] === "string" ? call[0] : call[0].toString();
      return url.endsWith("/api/v1/medications") && call[1]?.method === "POST";
    });
    expect(createCall).toBeDefined();
    expect(createCall?.[1]?.body).toContain("\"animalId\":\"animal-1\"");
    expect(createCall?.[1]?.body).toContain("\"medicationName\":\"Flunixin\"");
  });

  it("shows skeleton while medications are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/health/medications"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("medications-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
