import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const ANIMALS_RESPONSE = {
  items: [{ id: "animal-1", identifier: "347", name: "Trovao", sex: "male", breed: "Nelore", status: "active", lotId: "lot-1", lotName: "Engorda 1" }],
  total: 1, limit: 100, offset: 0
};
const LOTS_RESPONSE = {
  items: [{ id: "lot-1", name: "Engorda 1", purpose: "fattening", pastureArea: "Pasto Norte", animalsCount: 18 }],
  total: 1, limit: 100, offset: 0
};
const FEEDINGS_ANIMAL_RESPONSE = {
  items: [
    { id: "f-1", animalId: "animal-1", lotId: null, feedType: "racao", description: "Racao de acabamento", quantityKg: 42.5, costPerKg: 2.1, startDate: "2026-04-20", endDate: "2026-04-26", source: "app", recordedBy: "u-1" },
    { id: "f-2", animalId: "animal-1", lotId: null, feedType: "silagem", description: "Silagem de milho", quantityKg: 15, costPerKg: 0.45, startDate: "2026-04-21", endDate: null, source: "whatsapp", recordedBy: "u-1" }
  ],
  total: 2, limit: 20, offset: 0
};

function setupMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input.toString();
    if (url.includes("/api/v1/animals?")) return json(ANIMALS_RESPONSE);
    if (url.includes("/api/v1/lots?")) return json(LOTS_RESPONSE);
    if (url.includes("/api/v1/feedings?animalId=animal-1")) return json(FEEDINGS_ANIMAL_RESPONSE);
    if (url.includes("/api/v1/feedings?lotId=lot-1")) return json({ items: [], total: 0, limit: 20, offset: 0 });
    throw new Error(`Unexpected: ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => { vi.unstubAllGlobals(); });

describe("feeding page migration", () => {
  it("renders page heading 'Feedings' as h1", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/animals/feedings"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Alimentações" })).toBeInTheDocument());
  });

  it("feeding history shows a table with Type, Quantity, Period column headers and row data", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/animals/feedings"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("columnheader", { name: "Tipo" })).toBeInTheDocument());
    expect(screen.getByRole("columnheader", { name: "Quantidade" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Período" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Fonte" })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("Racao de acabamento")).toBeInTheDocument());
    expect(screen.getByText("42.5 kg")).toBeInTheDocument();
    expect(screen.getByText("2026-04-20 até 2026-04-26")).toBeInTheDocument();
  });

  it("filters feeding rows by search text and feed type", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupMock();
    renderAppRoutes({ initialEntries: ["/app/animals/feedings"], session: admin.session });

    await waitFor(() => expect(screen.getByText("Racao de acabamento")).toBeInTheDocument());
    expect(screen.getByText("Silagem de milho")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Buscar alimentações"), { target: { value: "silagem" } });

    expect(screen.queryByText("Racao de acabamento")).not.toBeInTheDocument();
    expect(screen.getByText("Silagem de milho")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Buscar alimentações"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("Feed type filter"), { target: { value: "racao" } });

    expect(screen.getByText("Racao de acabamento")).toBeInTheDocument();
    expect(screen.queryByText("Silagem de milho")).not.toBeInTheDocument();
  });

  it("shows skeleton while options are loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/animals/feedings"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("feedings-skeleton")).toBeInTheDocument());
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}
