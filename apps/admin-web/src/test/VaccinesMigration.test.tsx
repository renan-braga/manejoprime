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

const LOTS_RESPONSE = {
  items: [
    { id: "lot-1", name: "Engorda 1", purpose: "fattening", pastureArea: "Pasto Norte", animalsCount: 18 }
  ],
  total: 1,
  limit: 100,
  offset: 0,
};

const VACCINES_RESPONSE = {
  items: [
    {
      id: "vaccine-1",
      animalId: "animal-1",
      lotId: null,
      vaccineType: "Aftosa",
      vaccineBatch: "AF-123",
      dose: "primeira",
      applicationDate: "2026-04-24",
      nextDoseDate: "2026-05-24",
      volumeMl: 2.5,
      source: "app",
      recordedBy: "admin-user",
    },
  ],
  total: 1,
  limit: 20,
  offset: 0,
};

function setupMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    const method = init?.method ?? "GET";

    if (url.includes("/api/v1/animals?")) return json(ANIMALS_RESPONSE);
    if (url.includes("/api/v1/lots?")) return json(LOTS_RESPONSE);
    if (url.includes("/api/v1/vaccines?")) return json(VACCINES_RESPONSE);
    if (url.endsWith("/api/v1/vaccines") && method === "POST") {
      return json({
        id: "vaccine-2",
        animalId: "animal-1",
        lotId: null,
        vaccineType: "Brucelose",
        vaccineBatch: "BR-456",
        dose: "reforco",
        applicationDate: "2026-05-01",
        nextDoseDate: "2026-06-01",
        volumeMl: 3,
        source: "app",
        recordedBy: "admin-user",
      });
    }
    throw new Error(`Unexpected request: ${method} ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => { vi.unstubAllGlobals(); });

describe("vaccines page migration", () => {
  it("renders /health/vaccines with vaccine rows and table columns", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/vaccines"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Vacinas" })).toBeInTheDocument());
    await waitFor(() => {
      expect(screen.getByRole("columnheader", { name: "Tipo" })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: "Dose" })).toBeInTheDocument();
      expect(screen.getByRole("columnheader", { name: "Data" })).toBeInTheDocument();
    });
    await waitFor(() => expect(screen.getByText("Aftosa")).toBeInTheDocument());
    expect(screen.getByText("Próxima dose em 2026-05-24")).toBeInTheDocument();
  });

  it("lets veterinarians access the standalone route", async () => {
    const veterinarian = adminWebRoleScenarios.find((s) => s.name === "veterinarian")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/vaccines"], session: veterinarian.session });

    await waitFor(() => expect(screen.getByText("Aftosa")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Salvar vacina" })).toBeInTheDocument();
  });

  it("keeps the vaccine create form posting to the existing API", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    const fetchMock = setupMock();
    renderAppRoutes({ initialEntries: ["/app/health/vaccines"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Aftosa")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Vaccine type"), { target: { value: "Brucelose" } });
    fireEvent.change(screen.getByLabelText("Vaccine batch"), { target: { value: "BR-456" } });
    fireEvent.change(screen.getByLabelText("Vaccine dose"), { target: { value: "reforco" } });
    fireEvent.change(screen.getByLabelText("Data de aplicação"), { target: { value: "2026-05-01" } });
    fireEvent.change(screen.getByLabelText("Data da próxima dose"), { target: { value: "2026-06-01" } });
    fireEvent.change(screen.getByLabelText("Volume (ml)"), { target: { value: "3.00" } });

    fireEvent.click(screen.getByRole("button", { name: "Salvar vacina" }));

    await waitFor(() => expect(screen.getByText("Brucelose")).toBeInTheDocument());
    const createCall = fetchMock.mock.calls.find((call) => {
      const url = typeof call[0] === "string" ? call[0] : call[0].toString();
      return url.endsWith("/api/v1/vaccines") && call[1]?.method === "POST";
    });
    expect(createCall).toBeDefined();
    expect(createCall?.[1]?.body).toContain("\"animalId\":\"animal-1\"");
    expect(createCall?.[1]?.body).toContain("\"vaccineType\":\"Brucelose\"");
  });

  it("shows skeleton while vaccines are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/health/vaccines"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("vaccines-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
